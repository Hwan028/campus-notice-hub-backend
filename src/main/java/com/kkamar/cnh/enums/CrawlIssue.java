package com.kkamar.cnh.enums;

/**
 * 이상의 원인 분류. immediateAlert가 true면 첫 발생 즉시 알림을 보내고,
 * false(일시적 오류 가능성이 큰 경우)면 연속 발생 임계치를 넘어야 알림을 보낸다.
 */
public enum CrawlIssue {
    NONE(false),
    /** 401/403/429 응답 - 봇 차단 의심 */
    BLOCKED(true),
    /** 타임아웃/연결 오류/5xx 등 수집 실패 (일시적일 수 있음) */
    FETCH_FAILED(false),
    /** 응답은 정상(200)인데 파싱된 행이 0개 */
    EMPTY_PAGE(true),
    /** 행은 있는데 링크 추출률이 기준선 대비 급락 - 셀렉터/구조 변경 의심 */
    PARSE_COLLAPSE(true),
    /** 파싱된 행 수 자체가 기준선 대비 급감 - 에러 페이지/레이아웃 변경 의심 */
    PAGE_SIZE_COLLAPSE(true),
    /** 페이지는 받았지만 파서가 예외를 던짐 - 매번 똑같이 재현되므로 즉시 알림 */
    PARSE_FAILED(true),
    /** 사이트는 정상인데 우리 DB 저장이 실패 - 일시적 오류(데드락 등)일 수 있어 연속 발생 시 알림 */
    INGEST_FAILED(false);

    private final boolean immediateAlert;

    CrawlIssue(boolean immediateAlert) {
        this.immediateAlert = immediateAlert;
    }

    public boolean isImmediateAlert() {
        return immediateAlert;
    }
}
