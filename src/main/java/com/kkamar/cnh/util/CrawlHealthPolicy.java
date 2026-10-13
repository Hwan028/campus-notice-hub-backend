package com.kkamar.cnh.util;

import com.kkamar.cnh.enums.CrawlHealth;
import com.kkamar.cnh.enums.CrawlIssue;

import java.util.List;
import java.util.Locale;

/**
 * 크롤링 한 사이클의 결과를 "정상 기준선"과 비교해 건강 상태를 판정하는 순수 로직.
 * 프레임워크에 의존하지 않으므로 단위 테스트로 판정 규칙을 그대로 검증할 수 있다.
 *
 * 판정 순서
 *  1) 수집 실패(fetchOk=false)        -> DOWN (401/403/429면 BLOCKED, 그 외 FETCH_FAILED)
 *  2) 파싱 중 예외(parseFailed)       -> DOWN (PARSE_FAILED, 즉시 알림)
 *  3) 행이 0개                        -> DOWN (EMPTY_PAGE)
 *  4) 기준선 표본 부족(관찰 기간)      -> 절대 규칙(행은 충분한데 추출 0건)만 적용, 나머지는 LEARNING
 *  5) 추출률이 기준선 평균의 rateDropFactor 미만 -> DEGRADED (PARSE_COLLAPSE)
 *  6) 행 수가 기준선 평균의 sizeDropFactor 미만  -> DEGRADED (PAGE_SIZE_COLLAPSE)
 *  7) 그 외                           -> HEALTHY
 *
 * 위 사이트 판정이 정상(LEARNING/HEALTHY)인데 DB 저장이 실패했다면(ingestFailed) DEGRADED (INGEST_FAILED).
 * 사이트 자체에 이상이 있으면 그쪽 판정을 우선한다(원인을 가리지 않도록).
 */
public final class CrawlHealthPolicy {

    /** 이번 사이클의 관측값 */
    public record Sample(boolean fetchOk, Integer httpStatus, int rowsFound, int linksExtracted,
                         boolean parseFailed, boolean ingestFailed) {

        /** 수집/파싱/저장이 모두 예외 없이 끝난 일반적인 관측값 */
        public Sample(boolean fetchOk, Integer httpStatus, int rowsFound, int linksExtracted) {
            this(fetchOk, httpStatus, rowsFound, linksExtracted, false, false);
        }
    }

    /** 기준선을 이루는 과거 정상 사이클 하나 */
    public record Point(int rowsFound, int linksExtracted) {
    }

    /** 판정 결과 */
    public record Verdict(CrawlHealth status, CrawlIssue issue,
                          Double baselineRate, Double currentRate, String message) {
    }

    private final int minBaselineSamples;
    private final int minRowsForRate;
    private final double rateDropFactor;
    private final double sizeDropFactor;

    public CrawlHealthPolicy(int minBaselineSamples, int minRowsForRate,
                             double rateDropFactor, double sizeDropFactor) {
        if (minBaselineSamples < 1) {
            throw new IllegalArgumentException("minBaselineSamples must be >= 1");
        }
        if (minRowsForRate < 1) {
            throw new IllegalArgumentException("minRowsForRate must be >= 1");
        }
        if (rateDropFactor <= 0 || rateDropFactor >= 1) {
            throw new IllegalArgumentException("rateDropFactor must be in (0, 1)");
        }
        if (sizeDropFactor <= 0 || sizeDropFactor >= 1) {
            throw new IllegalArgumentException("sizeDropFactor must be in (0, 1)");
        }
        this.minBaselineSamples = minBaselineSamples;
        this.minRowsForRate = minRowsForRate;
        this.rateDropFactor = rateDropFactor;
        this.sizeDropFactor = sizeDropFactor;
    }

    public Verdict evaluate(Sample current, List<Point> baseline) {
        Verdict site = evaluateSite(current, baseline);
        if (current.ingestFailed() && !site.status().isBad()) {
            return new Verdict(CrawlHealth.DEGRADED, CrawlIssue.INGEST_FAILED,
                    site.baselineRate(), site.currentRate(),
                    "사이트 수집은 정상이나 DB 저장에 실패 - DB 상태/제약 위반 확인 필요");
        }
        return site;
    }

