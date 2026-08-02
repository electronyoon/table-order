package com.electronyoon.tableorder.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.electronyoon.tableorder.TestcontainersConfiguration;
import com.electronyoon.tableorder.domain.menu.Menu;
import com.electronyoon.tableorder.domain.menu.MenuCategory;
import com.electronyoon.tableorder.domain.menu.MenuCategoryRepository;
import com.electronyoon.tableorder.domain.menu.MenuRepository;
import com.electronyoon.tableorder.domain.order.Order;
import com.electronyoon.tableorder.domain.order.OrderItem;
import com.electronyoon.tableorder.domain.order.OrderItemStatus;
import com.electronyoon.tableorder.domain.order.OrderRepository;
import com.electronyoon.tableorder.domain.order.OrderSource;
import com.electronyoon.tableorder.domain.order.OrderStatus;
import com.electronyoon.tableorder.domain.payment.Payment;
import com.electronyoon.tableorder.domain.payment.PaymentRepository;
import com.electronyoon.tableorder.domain.session.TableSession;
import com.electronyoon.tableorder.domain.session.TableSessionRepository;
import com.electronyoon.tableorder.domain.storetable.StoreTable;
import com.electronyoon.tableorder.domain.storetable.StoreTableRepository;
import java.time.OffsetDateTime;
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
    private PaymentRepository paymentRepository;

    @Test
    void 엔티티가_스키마에_매핑되고_저장된다() {
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

        Payment payment = new Payment();
        payment.setSession(session);
        payment.setMethod("CASH");
        payment.setStatus("PAID");
        payment.setAmount(18000);
        payment.setCreatedAt(OffsetDateTime.now());
        paymentRepository.saveAndFlush(payment);
    }

    @Test
    void 테이블당_OPEN_세션은_하나만_허용된다() {
        StoreTable table = new StoreTable();
        table.setLabel("5번");
        table.setQrToken("qr-" + UUID.randomUUID());
        table = storeTableRepository.saveAndFlush(table);

        tableSessionRepository.saveAndFlush(TableSession.open(table));

        StoreTable finalTable = table;
        assertThatThrownBy(() -> tableSessionRepository.saveAndFlush(TableSession.open(finalTable)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void Order_생성시_상태는_RECEIVED다() {
        Order order = Order.create(TableSession.open(new StoreTable()), OrderSource.COUNTER, UUID.randomUUID());
        assertThat(order.getStatus()).isEqualTo(OrderStatus.RECEIVED);
    }

    @Test
    void OrderItem_생성시_상태는_ACTIVE다() {
        Menu menu = new Menu();
        menu.setName("제육볶음");
        menu.setPrice(9000);

        OrderItem item = OrderItem.fromMenu(menu, 1, null);

        assertThat(item.getStatus()).isEqualTo(OrderItemStatus.ACTIVE);
    }

    @Test
    void idempotency_key는_UNIQUE_제약을_가진다() {
        StoreTable table = new StoreTable();
        table.setLabel("7번");
        table.setQrToken("qr-" + UUID.randomUUID());
        table = storeTableRepository.saveAndFlush(table);
        TableSession session = tableSessionRepository.saveAndFlush(TableSession.open(table));

        UUID duplicateKey = UUID.randomUUID();
        orderRepository.saveAndFlush(Order.create(session, OrderSource.COUNTER, duplicateKey));

        TableSession finalSession = session;
        assertThatThrownBy(() ->
                        orderRepository.saveAndFlush(Order.create(finalSession, OrderSource.QR, duplicateKey)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void qr_token은_UNIQUE_제약을_가진다() {
        String duplicateToken = "qr-" + UUID.randomUUID();

        StoreTable first = new StoreTable();
        first.setLabel("8번");
        first.setQrToken(duplicateToken);
        storeTableRepository.saveAndFlush(first);

        StoreTable second = new StoreTable();
        second.setLabel("9번");
        second.setQrToken(duplicateToken);
        assertThatThrownBy(() -> storeTableRepository.saveAndFlush(second))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
