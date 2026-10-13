package com.kkamar.cnh.service;

import com.kkamar.cnh.component.CrawlerProperties;
import com.kkamar.cnh.domain.Board;
import com.kkamar.cnh.domain.CrawlLog;
import com.kkamar.cnh.enums.AlertType;
import com.kkamar.cnh.enums.CrawlHealth;
import com.kkamar.cnh.exception.ResourceNotFoundException;
import com.kkamar.cnh.repository.BoardRepository;
import com.kkamar.cnh.repository.CrawlLogRepository;
import com.kkamar.cnh.util.CrawlHealthPolicy;
import com.kkamar.cnh.util.CrawlHealthPolicy.Point;
import com.kkamar.cnh.util.CrawlHealthPolicy.Sample;
import com.kkamar.cnh.util.CrawlHealthPolicy.Verdict;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 크롤러 자가진단.
 * (1) 게시판별 "정상 기준선"을 과거 정상 사이클에서 만들고
 * (2) 이번 사이클을 CrawlHealthPolicy로 판정하고
 * (3) 알림을 보낼지(첫 알림/재알림/복구)를 이력으로부터 결정한다.
 * 알림 상태를 메모리가 아니라 crawl_log에 남기므로 서버가 재시작해도 중복/누락이 생기지 않는다.
 */
@Slf4j
@Service
public class CrawlHealthService {

    private static final LocalDateTime EARLIEST = LocalDateTime.of(2000, 1, 1, 0, 0);
    private static final List<CrawlHealth> NORMAL_STATUSES = List.of(CrawlHealth.LEARNING, CrawlHealth.HEALTHY);

    private final CrawlLogRepository crawlLogRepository;
    private final BoardRepository boardRepository;
    private final CrawlerProperties.Health config;
    private final CrawlHealthPolicy policy;

    public CrawlHealthService(CrawlLogRepository crawlLogRepository,
                              BoardRepository boardRepository,
                              CrawlerProperties properties) {
        this.crawlLogRepository = crawlLogRepository;
        this.boardRepository = boardRepository;
        this.config = properties.getHealth();
        this.policy = new CrawlHealthPolicy(
                config.getMinBaselineSamples(),
                config.getMinRowsForRate(),
                config.getRateDropFactor(),
                config.getSizeDropFactor());
    }

    /** 판정 결과 + 이번 사이클에 보내야 할 알림 종류 */
    public record Assessment(Verdict verdict, AlertType alertType, int consecutiveBad, LocalDateTime lastSuccessAt) {
    }

    @Transactional(readOnly = true)
    public Assessment assess(Board board, Sample sample) {
        Long boardId = board.getId();

        // 1) 기준선: 정상 판정을 받았던 최근 N개 사이클 (기준선 리셋 시각 이후만)
        LocalDateTime since = board.getBaselineSince() != null ? board.getBaselineSince() : EARLIEST;
        List<Point> baseline = crawlLogRepository
                .findBaselineCandidates(boardId, config.getMinRowsForRate(), NORMAL_STATUSES, since,
                        PageRequest.of(0, config.getBaselineWindow()))
                .stream()
                .map(c -> new Point(c.getRowsFound(), c.getLinksExtracted()))
                .toList();

        // 2) 이번 사이클 판정
        Verdict verdict = policy.evaluate(sample, baseline);

        // 3) 이상 지속 구간(streak)과 마지막 알림 시점을 이력에서 계산
        int streakFetch = config.getReminderEveryCycles() * 3 + 10;
        List<CrawlLog> recent = crawlLogRepository
                .findByBoardIdOrderByStartedAtDesc(boardId, PageRequest.of(0, streakFetch));

        int priorBad = 0;
        int sinceAlert = -1; // 마지막 firing 알림 이후 몇 사이클이 지났는지 (직전 사이클이면 1)
        for (int i = 0; i < recent.size(); i++) {
            CrawlLog previous = recent.get(i);
            if (!previous.getHealthStatus().isBad()) {
                break;
            }
            priorBad++;
            if (sinceAlert < 0 && previous.getAlertType().isFiring()) {
                sinceAlert = i + 1;
            }
        }

        // 4) 알림 종류 결정
        AlertType alertType = AlertType.NONE;
        int consecutiveBad = 0;
        if (verdict.status().isBad()) {
            consecutiveBad = priorBad + 1;
            int threshold = verdict.issue().isImmediateAlert() ? 1 : config.getTransientFailureThreshold();
            if (sinceAlert < 0) {
                if (consecutiveBad >= threshold) {
                    alertType = AlertType.INCIDENT;
                }
            } else if (sinceAlert >= config.getReminderEveryCycles()) {
                alertType = AlertType.REMINDER;
            }
        } else if (priorBad > 0 && sinceAlert >= 0) {
            alertType = AlertType.RECOVERED;
        }

        LocalDateTime lastSuccessAt = crawlLogRepository
                .findFirstByBoardIdAndSuccessTrueOrderByStartedAtDesc(boardId)
                .map(CrawlLog::getStartedAt)
                .orElse(null);

        return new Assessment(verdict, alertType, consecutiveBad, lastSuccessAt);
    }

    /**
     * 학교가 사이트를 정상적으로 개편해 "새로운 정상"이 생겼을 때(파서를 고친 뒤)
     * 이 시각 이후의 기록으로만 기준선을 다시 만들도록 한다.
     */
    @Transactional
    public void resetBaseline(Long boardId) {
        Board board = boardRepository.findById(boardId)
                .orElseThrow(() -> new ResourceNotFoundException("Board not found: id=" + boardId));
        board.resetBaseline(LocalDateTime.now());
        log.info("Crawl health baseline reset: boardId={}, name={}", boardId, board.getName());
    }
}
