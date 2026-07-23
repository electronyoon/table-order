package com.electronyoon.tableorder.web.admin;

import com.electronyoon.tableorder.domain.menu.Menu;
import com.electronyoon.tableorder.service.MenuService;
import com.electronyoon.tableorder.web.dto.MenuDto;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AdminMenuController {

    private final MenuService menuService;

    public AdminMenuController(MenuService menuService) {
        this.menuService = menuService;
    }

    @PostMapping("/admin/menus/{menuId}/restore")
    public MenuDto restoreMenu(@PathVariable Long menuId) {
        Menu menu = menuService.restoreMenu(menuId);
        return MenuDto.from(menu);
    }
}
