package com.kkamar.cnh.service;

import com.kkamar.cnh.dto.NoticeResponse;

import java.util.List;

public interface FeedService {
    List<NoticeResponse> getFeed(Long deviceId);
}