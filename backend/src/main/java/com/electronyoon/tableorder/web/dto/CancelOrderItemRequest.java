package com.electronyoon.tableorder.web.dto;

import jakarta.validation.constraints.NotNull;

public record CancelOrderItemRequest(@NotNull Boolean markSoldOut) {
}
