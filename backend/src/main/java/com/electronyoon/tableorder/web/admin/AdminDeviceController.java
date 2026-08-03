package com.electronyoon.tableorder.web.admin;

import com.electronyoon.tableorder.domain.device.Device;
import com.electronyoon.tableorder.service.DeviceService;
import com.electronyoon.tableorder.web.dto.DeviceDto;
import com.electronyoon.tableorder.web.dto.RegisterDeviceRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AdminDeviceController {

    private final DeviceService deviceService;

    public AdminDeviceController(DeviceService deviceService) {
        this.deviceService = deviceService;
    }

    @PostMapping("/admin/devices")
    public ResponseEntity<DeviceDto> registerDevice(@Valid @RequestBody RegisterDeviceRequest request) {
        Device device = deviceService.register(request.name(), request.role(), request.fcmToken());
        return ResponseEntity.status(HttpStatus.CREATED).body(DeviceDto.from(device));
    }
}
