package com.electronyoon.tableorder.web.dto;

import java.util.List;

public record MenuBoardResponse(List<MenuCategoryDto> categories, List<MenuDto> menus) {
}
