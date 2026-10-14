package com.kkamar.cnh.component;

import com.kkamar.cnh.service.CrawlerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 주기 실행만 담당한다. 크롤링 로직과 스케줄링을 분리해 두었기 때문에
 * 테스트에서는 crawler.scheduler.enabled=false로 스케줄러만 끄고 crawlAllBoards()를 직접 호출할 수 있다.
 * 주기는 crawler.scheduler.interval-ms (기본 1시간).
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "crawler.scheduler", name = "enabled", havingValue = "true", matchIfMissing = true)
public class CrawlScheduler {

    private final CrawlerService crawlerService;

    @Scheduled(
            fixedRateString = "${crawler.scheduler.interval-ms:3600000}",
            initialDelayString = "${crawler.scheduler.initial-delay-ms:5000}")
    public void run() {
        log.info("Scheduled crawl started");
        crawlerService.crawlAllBoards();
    }
}
