package com.electronyoon.tableorder.domain.busymode;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** 단일 행만 사용한다 (V3 마이그레이션이 초기 row를 만들어둔다). */
@Entity
@Table(name = "busy_mode")
@Getter
@Setter
@NoArgsConstructor
public class BusyModeSetting {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private boolean enabled;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
}
