package com.electronyoon.tableorder.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.electronyoon.tableorder.TestcontainersConfiguration;
import com.electronyoon.tableorder.domain.device.Device;
import com.electronyoon.tableorder.domain.device.DeviceRepository;
import com.electronyoon.tableorder.domain.device.DeviceRole;
import com.electronyoon.tableorder.domain.menu.Menu;
import com.electronyoon.tableorder.domain.menu.MenuCategory;
import com.electronyoon.tableorder.domain.menu.MenuCategoryRepository;
import com.electronyoon.tableorder.domain.menu.MenuRepository;
import com.electronyoon.tableorder.domain.order.Order;
import com.electronyoon.tableorder.domain.order.OrderItem;
import com.electronyoon.tableorder.domain.order.OrderRepository;
import com.electronyoon.tableorder.domain.order.OrderSource;
import com.electronyoon.tableorder.domain.outbox.OutboxEvent;
import com.electronyoon.tableorder.domain.outbox.OutboxEventRepository;
import com.electronyoon.tableorder.domain.payment.Payment;
import com.electronyoon.tableorder.domain.payment.PaymentRepository;
import com.electronyoon.tableorder.domain.session.TableSession;
import com.electronyoon.tableorder.domain.session.TableSessionRepository;
import com.electronyoon.tableorder.domain.storetable.StoreTable;
import com.electronyoon.tableorder.domain.storetable.StoreTableRepository;
import jakarta.persistence.EntityManager;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class DomainEntityMappingTest {

    @Autowired
    private StoreTableRepository storeTableRepository;
    @Autowired
    private TableSessionRepository tableSessionRepository;
    @Autowired
    private MenuCategoryRepository menuCategoryRepository;
    @Autowired
    private MenuRepository menuRepository;
    @Autowired
    private OrderRepository orderRepository;
    @Autowired
    private DeviceRepository deviceRepository;
    @Autowired
    private OutboxEventRepository outboxEventRepository;
    @Autowired
    private PaymentRepository paymentRepository;
    @Autowired
    private EntityManager entityManager;

    @Test
    void entitiesMapToSchemaAndPersistCorrectly() {
        StoreTable table = new StoreTable();
        table.setLabel("3번");
        table.setQrToken("qr-" + UUID.randomUUID());
        table = storeTableRepository.saveAndFlush(table);

        TableSession session = TableSession.open(table);
        session = tableSessionRepository.saveAndFlush(session);

        MenuCategory category = new MenuCategory();
        category.setName("메인");
        category.setSortOrder(1);
        category = menuCategoryRepository.saveAndFlush(category);

        Menu menu = new Menu();
        menu.setCategory(category);
        menu.setName("제육볶음");
        menu.setPrice(9000);
        menu.setSortOrder(1);
        menu.setSelfService(false);
        menu = menuRepository.saveAndFlush(menu);

        Order order = Order.create(session, OrderSource.COUNTER, UUID.randomUUID());
        order.addItem(OrderItem.fromMenu(menu, 2, null));
        order = orderRepository.saveAndFlush(order);

        assertThat(order.getId()).isNotNull();
        assertThat(order.getItems()).hasSize(1);
        assertThat(order.getItems().get(0).getMenuName()).isEqualTo("제육볶음");
        assertThat(order.getItems().get(0).getUnitPrice()).isEqualTo(9000);

        Device device = new Device();
        device.setName("주방 태블릿");
        device.setRole(DeviceRole.PRIMARY);
        device.setFcmToken("token-abc");
        device = deviceRepository.saveAndFlush(device);
        assertThat(device.getId()).isNotNull();

        OutboxEvent event = OutboxEvent.create("ORDER_CREATED", "{\"orderId\":" + order.getId() + "}");
        event = outboxEventRepository.saveAndFlush(event);
        entityManager.clear();

        OutboxEvent reloaded = outboxEventRepository.findById(event.getId()).orElseThrow();
        // jsonb 컬럼은 저장 시 공백 등 포맷을 정규화하므로 문자열이 아니라 JSON 값으로 비교한다.
        assertThat(reloaded.getPayload()).isEqualToIgnoringWhitespace("{\"orderId\": " + order.getId() + "}");

        Payment payment = new Payment();
        payment.setSession(session);
        payment.setMethod("CASH");
        payment.setStatus("PAID");
        payment.setAmount(18000);
        payment.setCreatedAt(java.time.OffsetDateTime.now());
        paymentRepository.saveAndFlush(payment);
    }

    @Test
    void onlyOneOpenSessionAllowedPerTable() {
        StoreTable table = new StoreTable();
        table.setLabel("5번");
        table.setQrToken("qr-" + UUID.randomUUID());
        table = storeTableRepository.saveAndFlush(table);

        tableSessionRepository.saveAndFlush(TableSession.open(table));

        StoreTable finalTable = table;
        assertThatThrownBy(() -> tableSessionRepository.saveAndFlush(TableSession.open(finalTable)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
