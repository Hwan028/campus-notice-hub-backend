package com.kkamar.cnh.service.impl;

import com.kkamar.cnh.domain.Device;
import com.kkamar.cnh.domain.Notice;
import com.kkamar.cnh.dto.NoticeResponse;
import com.kkamar.cnh.dto.PageResponse;
import com.kkamar.cnh.exception.ResourceNotFoundException;
import com.kkamar.cnh.repository.DeviceKeywordRepository;
import com.kkamar.cnh.repository.DeviceRepository;
import com.kkamar.cnh.repository.NoticeRepository;
import com.kkamar.cnh.service.FeedService;
import com.kkamar.cnh.util.NoticeSpecifications;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FeedServiceImpl implements FeedService {

    private final DeviceRepository deviceRepository;
    private final DeviceKeywordRepository deviceKeywordRepository;
    private final NoticeRepository noticeRepository;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<NoticeResponse> getFeed(Long deviceId, int page, int size) {
        Device device = deviceRepository.findById(deviceId)
                .orElseThrow(() -> new ResourceNotFoundException("Device not found: id=" + deviceId));

        // 1단계: 학과 스코프 (전체공지 + 내 학과 게시판) - 항상 적용
        Specification<Notice> spec = NoticeSpecifications.inScope(device.getDepartment());

        // 2단계: 구독 키워드 필터 - 구독한 키워드가 있을 때만 (없으면 신규 사용자의 피드가 비어 보인다)
        List<String> keywords = deviceKeywordRepository.findKeywordNamesByDeviceId(deviceId);
        if (!keywords.isEmpty()) {
            spec = spec.and(NoticeSpecifications.titleContainsAny(keywords));
        }

        Page<Notice> result = noticeRepository.findAll(spec,
                PageRequest.of(page, size, NoticeServiceImpl.LATEST_FIRST));
        return PageResponse.from(result.map(NoticeResponse::from));
    }
}
