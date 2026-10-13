package com.kkamar.cnh.controller;

import com.kkamar.cnh.dto.NoticeResponse;
import com.kkamar.cnh.dto.PageResponse;
import com.kkamar.cnh.service.FeedService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class FeedController {

    private final FeedService feedService;

    @GetMapping("/api/feed")
    public PageResponse<NoticeResponse> getFeed(
            @RequestParam Long deviceId,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        return feedService.getFeed(deviceId, page, size);
    }
}
