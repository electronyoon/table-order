package com.electronyoon.tableorder.service;

import com.electronyoon.tableorder.common.BusinessDayCalculator;
import com.electronyoon.tableorder.domain.menu.Menu;
import com.electronyoon.tableorder.domain.menu.MenuCategoryRepository;
import com.electronyoon.tableorder.domain.menu.MenuRepository;
import com.electronyoon.tableorder.web.dto.MenuBoardResponse;
import com.electronyoon.tableorder.web.dto.MenuCategoryDto;
import com.electronyoon.tableorder.web.dto.MenuDto;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MenuService {

    private final MenuCategoryRepository menuCategoryRepository;
    private final MenuRepository menuRepository;
    private final BusinessDayCalculator businessDayCalculator;

    public MenuService(
            MenuCategoryRepository menuCategoryRepository,
            MenuRepository menuRepository,
            BusinessDayCalculator businessDayCalculator
    ) {
        this.menuCategoryRepository = menuCategoryRepository;
        this.menuRepository = menuRepository;
        this.businessDayCalculator = businessDayCalculator;
    }

    @Transactional(readOnly = true)
    public MenuBoardResponse getMenuBoard() {
        List<MenuCategoryDto> categories = menuCategoryRepository.findAllByOrderBySortOrderAsc().stream()
                .map(category -> new MenuCategoryDto(category.getId(), category.getName(), category.getSortOrder()))
                .toList();

        List<MenuDto> menus = menuRepository.findAllByOrderBySortOrderAsc().stream()
                .map(this::toDto)
                .toList();

        return new MenuBoardResponse(categories, menus);
    }

    private MenuDto toDto(Menu menu) {
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

    /** 품절 판정: sold_out_date == 오늘 영업일. */
    public boolean isSoldOut(Menu menu) {
        return businessDayCalculator.isSoldOutToday(menu.getSoldOutDate());
    }
}
