package com.kkamar.cnh.util;

import com.kkamar.cnh.enums.CrawlHealth;
import com.kkamar.cnh.enums.CrawlIssue;
import com.kkamar.cnh.util.CrawlHealthPolicy.Point;
import com.kkamar.cnh.util.CrawlHealthPolicy.Sample;
import com.kkamar.cnh.util.CrawlHealthPolicy.Verdict;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CrawlHealthPolicyTest {

    // 기준선 최소 3개, 추출률 판단 최소 5행, 추출률/페이지크기가 평균의 50% 미만이면 이상
    private final CrawlHealthPolicy policy = new CrawlHealthPolicy(3, 5, 0.5, 0.5);

    private static List<Point> baseline(int count, int rows, int links) {
        List<Point> points = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            points.add(new Point(rows, links));
        }
        return points;
    }

    private static Sample ok(int rows, int links) {
        return new Sample(true, null, rows, links);
    }

    @Test
    void http403And429AreTreatedAsBlocking() {
        for (int status : new int[]{401, 403, 429}) {
            Verdict v = policy.evaluate(new Sample(false, status, 0, 0), baseline(5, 11, 10));
            assertThat(v.status()).isEqualTo(CrawlHealth.DOWN);
            assertThat(v.issue()).isEqualTo(CrawlIssue.BLOCKED);
            assertThat(v.issue().isImmediateAlert()).isTrue();
        }
    }

    @Test
    void serverErrorsAndTimeoutsAreTransientFetchFailures() {
        Verdict http500 = policy.evaluate(new Sample(false, 500, 0, 0), baseline(5, 11, 10));
        Verdict timeout = policy.evaluate(new Sample(false, null, 0, 0), baseline(5, 11, 10));

        for (Verdict v : List.of(http500, timeout)) {
            assertThat(v.status()).isEqualTo(CrawlHealth.DOWN);
            assertThat(v.issue()).isEqualTo(CrawlIssue.FETCH_FAILED);
            assertThat(v.issue().isImmediateAlert()).isFalse(); // 일시적일 수 있어 연속 발생 시에만 알림
        }
    }

    @Test
    void okResponseWithZeroRowsIsEmptyPage() {
        Verdict v = policy.evaluate(ok(0, 0), baseline(5, 11, 10));

        assertThat(v.status()).isEqualTo(CrawlHealth.DOWN);
        assertThat(v.issue()).isEqualTo(CrawlIssue.EMPTY_PAGE);
    }

    @Test
    void whileLearning_normalCyclesAreNotJudged() {
        Verdict v = policy.evaluate(ok(11, 10), baseline(2, 11, 10));

        assertThat(v.status()).isEqualTo(CrawlHealth.LEARNING);
        assertThat(v.issue()).isEqualTo(CrawlIssue.NONE);
    }

    @Test
    void whileLearning_zeroExtractionFromManyRowsIsStillCaught() {
        // 기준선이 없어도 "행은 충분한데 추출이 0건"은 절대적으로 비정상이다 (콜드스타트 안전망)
        Verdict v = policy.evaluate(ok(11, 0), baseline(0, 11, 10));

        assertThat(v.status()).isEqualTo(CrawlHealth.DEGRADED);
        assertThat(v.issue()).isEqualTo(CrawlIssue.PARSE_COLLAPSE);
    }

    @Test
    void whileLearning_tinyPagesAreNotJudgedByTheAbsoluteRule() {
        Verdict v = policy.evaluate(ok(2, 0), baseline(0, 11, 10));

        assertThat(v.status()).isEqualTo(CrawlHealth.LEARNING);
    }

    @Test
    void steadyStateAndNewPostsAreHealthy() {
        assertThat(policy.evaluate(ok(11, 10), baseline(3, 11, 10)).status()).isEqualTo(CrawlHealth.HEALTHY);
        // 새 글이 올라와 행이 늘어도 추출률은 비슷하므로 정상
        assertThat(policy.evaluate(ok(12, 11), baseline(3, 11, 10)).status()).isEqualTo(CrawlHealth.HEALTHY);
    }

    @Test
    void totalAndPartialParseCollapseAreBothDetected() {
        Verdict total = policy.evaluate(ok(11, 0), baseline(3, 11, 10));
        Verdict partial = policy.evaluate(ok(11, 3), baseline(3, 11, 10)); // 0건은 아니지만 급락

        for (Verdict v : List.of(total, partial)) {
            assertThat(v.status()).isEqualTo(CrawlHealth.DEGRADED);
            assertThat(v.issue()).isEqualTo(CrawlIssue.PARSE_COLLAPSE);
            assertThat(v.baselineRate()).isNotNull();
        }
    }

    @Test
    void mildDropAboveThresholdIsTolerated() {
        // 기준 91%의 50% = 45.5%. 6/11 = 54.5%는 허용 범위
        assertThat(policy.evaluate(ok(11, 6), baseline(3, 11, 10)).status()).isEqualTo(CrawlHealth.HEALTHY);
    }

    @Test
    void shrinkingPageIsCaughtEvenWhenRateLooksPerfect() {
        // 에러 페이지가 작은 표를 보여주는 경우: 3/3 = 100% 이지만 평소 11행이었다
        Verdict v = policy.evaluate(ok(3, 3), baseline(3, 11, 10));

        assertThat(v.status()).isEqualTo(CrawlHealth.DEGRADED);
        assertThat(v.issue()).isEqualTo(CrawlIssue.PAGE_SIZE_COLLAPSE);
    }

    @Test
    void invalidConfigurationIsRejected() {
        assertThatThrownBy(() -> new CrawlHealthPolicy(0, 5, 0.5, 0.5)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new CrawlHealthPolicy(3, 0, 0.5, 0.5)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new CrawlHealthPolicy(3, 5, 1.0, 0.5)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new CrawlHealthPolicy(3, 5, 0.5, 0.0)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void parserExceptionIsDownAndAlertsImmediately() {
        Verdict v = policy.evaluate(new Sample(true, null, 0, 0, true, false), baseline(5, 11, 10));

        assertThat(v.status()).isEqualTo(CrawlHealth.DOWN);
        assertThat(v.issue()).isEqualTo(CrawlIssue.PARSE_FAILED);
        assertThat(v.issue().isImmediateAlert()).isTrue();
    }

    @Test
    void ingestFailureOnAHealthySiteIsReportedSeparately() {
        Verdict v = policy.evaluate(new Sample(true, null, 11, 10, false, true), baseline(5, 11, 10));

        assertThat(v.status()).isEqualTo(CrawlHealth.DEGRADED);
        assertThat(v.issue()).isEqualTo(CrawlIssue.INGEST_FAILED);
        assertThat(v.issue().isImmediateAlert()).isFalse(); // 데드락 같은 일시 오류일 수 있어 연속 발생 시에만 알림
    }

    @Test
    void ingestFailureAlsoSurfacesDuringTheLearningPeriod() {
        Verdict v = policy.evaluate(new Sample(true, null, 11, 10, false, true), baseline(1, 11, 10));

        assertThat(v.issue()).isEqualTo(CrawlIssue.INGEST_FAILED);
    }

    @Test
    void ingestFailureDoesNotMaskARealSiteProblem() {
        // 사이트 구조도 깨졌고 DB 저장도 실패했다면, 원인을 가리지 않도록 사이트 쪽 판정을 우선한다.
        Verdict v = policy.evaluate(new Sample(true, null, 11, 0, false, true), baseline(5, 11, 10));

        assertThat(v.issue()).isEqualTo(CrawlIssue.PARSE_COLLAPSE);
    }
}
