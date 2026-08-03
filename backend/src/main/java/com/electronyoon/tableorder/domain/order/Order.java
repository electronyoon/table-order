package com.electronyoon.tableorder.domain.order;

import com.electronyoon.tableorder.domain.session.TableSession;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "orders")
@Getter
@NoArgsConstructor
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private TableSession session;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private OrderSource source;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private OrderStatus status;

    @Column(name = "idempotency_key", nullable = false, unique = true)
    private UUID idempotencyKey;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    /** ACK는 가게 단위(design.md §5) — 한 번이라도 화면에 표시되면 끝. V2 마이그레이션. */
    @Column(name = "acked_at")
    private OffsetDateTime ackedAt;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = false, fetch = FetchType.LAZY)
    @OrderBy("id ASC")
    private List<OrderItem> items = new ArrayList<>();

    public static Order create(TableSession session, OrderSource source, UUID idempotencyKey) {
        Order order = new Order();
        order.session = session;
        order.source = source;
        order.status = OrderStatus.RECEIVED;
        order.idempotencyKey = idempotencyKey;
        order.createdAt = OffsetDateTime.now();
        return order;
    }

    public void addItem(OrderItem item) {
        item.assignOrder(this);
        this.items.add(item);
    }

    /** 남은 ACTIVE 품목이 없으면(전부 취소) 주문도 자동 완료 처리. */
    public boolean hasNoActiveItems() {
        return items.stream().noneMatch(item -> item.getStatus() == OrderItemStatus.ACTIVE);
    }

    public void complete() {
        this.status = OrderStatus.COMPLETED;
    }
}
