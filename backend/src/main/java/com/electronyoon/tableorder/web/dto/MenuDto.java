package com.electronyoon.tableorder.web.dto;

import java.time.LocalDate;

public record MenuDto(
        Long id,
        Long categoryId,
        String name,
        int price,
        int sortOrder,
        boolean isSelfService,
        LocalDate soldOutDate
) {
}
