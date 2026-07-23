package com.electronyoon.tableorder.service;

import com.electronyoon.tableorder.domain.busymode.BusyModeSetting;
import com.electronyoon.tableorder.domain.busymode.BusyModeSettingRepository;
import java.time.OffsetDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** V3 마이그레이션이 만들어둔 단일 row를 갱신한다. */
@Service
public class BusyModeService {

    private final BusyModeSettingRepository busyModeSettingRepository;

    public BusyModeService(BusyModeSettingRepository busyModeSettingRepository) {
        this.busyModeSettingRepository = busyModeSettingRepository;
    }

    @Transactional
    public void setEnabled(boolean enabled) {
        BusyModeSetting setting = busyModeSettingRepository.findAll().stream()
                .findFirst()
                .orElseGet(BusyModeSetting::new);
        setting.setEnabled(enabled);
        setting.setUpdatedAt(OffsetDateTime.now());
        busyModeSettingRepository.save(setting);
    }
}
