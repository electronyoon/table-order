package com.electronyoon.tableorder.domain.payment;

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
 * 카운터 후불 정산 결제. 세션 CLOSE 시 세션 단위로 1건 기록한다 (design.md §4 — 선불/PG는
 * v0.3에서 스코프 제외). 결제는 세션 전속이므로 session은 항상 채워진다.
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
    @JoinColumn(name = "session_id", nullable = false)
    private TableSession session;

    @Column(nullable = false, length = 20)
    private String method;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(nullable = false)
    private int amount;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;
}