    private Verdict evaluateSite(Sample current, List<Point> baseline) {
        // 1) 수집 자체가 실패
        if (!current.fetchOk()) {
            Integer status = current.httpStatus();
            if (status != null && (status == 401 || status == 403 || status == 429)) {
                return new Verdict(CrawlHealth.DOWN, CrawlIssue.BLOCKED, null, null,
                        "HTTP " + status + " 응답 - 크롤러 차단이 의심됨");
            }
            String detail = status != null
                    ? "HTTP " + status + " 응답"
                    : "페이지 수집 실패(타임아웃/연결 오류 등)";
            return new Verdict(CrawlHealth.DOWN, CrawlIssue.FETCH_FAILED, null, null, detail);
        }

        // 2) 페이지는 받았지만 파서가 예외를 던짐 (구조가 파서의 가정과 크게 어긋남)
        if (current.parseFailed()) {
            return new Verdict(CrawlHealth.DOWN, CrawlIssue.PARSE_FAILED, null, null,
                    "페이지는 받았으나 파싱 중 예외 발생 - 사이트 구조 변경 또는 파서 버그 의심");
        }

        // 3) 응답은 받았지만 행이 없음
        if (current.rowsFound() == 0) {
            return new Verdict(CrawlHealth.DOWN, CrawlIssue.EMPTY_PAGE, null, null,
                    "응답은 정상이나 파싱된 행이 0개 - 점검/에러 페이지 또는 구조 변경 의심");
        }

        double currentRate = (double) current.linksExtracted() / current.rowsFound();

        // 3) 기준선이 아직 부족 - 관찰 기간
        if (baseline.size() < minBaselineSamples) {
            if (current.rowsFound() >= minRowsForRate && current.linksExtracted() == 0) {
                return new Verdict(CrawlHealth.DEGRADED, CrawlIssue.PARSE_COLLAPSE, null, currentRate,
                        "기준선 학습 중이지만 행 " + current.rowsFound() + "개에서 링크 추출이 0건 - 파싱 로직 이상 의심");
            }
            return new Verdict(CrawlHealth.LEARNING, CrawlIssue.NONE, null, currentRate,
                    "기준선 학습 중 (" + baseline.size() + "/" + minBaselineSamples + ")");
        }

        double baselineRate = baseline.stream()
                .mapToDouble(p -> p.rowsFound() > 0 ? (double) p.linksExtracted() / p.rowsFound() : 0.0)
                .average()
                .orElse(0.0);
        double baselineRows = baseline.stream()
                .mapToInt(Point::rowsFound)
                .average()
                .orElse(0.0);

        // 4) 추출률 급락 (표본이 너무 작은 페이지는 우연히 튈 수 있어 이 규칙에서 제외)
        if (current.rowsFound() >= minRowsForRate && currentRate < baselineRate * rateDropFactor) {
            return new Verdict(CrawlHealth.DEGRADED, CrawlIssue.PARSE_COLLAPSE, baselineRate, currentRate,
                    String.format(Locale.ROOT, "추출률 %.0f%% (최근 정상 평균 %.0f%%) - 파싱 로직이 구조와 맞지 않을 가능성",
                            currentRate * 100, baselineRate * 100));
        }

        // 5) 페이지 크기 급감
        if (current.rowsFound() < baselineRows * sizeDropFactor) {
            return new Verdict(CrawlHealth.DEGRADED, CrawlIssue.PAGE_SIZE_COLLAPSE, baselineRate, currentRate,
                    String.format(Locale.ROOT, "파싱된 행 %d개 (최근 정상 평균 %.1f개) - 에러 페이지/레이아웃 변경 가능성",
                            current.rowsFound(), baselineRows));
        }

        // 6) 정상
        return new Verdict(CrawlHealth.HEALTHY, CrawlIssue.NONE, baselineRate, currentRate, "정상");
    }
}
