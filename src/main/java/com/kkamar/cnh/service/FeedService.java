package com.kkamar.cnh.service;

import com.kkamar.cnh.dto.NoticeResponse;
import com.kkamar.cnh.dto.PageResponse;

public interface FeedService {
    PageResponse<NoticeResponse> getFeed(Long deviceId, int page, int size);
}
