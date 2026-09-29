package com.kkamar.cnh.controller;

import com.kkamar.cnh.dto.NoticeResponse;
import com.kkamar.cnh.service.FeedService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class FeedController {

    private final FeedService feedService;

    @GetMapping("/api/feed")
    public List<NoticeResponse> getFeed(@RequestParam Long deviceId) {
        return feedService.getFeed(deviceId);
    }
}