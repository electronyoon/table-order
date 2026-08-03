package com.electronyoon.tableorder.web.dto;

import jakarta.validation.constraints.NotNull;

public record BusyModeRequest(@NotNull Boolean enabled) {
}
