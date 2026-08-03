package com.electronyoon.tableorder.web.dto;

import com.electronyoon.tableorder.domain.menu.Menu;
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

    public static MenuDto from(Menu menu) {
        return new MenuDto(
                menu.getId(),
                menu.getCategory().getId(),
                menu.getName(),
                menu.getPrice(),
                menu.getSortOrder(),
                menu.isSelfService(),
                menu.getSoldOutDate()
        );
    }
}
