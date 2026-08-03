package com.electronyoon.tableorder.web.admin;

import com.electronyoon.tableorder.service.BusyModeService;
import com.electronyoon.tableorder.web.dto.BusyModeRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AdminBusyModeController {

    private final BusyModeService busyModeService;

    public AdminBusyModeController(BusyModeService busyModeService) {
        this.busyModeService = busyModeService;
    }

    @PostMapping("/admin/busy-mode")
    public ResponseEntity<Void> setBusyMode(@Valid @RequestBody BusyModeRequest request) {
        busyModeService.setEnabled(request.enabled());
        return ResponseEntity.ok().build();
    }
}
