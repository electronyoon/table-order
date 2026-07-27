package com.electronyoon.tableorder.domain.outbox;

import com.electronyoon.tableorder.domain.device.Device;
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
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * 주문 저장과 알림 발송을 분리하기 위한 outbox 패턴 (design.md §6).
 * 이번 단계에서는 row 생성까지만 하고, 실제 FCM dispatch(스케줄러)는 구현하지 않는다 —
 * Firebase Admin SDK 크리덴셜이 필요한 별도 작업이라 Phase 1 범위 밖.
 */
@Entity
@Table(name = "outbox_event")
@Getter
@NoArgsConstructor
public class OutboxEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String type;

    /** JSON 문자열 (jsonb 컬럼). 호출부에서 ObjectMapper로 직렬화해서 넣는다. */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private OutboxEventStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "acked_by_device_id")
    private Device ackedByDevice;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    public static OutboxEvent create(String type, String payloadJson) {
        OutboxEvent event = new OutboxEvent();
        event.type = type;
        event.payload = payloadJson;
        event.status = OutboxEventStatus.NEW;
        event.createdAt = OffsetDateTime.now();
        return event;
    }
}
