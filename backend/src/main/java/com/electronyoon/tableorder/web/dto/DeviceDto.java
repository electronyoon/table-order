package com.electronyoon.tableorder.web.dto;

import com.electronyoon.tableorder.domain.device.Device;
import java.time.OffsetDateTime;

public record DeviceDto(
        Long id,
        String name,
        String role,
        String fcmToken,
        OffsetDateTime lastSeenAt
) {

    public static DeviceDto from(Device device) {
        return new DeviceDto(
                device.getId(),
                device.getName(),
                device.getRole().name(),
                device.getFcmToken(),
                device.getLastSeenAt()
        );
    }
}
