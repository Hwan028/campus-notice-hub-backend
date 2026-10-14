package com.kkamar.cnh.controller;

import com.kkamar.cnh.component.CrawlerProperties;
import com.kkamar.cnh.domain.CrawlLog;
import com.kkamar.cnh.dto.CrawlLogResponse;
import com.kkamar.cnh.exception.ForbiddenException;
import com.kkamar.cnh.repository.CrawlLogRepository;
import com.kkamar.cnh.service.CrawlHealthService;
import com.kkamar.cnh.service.CrawlerService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;
import java.util.Map;

/**
 * 운영자용 크롤러 관리 API. X-Admin-Token 헤더가 crawler.admin-token 과 일치해야 하며,
 * 토큰이 설정되지 않은 환경에서는 전부 403으로 막힌다(기본값이 "열려 있음"이 되지 않도록).
 */
@RestController
@RequestMapping("/api/admin/crawl")
@RequiredArgsConstructor
public class AdminCrawlController {

    private static final String TOKEN_HEADER = "X-Admin-Token";

    private final CrawlerService crawlerService;
    private final CrawlHealthService crawlHealthService;
    private final CrawlLogRepository crawlLogRepository;
    private final CrawlerProperties properties;

    /** 스케줄러를 기다리지 않고 지금 한 사이클을 실행한다 (동기 실행). */
    @PostMapping("/run")
    public List<CrawlLogResponse> run(@RequestHeader(value = TOKEN_HEADER, required = false) String token) {
        authorize(token);
        crawlerService.crawlAllBoards();
        return toResponses(crawlLogRepository.findAllByOrderByStartedAtDesc(PageRequest.of(0, 20)));
    }

    @GetMapping("/logs")
    public List<CrawlLogResponse> logs(@RequestHeader(value = TOKEN_HEADER, required = false) String token,
                                       @RequestParam(required = false) Long boardId,
                                       @RequestParam(defaultValue = "20") int size) {
        authorize(token);
        Pageable pageable = PageRequest.of(0, Math.min(Math.max(size, 1), 100));
        List<CrawlLog> logs = boardId == null
                ? crawlLogRepository.findAllByOrderByStartedAtDesc(pageable)
                : crawlLogRepository.findByBoardIdOrderByStartedAtDesc(boardId, pageable);
        return toResponses(logs);
    }

    /** 사이트가 정상적으로 개편되어 파서를 고친 뒤, 새 구조를 기준선으로 다시 학습시킨다. */
    @PostMapping("/boards/{boardId}/reset-baseline")
    public Map<String, Object> resetBaseline(@RequestHeader(value = TOKEN_HEADER, required = false) String token,
                                             @PathVariable Long boardId) {
        authorize(token);
        crawlHealthService.resetBaseline(boardId);
        return Map.of("boardId", boardId, "message", "baseline reset - 이후 기록으로 기준선을 다시 학습합니다.");
    }

    private void authorize(String token) {
        String expected = properties.getAdminToken();
        if (expected == null || expected.isBlank()) {
            throw new ForbiddenException("관리자 API가 비활성화되어 있습니다 (crawler.admin-token 미설정).");
        }
        boolean matches = token != null && MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8), token.getBytes(StandardCharsets.UTF_8));
        if (!matches) {
            throw new ForbiddenException("관리자 토큰이 올바르지 않습니다.");
        }
    }

    private List<CrawlLogResponse> toResponses(List<CrawlLog> logs) {
        return logs.stream().map(CrawlLogResponse::from).toList();
    }
}
