package com.electronyoon.tableorder.service;

import com.electronyoon.tableorder.domain.device.Device;
import com.electronyoon.tableorder.domain.device.DeviceRepository;
import com.electronyoon.tableorder.domain.device.DeviceRole;
import java.time.OffsetDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DeviceService {

    private final DeviceRepository deviceRepository;

    public DeviceService(DeviceRepository deviceRepository) {
        this.deviceRepository = deviceRepository;
    }

    @Transactional
    public Device register(String name, DeviceRole role, String fcmToken) {
        Device device = new Device();
        device.setName(name);
        device.setRole(role);
        device.setFcmToken(fcmToken);
        device.setLastSeenAt(OffsetDateTime.now());
        return deviceRepository.save(device);
    }
}
