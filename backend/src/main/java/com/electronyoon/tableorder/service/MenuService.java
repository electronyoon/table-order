package com.electronyoon.tableorder.service;

import com.electronyoon.tableorder.common.ApiException;
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
                .map(MenuDto::from)
                .toList();

        return new MenuBoardResponse(categories, menus);
    }

    /** 품절 판정: sold_out_date == 오늘 영업일. */
    public boolean isSoldOut(Menu menu) {
        return businessDayCalculator.isSoldOutToday(menu.getSoldOutDate());
    }

    /** 품절 해제. design.md §2 — 착오 품절 복구용 단일 액션. */
    @Transactional
    public Menu restoreMenu(Long menuId) {
        Menu menu = menuRepository.findById(menuId)
                .orElseThrow(() -> ApiException.notFound("존재하지 않는 메뉴입니다."));
        menu.restore();
        return menu;
    }
}
