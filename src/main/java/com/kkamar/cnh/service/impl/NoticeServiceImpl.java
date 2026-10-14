package com.kkamar.cnh.service.impl;

import com.kkamar.cnh.dto.NoticeResponse;
import com.kkamar.cnh.dto.PageResponse;
import com.kkamar.cnh.repository.NoticeRepository;
import com.kkamar.cnh.service.NoticeService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NoticeServiceImpl implements NoticeService {

    /** 최신 게시일 순. 같은 날짜 안에서는 id 역순으로 정렬이 항상 같게 유지된다(페이지 경계에서 중복/누락 방지). */
    static final Sort LATEST_FIRST = Sort.by(Sort.Order.desc("postedAt"), Sort.Order.desc("id"));

    private final NoticeRepository noticeRepository;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<NoticeResponse> getNotices(int page, int size) {
        return PageResponse.from(
                noticeRepository.findAll(PageRequest.of(page, size, LATEST_FIRST))
                        .map(NoticeResponse::from));
    }
}
