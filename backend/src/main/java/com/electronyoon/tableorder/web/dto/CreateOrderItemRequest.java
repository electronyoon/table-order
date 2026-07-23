package com.electronyoon.tableorder.web.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record CreateOrderItemRequest(
        @NotNull Long menuId,
        @Min(1) int quantity,
        String note
) {
}
