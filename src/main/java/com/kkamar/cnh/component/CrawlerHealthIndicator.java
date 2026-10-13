package com.kkamar.cnh.component;

import com.kkamar.cnh.domain.Board;
import com.kkamar.cnh.domain.CrawlLog;
import com.kkamar.cnh.enums.CrawlHealth;
import com.kkamar.cnh.repository.BoardRepository;
import com.kkamar.cnh.repository.CrawlLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.actuate.health.Status;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * /actuator/health 의 "crawler" 항목.
 *  - 어떤 게시판이든 최신 사이클이 DOWN이거나, 마지막 크롤링이 오래됐으면(스케줄러 정지) -> DOWN (HTTP 503)
 *  - DEGRADED가 있으면 -> DEGRADED (HTTP 200, 상세 정보에 표시)
 *
 * "스케줄러 자체가 죽은" 경우에는 스스로 Discord 알림을 보낼 수 없다.
 * 그 경우를 잡으려면 외부(UptimeRobot 등)에서 이 엔드포인트를 주기적으로 호출해 두어야 한다.
 */
@Component
@RequiredArgsConstructor
public class CrawlerHealthIndicator implements HealthIndicator {

    public static final Status DEGRADED = new Status("DEGRADED");

    private final BoardRepository boardRepository;
    private final CrawlLogRepository crawlLogRepository;
    private final CrawlerProperties properties;

    @Override
    public Health health() {
        List<Board> boards = boardRepository.findAllActiveWithAssociations();
        if (boards.isEmpty()) {
            return Health.unknown().withDetail("reason", "활성 게시판이 없습니다.").build();
        }

        LocalDateTime staleLimit = LocalDateTime.now().minusMinutes(properties.getHealth().getStaleAfterMinutes());
        Map<String, Object> details = new LinkedHashMap<>();
        boolean down = false;
        boolean degraded = false;
        boolean noData = false;

        for (Board board : boards) {
            String key = board.getName() + "#" + board.getId();
            Optional<CrawlLog> latest = crawlLogRepository.findFirstByBoardIdOrderByStartedAtDesc(board.getId());

            if (latest.isEmpty()) {
                details.put(key, "NO_DATA");
                noData = true;
                continue;
            }

            CrawlLog crawlLog = latest.get();
            Map<String, Object> entry = new LinkedHashMap<>();
            String state;
            if (crawlLog.getStartedAt().isBefore(staleLimit)) {
                state = "STALE";
                down = true;
            } else if (crawlLog.getHealthStatus() == CrawlHealth.DOWN) {
                state = "DOWN";
                down = true;
            } else if (crawlLog.getHealthStatus() == CrawlHealth.DEGRADED) {
                state = "DEGRADED";
                degraded = true;
            } else {
                state = crawlLog.getHealthStatus().name();
            }
            entry.put("state", state);
            entry.put("issue", crawlLog.getIssue().name());
            entry.put("lastCrawledAt", crawlLog.getStartedAt().toString());
            entry.put("rowsFound", crawlLog.getRowsFound());
            entry.put("linksExtracted", crawlLog.getLinksExtracted());
            entry.put("extractionRate", crawlLog.getExtractionRate());
            details.put(key, entry);
        }

        Health.Builder builder;
        if (down) {
            builder = Health.down();
        } else if (degraded) {
            builder = Health.status(DEGRADED);
        } else if (noData) {
            builder = Health.unknown();
        } else {
            builder = Health.up();
        }
        return builder.withDetails(details).build();
    }
}
