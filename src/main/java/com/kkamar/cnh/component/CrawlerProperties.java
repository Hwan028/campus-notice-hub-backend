package com.kkamar.cnh.component;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * application.yml의 crawler.* 설정을 한곳에 모은 클래스.
 * 임계값/주기/웹훅 주소를 코드에서 분리해 두어야, 값을 바꿀 때 재빌드 없이 설정만 고치면 된다.
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "crawler")
public class CrawlerProperties {

    /** /api/admin/crawl/** 호출용 토큰. 비어 있으면 관리자 API 전체가 비활성화된다. */
    private String adminToken = "";

    private Scheduler scheduler = new Scheduler();
    private Fetch fetch = new Fetch();
    private Health health = new Health();
    private Alert alert = new Alert();

    @Getter
    @Setter
    public static class Scheduler {
        private boolean enabled = true;
        private long intervalMs = 3_600_000L;
        private long initialDelayMs = 5_000L;
    }

    @Getter
    @Setter
    public static class Fetch {
        private String userAgent = "Mozilla/5.0";
        private int timeoutMs = 5_000;
    }

    @Getter
    @Setter
    public static class Health {
        /** 기준선 계산에 쓰는 최근 정상 사이클 수 */
        private int baselineWindow = 24;
        /** 이 개수 이상의 정상 표본이 쌓이기 전까지는 관찰 기간(LEARNING) */
        private int minBaselineSamples = 12;
        /** 추출률 판단/기준선 편입에 필요한 최소 행 수 (표본이 너무 작으면 우연히 튈 수 있음) */
        private int minRowsForRate = 5;
        /** 현재 추출률이 기준선 평균의 이 비율 미만이면 급락 */
        private double rateDropFactor = 0.5;
        /** 현재 행 수가 기준선 평균의 이 비율 미만이면 페이지 크기 급감 */
        private double sizeDropFactor = 0.5;
        /** 일시적 오류(타임아웃 등)는 이 횟수만큼 연속될 때 알림 */
        private int transientFailureThreshold = 2;
        /** 이상이 지속될 때 재알림 주기(사이클 수) */
        private int reminderEveryCycles = 24;
        /** 마지막 크롤링이 이 시간(분)보다 오래되면 스케줄러 정지로 간주 */
        private int staleAfterMinutes = 180;
    }

    @Getter
    @Setter
    public static class Alert {
        /** 비어 있으면 알림은 로그로만 남는다. */
        private String discordWebhookUrl = "";
    }
}
