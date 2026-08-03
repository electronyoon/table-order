package com.electronyoon.tableorder.web.admin;

import static org.assertj.core.api.Assertions.assertThat;

import com.electronyoon.tableorder.TestcontainersConfiguration;
import com.electronyoon.tableorder.domain.menu.Menu;
import com.electronyoon.tableorder.domain.menu.MenuCategory;
import com.electronyoon.tableorder.domain.menu.MenuCategoryRepository;
import com.electronyoon.tableorder.domain.menu.MenuRepository;
import com.electronyoon.tableorder.domain.storetable.StoreTable;
import com.electronyoon.tableorder.domain.storetable.StoreTableRepository;
import com.electronyoon.tableorder.web.dto.CancelOrderItemRequest;
import com.electronyoon.tableorder.web.dto.CreateOrderItemRequest;
import com.electronyoon.tableorder.web.dto.CreateOrderRequest;
import com.electronyoon.tableorder.web.dto.MenuDto;
import com.electronyoon.tableorder.web.dto.OrderItemResponse;
import com.electronyoon.tableorder.web.dto.OrderResponse;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
@Import(TestcontainersConfiguration.class)
class AdminQueueIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;
    @Autowired
    private StoreTableRepository storeTableRepository;
    @Autowired
    private MenuCategoryRepository menuCategoryRepository;
    @Autowired
    private MenuRepository menuRepository;
    @Value("${app.admin-token}")
    private String adminToken;

    private StoreTable table;
    private Menu menu;

    @BeforeEach
    void setUp() {
        table = new StoreTable();
        table.setLabel("관리자테스트");
        table.setQrToken("qr-" + UUID.randomUUID());
        table = storeTableRepository.saveAndFlush(table);

        MenuCategory category = new MenuCategory();
        category.setName("메인");
        category.setSortOrder(1);
        category = menuCategoryRepository.saveAndFlush(category);

        menu = new Menu();
        menu.setCategory(category);
        menu.setName("제육볶음");
        menu.setPrice(9000);
        menu.setSortOrder(1);
        menu.setSelfService(false);
        menu = menuRepository.saveAndFlush(menu);
    }

    private HttpHeaders authHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(adminToken);
        return headers;
    }

    private OrderResponse createCounterOrder() {
        CreateOrderRequest request = new CreateOrderRequest(
                UUID.randomUUID(), table.getId(), List.of(new CreateOrderItemRequest(menu.getId(), 1, null)));
        ResponseEntity<OrderResponse> response = restTemplate.exchange(
                "/admin/orders", HttpMethod.POST, new HttpEntity<>(request, authHeaders()), OrderResponse.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return response.getBody();
    }

    @Test
    void adminEndpointWithoutTokenReturns401() {
        ResponseEntity<String> response = restTemplate.getForEntity("/admin/orders", String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void adminEndpointWithWrongTokenReturns401() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth("wrong-token");
        ResponseEntity<String> response = restTemplate.exchange(
                "/admin/orders", HttpMethod.GET, new HttpEntity<>(headers), String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void createCounterOrderWithoutTableIdReturns409() {
        CreateOrderRequest request = new CreateOrderRequest(
                UUID.randomUUID(), null, List.of(new CreateOrderItemRequest(menu.getId(), 1, null)));
        ResponseEntity<String> response = restTemplate.exchange(
                "/admin/orders", HttpMethod.POST, new HttpEntity<>(request, authHeaders()), String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void listOrdersReturnsCreatedOrder() {
        createCounterOrder();

        ResponseEntity<OrderResponse[]> response = restTemplate.exchange(
                "/admin/orders?status=RECEIVED", HttpMethod.GET, new HttpEntity<>(authHeaders()), OrderResponse[].class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).anyMatch(o -> o.status().equals("RECEIVED"));
    }

    @Test
    void ackIsIdempotent() {
        OrderResponse order = createCounterOrder();

        ResponseEntity<Void> first = restTemplate.exchange(
                "/admin/orders/" + order.id() + "/ack", HttpMethod.POST, new HttpEntity<>(authHeaders()), Void.class);
        assertThat(first.getStatusCode()).isEqualTo(HttpStatus.OK);

        ResponseEntity<Void> second = restTemplate.exchange(
                "/admin/orders/" + order.id() + "/ack", HttpMethod.POST, new HttpEntity<>(authHeaders()), Void.class);
        assertThat(second.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void completeTwiceReturns409OnSecondCall() {
        OrderResponse order = createCounterOrder();

        ResponseEntity<Void> first = restTemplate.exchange(
                "/admin/orders/" + order.id() + "/complete", HttpMethod.POST, new HttpEntity<>(authHeaders()), Void.class);
        assertThat(first.getStatusCode()).isEqualTo(HttpStatus.OK);

        ResponseEntity<String> second = restTemplate.exchange(
                "/admin/orders/" + order.id() + "/complete", HttpMethod.POST, new HttpEntity<>(authHeaders()), String.class);
        assertThat(second.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void cancelWithoutMarkSoldOutOnlyCancelsItem() {
        OrderResponse order = createCounterOrder();
        Long itemId = order.items().get(0).id();

        CancelOrderItemRequest request = new CancelOrderItemRequest(false);
        ResponseEntity<OrderItemResponse> response = restTemplate.exchange(
                "/admin/order-items/" + itemId + "/cancel", HttpMethod.POST,
                new HttpEntity<>(request, authHeaders()), OrderItemResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().status()).isEqualTo("CANCELED");

        Menu reloaded = menuRepository.findById(menu.getId()).orElseThrow();
        assertThat(reloaded.getSoldOutDate()).isNull();
    }

    @Test
    void cancelWithMarkSoldOutMarksMenuSoldOutAndCompletesOrder() {
        OrderResponse order = createCounterOrder();
        Long itemId = order.items().get(0).id();

        CancelOrderItemRequest request = new CancelOrderItemRequest(true);
        ResponseEntity<OrderItemResponse> response = restTemplate.exchange(
                "/admin/order-items/" + itemId + "/cancel", HttpMethod.POST,
                new HttpEntity<>(request, authHeaders()), OrderItemResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        Menu reloaded = menuRepository.findById(menu.getId()).orElseThrow();
        assertThat(reloaded.getSoldOutDate()).isNotNull();

        // 유일한 품목이 취소됐으니 주문도 자동 COMPLETED (design.md §3)
        ResponseEntity<OrderResponse[]> listResponse = restTemplate.exchange(
                "/admin/orders?status=COMPLETED", HttpMethod.GET, new HttpEntity<>(authHeaders()), OrderResponse[].class);
        assertThat(listResponse.getBody()).anyMatch(o -> o.id().equals(order.id()));
    }

    @Test
    void restoreMenuClearsSoldOutDate() {
        menu.markSoldOut(java.time.LocalDate.now());
        menuRepository.saveAndFlush(menu);

        ResponseEntity<MenuDto> response = restTemplate.exchange(
                "/admin/menus/" + menu.getId() + "/restore", HttpMethod.POST, new HttpEntity<>(authHeaders()), MenuDto.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().soldOutDate()).isNull();
    }
}
