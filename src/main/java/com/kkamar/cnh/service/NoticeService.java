package com.kkamar.cnh.service;

import com.kkamar.cnh.dto.NoticeResponse;
import com.kkamar.cnh.dto.PageResponse;

public interface NoticeService {
    PageResponse<NoticeResponse> getNotices(int page, int size);
}
