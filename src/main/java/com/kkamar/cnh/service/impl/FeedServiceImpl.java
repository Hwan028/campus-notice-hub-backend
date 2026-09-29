package com.kkamar.cnh.service.impl;

import com.kkamar.cnh.domain.Device;
import com.kkamar.cnh.domain.DeviceKeyword;
import com.kkamar.cnh.domain.Notice;
import com.kkamar.cnh.dto.NoticeResponse;
import com.kkamar.cnh.repository.DeviceKeywordRepository;
import com.kkamar.cnh.repository.DeviceRepository;
import com.kkamar.cnh.repository.NoticeRepository;
import com.kkamar.cnh.service.FeedService;
import lombok.RequiredArgsConstructor;
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
    public List<NoticeResponse> getFeed(Long deviceId) {
        Device device = deviceRepository.findById(deviceId)
                .orElseThrow(() -> new IllegalArgumentException("Device not found: id=" + deviceId));

        // 1단계: 학과 스코프 필터 (전체공지 + 내 학과 게시판)
        List<Notice> scoped = noticeRepository
                .findByBoard_DepartmentIsNullOrBoard_DepartmentOrderByPostedAtDesc(device.getDepartment());

        // 2단계: 구독 키워드 필터 (구독한 키워드가 없으면 스코프 결과 그대로 반환)
        List<String> subscribedKeywords = deviceKeywordRepository.findByDeviceId(deviceId).stream()
                .map(DeviceKeyword::getKeyword)
                .map(keyword -> keyword.getName())
                .toList();

        if (subscribedKeywords.isEmpty()) {
            return scoped.stream().map(NoticeResponse::from).toList();
        }

        return scoped.stream()
                .filter(notice -> containsAnyKeyword(notice.getTitle(), subscribedKeywords))
                .map(NoticeResponse::from)
                .toList();
    }

    private boolean containsAnyKeyword(String title, List<String> keywords) {
        return keywords.stream().anyMatch(title::contains);
    }
}