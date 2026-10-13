package com.kkamar.cnh.enums;

/**
 * 크롤링 한 사이클에 대한 건강 판정 결과.
 * LEARNING : 기준선을 쌓는 관찰 기간 (알림 보류)
 * HEALTHY  : 기준선 대비 정상
 * DEGRADED : 페이지는 열리지만 추출이 기준선 대비 급락 (구조 변경/부분 파손 의심)
 * DOWN     : 페이지 수집 자체가 실패하거나 빈 응답
 */
public enum CrawlHealth {
    LEARNING,
    HEALTHY,
    DEGRADED,
    DOWN;

    public boolean isBad() {
        return this == DEGRADED || this == DOWN;
    }
}
