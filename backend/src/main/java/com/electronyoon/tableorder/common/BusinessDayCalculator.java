package com.electronyoon.tableorder.common;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import org.springframework.stereotype.Component;

/**
 * design.md §2 "당일 품절 자동 해제 트릭" 계산 로직.
 * 영업일 시작 시각(기본 06:00) 이전이면 전날을, 그 외엔 오늘을 영업일로 본다.
 * 품절 판정(menu.sold_out_date == 오늘 영업일)과 품절 설정 양쪽에서 재사용한다.
 */
@Component
public class BusinessDayCalculator {

    private static final LocalTime BUSINESS_DAY_START = LocalTime.of(6, 0);

    private final Clock clock;

    public BusinessDayCalculator() {
        this.clock = Clock.systemDefaultZone();
    }

    public BusinessDayCalculator(Clock clock) {
        this.clock = clock;
    }

    public LocalDate today() {
        LocalDateTime now = LocalDateTime.now(clock);
        return now.toLocalTime().isBefore(BUSINESS_DAY_START) ? now.toLocalDate().minusDays(1) : now.toLocalDate();
    }

    public boolean isSoldOutToday(LocalDate soldOutDate) {
        return soldOutDate != null && soldOutDate.equals(today());
    }
}
