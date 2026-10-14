package com.kkamar.cnh.service.impl;

import com.kkamar.cnh.domain.Department;
import com.kkamar.cnh.domain.Device;
import com.kkamar.cnh.domain.DeviceKeyword;
import com.kkamar.cnh.domain.Keyword;
import com.kkamar.cnh.dto.DeviceResponse;
import com.kkamar.cnh.exception.ResourceNotFoundException;
import com.kkamar.cnh.repository.DepartmentRepository;
import com.kkamar.cnh.repository.DeviceKeywordRepository;
import com.kkamar.cnh.repository.DeviceRepository;
import com.kkamar.cnh.repository.KeywordRepository;
import com.kkamar.cnh.service.DeviceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * 동시성 처리 방침
 * "조회 후 없으면 삽입"(check-then-insert)은 두 요청이 동시에 오면 둘 다 "없음"을 보고 둘 다 삽입을 시도한다.
 * 최종 방어선은 DB의 UNIQUE 제약이고, 서비스는 그 제약 위반을 "다른 요청이 먼저 저장했다"는 신호로 받아들여
 * 새 트랜잭션에서 승자의 행을 다시 조회해 돌려준다.
 * (제약 위반이 난 트랜잭션은 롤백 전용이 되어 같은 트랜잭션 안에서 재조회할 수 없으므로,
 *  메서드 전체를 @Transactional로 묶지 않고 TransactionTemplate으로 단계를 나눴다.)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DeviceServiceImpl implements DeviceService {

    private final DeviceRepository deviceRepository;
    private final DepartmentRepository departmentRepository;
    private final KeywordRepository keywordRepository;
    private final DeviceKeywordRepository deviceKeywordRepository;
    private final PlatformTransactionManager transactionManager;

    @Override
    public DeviceResponse registerDevice(String deviceUuid, Long departmentId) {
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        try {
            return tx.execute(status -> createOrGet(deviceUuid, departmentId));
        } catch (DataIntegrityViolationException e) {
            log.info("Concurrent device registration detected, returning the existing row: uuid={}", deviceUuid);
            return tx.execute(status -> DeviceResponse.from(
                    deviceRepository.findByDeviceUuid(deviceUuid).orElseThrow(() -> e)));
        }
    }

    private DeviceResponse createOrGet(String deviceUuid, Long departmentId) {
        // 이미 등록된 UUID면 새로 만들지 않고 기존 디바이스를 그대로 반환
        Device existing = deviceRepository.findByDeviceUuid(deviceUuid).orElse(null);
        if (existing != null) {
            log.info("Device already registered, returning existing: uuid={}", deviceUuid);
            return DeviceResponse.from(existing);
        }

        Department department = resolveDepartment(departmentId);
        Device device = deviceRepository.save(Device.builder()
                .deviceUuid(deviceUuid)
                .department(department)
                .build());
        log.info("Registered new device: uuid={}", deviceUuid);
        return DeviceResponse.from(device);
    }

    @Override
    @Transactional
    public DeviceResponse updateDepartment(Long deviceId, Long departmentId) {
        Device device = findDevice(deviceId);
        device.changeDepartment(resolveDepartment(departmentId));
        return DeviceResponse.from(device);
    }

    @Override
    public void subscribeKeyword(Long deviceId, String keywordName) {
        String name = keywordName == null ? "" : keywordName.trim();
        if (name.isEmpty()) {
            throw new IllegalArgumentException("keywordName must not be blank");
        }

        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        try {
            tx.executeWithoutResult(status -> doSubscribe(deviceId, name));
        } catch (DataIntegrityViolationException e) {
            // 같은 키워드를 동시에 처음 만들었거나 같은 구독을 동시에 넣은 경우:
            // 이제는 승자가 만든 행이 있으므로 한 번만 재시도하면 재사용/무시 경로로 정상 종료된다.
            log.info("Concurrent keyword subscription detected, retrying once: deviceId={}, keyword={}", deviceId, name);
            tx.executeWithoutResult(status -> doSubscribe(deviceId, name));
        }
    }

    private void doSubscribe(Long deviceId, String keywordName) {
        Device device = findDevice(deviceId);

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
        findDevice(deviceId); // 없는 디바이스면 조용히 성공하지 말고 404
        deviceKeywordRepository.deleteByDeviceIdAndKeywordId(deviceId, keywordId);
        log.info("Unsubscribed keyword: deviceId={}, keywordId={}", deviceId, keywordId);
    }

    private Device findDevice(Long deviceId) {
        return deviceRepository.findById(deviceId)
                .orElseThrow(() -> new ResourceNotFoundException("Device not found: id=" + deviceId));
    }

    private Department resolveDepartment(Long departmentId) {
        if (departmentId == null) {
            return null; // 아직 학과 미선택 상태 허용
        }
        return departmentRepository.findById(departmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Department not found: id=" + departmentId));
    }
}
