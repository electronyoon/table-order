package com.electronyoon.tableorder.domain.payment;

import com.electronyoon.tableorder.domain.order.Order;
import com.electronyoon.tableorder.domain.session.TableSession;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * PG 연동 이전까지는 엔티티만 존재하고 API/로직은 없다 (design.md §4/§9 — PG 도입은
 * 실사용 이후 별도 단계). session_id / order_id 중 정확히 하나만 채워진다
 * (DB CHECK 제약 ck_payment_session_xor_order).
 */
@Entity
@Table(name = "payment")
@Getter
@Setter
@NoArgsConstructor
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id")
    private TableSession session;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    private Order order;

    @Column(nullable = false, length = 20)
    private String method;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(nullable = false)
    private int amount;

    @Column(name = "pg_provider", length = 50)
    private String pgProvider;

    @Column(name = "pg_tid", length = 100)
    private String pgTid;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;
}
