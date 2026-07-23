package com.electronyoon.tableorder.web.dto;

import com.electronyoon.tableorder.domain.device.DeviceRole;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record RegisterDeviceRequest(
        @NotBlank String name,
        @NotNull DeviceRole role,
        @NotBlank String fcmToken
) {
}
