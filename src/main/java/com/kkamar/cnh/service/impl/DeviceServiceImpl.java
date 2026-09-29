package com.kkamar.cnh.service.impl;

import com.kkamar.cnh.domain.Department;
import com.kkamar.cnh.domain.Device;
import com.kkamar.cnh.domain.DeviceKeyword;
import com.kkamar.cnh.domain.Keyword;
import com.kkamar.cnh.dto.DeviceResponse;
import com.kkamar.cnh.repository.DepartmentRepository;
import com.kkamar.cnh.repository.DeviceKeywordRepository;
import com.kkamar.cnh.repository.DeviceRepository;
import com.kkamar.cnh.repository.KeywordRepository;
import com.kkamar.cnh.service.DeviceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeviceServiceImpl implements DeviceService {

    private final DeviceRepository deviceRepository;
    private final DepartmentRepository departmentRepository;
    private final KeywordRepository keywordRepository;
    private final DeviceKeywordRepository deviceKeywordRepository;

    @Override
    @Transactional
    public DeviceResponse registerDevice(String deviceUuid, Long departmentId) {
        // 이미 등록된 UUID면 새로 만들지 않고 기존 디바이스를 그대로 반환
        Device existing = deviceRepository.findByDeviceUuid(deviceUuid).orElse(null);
        if (existing != null) {
            log.info("Device already registered, returning existing: uuid={}", deviceUuid);
            return DeviceResponse.from(existing);
        }

        Department department = resolveDepartment(departmentId);

        Device device = Device.builder()
                .deviceUuid(deviceUuid)
                .department(department)
                .build();
        deviceRepository.save(device);
        log.info("Registered new device: uuid={}", deviceUuid);

        return DeviceResponse.from(device);
    }

    @Override
    @Transactional
    public DeviceResponse updateDepartment(Long deviceId, Long departmentId) {
        Device device = deviceRepository.findById(deviceId)
                .orElseThrow(() -> new IllegalArgumentException("Device not found: id=" + deviceId));

        Department department = resolveDepartment(departmentId);
        device.changeDepartment(department);

        return DeviceResponse.from(device);
    }

    @Override
    @Transactional
    public void subscribeKeyword(Long deviceId, String keywordName) {
        Device device = deviceRepository.findById(deviceId)
                .orElseThrow(() -> new IllegalArgumentException("Device not found: id=" + deviceId));

        Keyword keyword = keywordRepository.findByName(keywordName)
                .orElseGet(() -> keywordRepository.save(
                        Keyword.builder()
                                .name(keywordName)
                                .isPreset(false)
                                .build()
                ));

        if (deviceKeywordRepository.existsByDeviceIdAndKeywordId(deviceId, keyword.getId())) {
            log.debug("Already subscribed, skipping: deviceId={}, keyword={}", deviceId, keywordName);
            return;
        }

        deviceKeywordRepository.save(
                DeviceKeyword.builder()
                        .device(device)
                        .keyword(keyword)
                        .build()
        );
        log.info("Subscribed keyword: deviceId={}, keyword={}", deviceId, keywordName);
    }

    @Override
    @Transactional
    public void unsubscribeKeyword(Long deviceId, Long keywordId) {
        deviceKeywordRepository.deleteByDeviceIdAndKeywordId(deviceId, keywordId);
        log.info("Unsubscribed keyword: deviceId={}, keywordId={}", deviceId, keywordId);
    }

    private Department resolveDepartment(Long departmentId) {
        if (departmentId == null) {
            return null; // 아직 학과 미선택 상태 허용
        }
        return departmentRepository.findById(departmentId)
                .orElseThrow(() -> new IllegalArgumentException("Department not found: id=" + departmentId));
    }
}