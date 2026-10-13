package com.kkamar.cnh.service.impl;

import com.kkamar.cnh.domain.Device;
import com.kkamar.cnh.domain.Notice;
import com.kkamar.cnh.domain.Scrap;
import com.kkamar.cnh.dto.ScrapResponse;
import com.kkamar.cnh.exception.ResourceNotFoundException;
import com.kkamar.cnh.repository.DeviceRepository;
import com.kkamar.cnh.repository.NoticeRepository;
import com.kkamar.cnh.repository.ScrapRepository;
import com.kkamar.cnh.service.ScrapService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ScrapServiceImpl implements ScrapService {

    static final int COLOR_PALETTE_SIZE = 8;

    private final ScrapRepository scrapRepository;
    private final DeviceRepository deviceRepository;
    private final NoticeRepository noticeRepository;
    private final PlatformTransactionManager transactionManager;

    @Override
    public ScrapResponse scrap(Long deviceId, Long noticeId, Integer colorIndex) {
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        try {
            return tx.execute(status -> createOrGet(deviceId, noticeId, colorIndex));
        } catch (DataIntegrityViolationException e) {
            // 같은 스크랩 요청이 동시에 두 번 들어온 경우: 먼저 저장된 행을 돌려준다 (멱등)
            log.info("Concurrent scrap detected, returning the existing row: deviceId={}, noticeId={}", deviceId, noticeId);
            return tx.execute(status -> ScrapResponse.from(
                    scrapRepository.findByDeviceIdAndNoticeId(deviceId, noticeId).orElseThrow(() -> e)));
        }
    }

    private ScrapResponse createOrGet(Long deviceId, Long noticeId, Integer colorIndex) {
        Device device = deviceRepository.findById(deviceId)
                .orElseThrow(() -> new ResourceNotFoundException("Device not found: id=" + deviceId));
        Notice notice = noticeRepository.findById(noticeId)
                .orElseThrow(() -> new ResourceNotFoundException("Notice not found: id=" + noticeId));

        Scrap existing = scrapRepository.findByDeviceIdAndNoticeId(deviceId, noticeId).orElse(null);
        if (existing != null) {
            return ScrapResponse.from(existing);
        }

        int color = colorIndex != null
                ? colorIndex
                : (int) (scrapRepository.countByDeviceId(deviceId) % COLOR_PALETTE_SIZE);

        Scrap saved = scrapRepository.save(Scrap.builder()
                .device(device)
                .notice(notice)
                .colorIndex(color)
                .build());
        log.info("Scrapped: deviceId={}, noticeId={}, colorIndex={}", deviceId, noticeId, color);
        return ScrapResponse.from(saved);
    }

    @Override
    @Transactional
    public void unscrap(Long deviceId, Long noticeId) {
        scrapRepository.deleteByDeviceIdAndNoticeId(deviceId, noticeId);
        log.info("Unscrapped: deviceId={}, noticeId={}", deviceId, noticeId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ScrapResponse> getScraps(Long deviceId) {
        if (!deviceRepository.existsById(deviceId)) {
            throw new ResourceNotFoundException("Device not found: id=" + deviceId);
        }
        return scrapRepository.findByDeviceIdOrderByCreatedAtDescIdDesc(deviceId).stream()
                .map(ScrapResponse::from)
                .toList();
    }
}
