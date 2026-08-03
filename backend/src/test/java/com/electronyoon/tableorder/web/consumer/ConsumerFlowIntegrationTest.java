package com.electronyoon.tableorder.web.consumer;

import static org.assertj.core.api.Assertions.assertThat;

import com.electronyoon.tableorder.TestcontainersConfiguration;
import com.electronyoon.tableorder.common.BusinessDayCalculator;
import com.electronyoon.tableorder.domain.menu.Menu;
import com.electronyoon.tableorder.domain.menu.MenuCategory;
import com.electronyoon.tableorder.domain.menu.MenuCategoryRepository;
import com.electronyoon.tableorder.domain.menu.MenuRepository;
import com.electronyoon.tableorder.domain.storetable.StoreTable;
import com.electronyoon.tableorder.domain.storetable.StoreTableRepository;
import com.electronyoon.tableorder.web.dto.CreateOrderItemRequest;
import com.electronyoon.tableorder.web.dto.CreateOrderRequest;
import com.electronyoon.tableorder.web.dto.MenuBoardResponse;
import com.electronyoon.tableorder.web.dto.OrderResponse;
import com.electronyoon.tableorder.web.dto.SessionDetailResponse;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
@Import(TestcontainersConfiguration.class)
class ConsumerFlowIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;
    @Autowired
    private StoreTableRepository storeTableRepository;
    @Autowired
    private MenuCategoryRepository menuCategoryRepository;
    @Autowired
    private MenuRepository menuRepository;
    @Autowired
    private BusinessDayCalculator businessDayCalculator;

    private StoreTable table;
    private Menu menu;

    @BeforeEach
    void setUp() {
        table = new StoreTable();
        table.setLabel("테스트 테이블");
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

    @Test
    void getMenuBoardReturnsMenusAndCategories() {
        ResponseEntity<MenuBoardResponse> response =
                restTemplate.getForEntity("/t/" + table.getQrToken(), MenuBoardResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().menus()).anyMatch(m -> m.name().equals("제육볶음"));
    }

    @Test
    void getMenuBoardWithUnknownQrTokenReturns404() {
        ResponseEntity<String> response = restTemplate.getForEntity("/t/no-such-token", String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void createOrderThenRetryWithSameIdempotencyKeyReturnsSameOrder() {
        CreateOrderRequest request = new CreateOrderRequest(
                UUID.randomUUID(),
                null,
                List.of(new CreateOrderItemRequest(menu.getId(), 2, null))
        );

        ResponseEntity<OrderResponse> first =
                restTemplate.postForEntity("/t/" + table.getQrToken() + "/orders", request, OrderResponse.class);
        assertThat(first.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(first.getBody().status()).isEqualTo("RECEIVED");
        assertThat(first.getBody().items()).hasSize(1);
        assertThat(first.getBody().items().get(0).unitPrice()).isEqualTo(9000);

        ResponseEntity<OrderResponse> retry =
                restTemplate.postForEntity("/t/" + table.getQrToken() + "/orders", request, OrderResponse.class);
        assertThat(retry.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(retry.getBody().id()).isEqualTo(first.getBody().id());
    }

    @Test
    void createOrderForSoldOutMenuReturns409() {
        // LocalDate.now()가 아니라 영업일(컷오프 06:00 기준)로 품절 설정해야 한다 —
        // 자정~06:00 사이에 테스트를 돌리면 실제 영업일은 어제이기 때문 (실제로 이 시간대에
        // 재현된 버그).
        menu.markSoldOut(businessDayCalculator.today());
        menuRepository.saveAndFlush(menu);

        CreateOrderRequest request = new CreateOrderRequest(
                UUID.randomUUID(),
                null,
                List.of(new CreateOrderItemRequest(menu.getId(), 1, null))
        );

        ResponseEntity<String> response =
                restTemplate.postForEntity("/t/" + table.getQrToken() + "/orders", request, String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void getSessionReturnsOrdersAfterOrderCreated() {
        CreateOrderRequest request = new CreateOrderRequest(
                UUID.randomUUID(),
                null,
                List.of(new CreateOrderItemRequest(menu.getId(), 1, null))
        );
        restTemplate.postForEntity("/t/" + table.getQrToken() + "/orders", request, OrderResponse.class);

        ResponseEntity<SessionDetailResponse> response = restTemplate.getForEntity(
                "/t/" + table.getQrToken() + "/session", SessionDetailResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().session().status()).isEqualTo("OPEN");
        assertThat(response.getBody().orders()).hasSize(1);
    }

    @Test
    void getSessionWithoutAnyOrderReturns404() {
        ResponseEntity<String> response =
                restTemplate.getForEntity("/t/" + table.getQrToken() + "/session", String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }
}
