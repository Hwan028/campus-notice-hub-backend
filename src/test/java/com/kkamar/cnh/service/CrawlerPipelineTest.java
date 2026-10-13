package com.kkamar.cnh.service;

import com.kkamar.cnh.component.CrawlerHealthIndicator;
import com.kkamar.cnh.domain.Board;
import com.kkamar.cnh.domain.CrawlLog;
import com.kkamar.cnh.domain.Department;
import com.kkamar.cnh.domain.University;
import com.kkamar.cnh.enums.AlertType;
import com.kkamar.cnh.enums.CrawlHealth;
import com.kkamar.cnh.enums.CrawlIssue;
import com.kkamar.cnh.support.IntegrationTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.Status;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * 수집 -> 파싱 -> 저장 -> 진단 -> 알림 -> crawl_log 기록 전체 흐름을 네트워크 없이 검증한다.
 * 기준선 최소 표본은 테스트 설정에서 3으로 줄여 두었다 (3사이클 학습 후 4번째부터 HEALTHY 판정 가능).
 */
class CrawlerPipelineTest extends IntegrationTestBase {

    private static final String UNIV_URL = "https://portal.test/bbs/all";
    private static final String DEPT_URL = "https://dept.test/board";

    @Autowired private CrawlerService crawlerService;
    @Autowired private CrawlHealthService crawlHealthService;
    @Autowired private CrawlerHealthIndicator crawlerHealthIndicator;
    @Autowired private JdbcTemplate jdbcTemplate;

    private University university;
    private Board univBoard;

    @BeforeEach
    void setUp() {
        university = saveUniversity("테스트대학교", "https://portal.test");
        univBoard = saveBoard(university, null, "전체공지", UNIV_URL);
        fakePageFetcher.setPage(UNIV_URL, fixture("university_board.html"));
    }

    private CrawlLog latestLog(Board board) {
        return crawlLogRepository.findByBoardIdOrderByStartedAtDesc(board.getId(), PageRequest.of(0, 1)).get(0);
    }

    private CrawlLog latestLog() {
        return latestLog(univBoard);
    }

    private void crawlTimes(int times) {
        for (int i = 0; i < times; i++) {
            crawlerService.crawlAllBoards();
        }
    }

    @Test
    void linksExtractedStaysConstant_whileNewlySavedCountDropsToZero() {
        crawlerService.crawlAllBoards();
        CrawlLog first = latestLog();

        assertThat(first.getRowsFound()).isEqualTo(11);
        assertThat(first.getLinksExtracted()).isEqualTo(10);
        assertThat(first.getNewlySavedCount()).isEqualTo(10);
        assertThat(first.getExtractionRate()).isCloseTo(10 / 11.0, within(1e-9));
        assertThat(first.getHealthStatus()).isEqualTo(CrawlHealth.LEARNING);
        assertThat(first.isSuccess()).isTrue();
        assertThat(noticeRepository.count()).isEqualTo(10);

        crawlerService.crawlAllBoards();
        CrawlLog second = latestLog();

        // 이미 저장된 글이라도 파싱에는 성공하므로 links_extracted는 그대로, 신규 저장 건수만 0이 된다.
        assertThat(second.getLinksExtracted()).isEqualTo(10);
        assertThat(second.getNewlySavedCount()).isZero();
        assertThat(noticeRepository.count()).isEqualTo(10);
    }

    @Test
    void siteRedesign_isDetectedOnFirstBadCycle_notifiedOnce_andRecoveryIsAnnounced() {
        crawlTimes(4); // 학습 3사이클 + 정상 판정 1사이클
        assertThat(latestLog().getHealthStatus()).isEqualTo(CrawlHealth.HEALTHY);
        assertThat(alertNotifier.sent()).isEmpty();

        // 학교가 사이트를 개편: 같은 표 구조지만 셀렉터가 더 이상 맞지 않는다 (예외 없이 조용히 0건)
        fakePageFetcher.setPage(UNIV_URL, fixture("university_board_redesigned.html"));
        crawlerService.crawlAllBoards();

        CrawlLog broken = latestLog();
        assertThat(broken.getRowsFound()).isEqualTo(11);
        assertThat(broken.getLinksExtracted()).isZero();
        assertThat(broken.getNewlySavedCount()).isZero();
        assertThat(broken.isSuccess()).isTrue(); // 예외가 없었으므로 "성공"으로 기록된다 -> 그래서 별도 판정이 필요하다
        assertThat(broken.getHealthStatus()).isEqualTo(CrawlHealth.DEGRADED);
        assertThat(broken.getIssue()).isEqualTo(CrawlIssue.PARSE_COLLAPSE);
        assertThat(broken.getAlertType()).isEqualTo(AlertType.INCIDENT);
        assertThat(alertNotifier.sent()).hasSize(1);
        assertThat(alertNotifier.sent().get(0).boardName()).isEqualTo("전체공지");
        assertThat(crawlerHealthIndicator.health().getStatus()).isEqualTo(CrawlerHealthIndicator.DEGRADED);

        // 고장이 계속돼도 같은 알림을 매 사이클 반복하지 않는다
        crawlerService.crawlAllBoards();
        assertThat(latestLog().getHealthStatus()).isEqualTo(CrawlHealth.DEGRADED);
        assertThat(latestLog().getAlertType()).isEqualTo(AlertType.NONE);
        assertThat(alertNotifier.sent()).hasSize(1);

        // 파서를 고쳐(=원래 구조로 복구) 정상으로 돌아오면 복구 알림 1회
        fakePageFetcher.setPage(UNIV_URL, fixture("university_board.html"));
        crawlerService.crawlAllBoards();
        assertThat(latestLog().getHealthStatus()).isEqualTo(CrawlHealth.HEALTHY);
        assertThat(latestLog().getAlertType()).isEqualTo(AlertType.RECOVERED);
        assertThat(alertNotifier.sent()).hasSize(2);
        assertThat(alertNotifier.sent().get(1).type()).isEqualTo(AlertType.RECOVERED);
        assertThat(crawlerHealthIndicator.health().getStatus()).isEqualTo(Status.UP);
    }

    @Test
    void brokenCyclesDoNotPolluteTheBaseline() {
        crawlTimes(4);
        fakePageFetcher.setPage(UNIV_URL, fixture("university_board_redesigned.html"));
        crawlTimes(10); // 고장이 길게 이어져도

        // 기준선은 정상 판정을 받은 사이클로만 만들어지므로 계속 DEGRADED로 판정된다 (고장 상태에 적응하지 않는다)
        assertThat(latestLog().getHealthStatus()).isEqualTo(CrawlHealth.DEGRADED);
    }

    @Test
    void http403IsReportedAsBlockedImmediately() {
        fakePageFetcher.failWith(UNIV_URL, 403);
        crawlerService.crawlAllBoards();

        CrawlLog log = latestLog();
        assertThat(log.isSuccess()).isFalse();
        assertThat(log.getHttpStatus()).isEqualTo(403);
        assertThat(log.getRowsFound()).isZero();
        assertThat(log.getHealthStatus()).isEqualTo(CrawlHealth.DOWN);
        assertThat(log.getIssue()).isEqualTo(CrawlIssue.BLOCKED);
        assertThat(log.getAlertType()).isEqualTo(AlertType.INCIDENT);
        assertThat(alertNotifier.sent()).hasSize(1);
        assertThat(crawlerHealthIndicator.health().getStatus()).isEqualTo(Status.DOWN);
    }

    @Test
    void transientFailureNeedsTwoConsecutiveCyclesBeforeAlerting() {
        fakePageFetcher.failWith(UNIV_URL, null); // 타임아웃 같은 네트워크 오류

        crawlerService.crawlAllBoards();
        assertThat(latestLog().getHealthStatus()).isEqualTo(CrawlHealth.DOWN);
        assertThat(latestLog().getIssue()).isEqualTo(CrawlIssue.FETCH_FAILED);
        assertThat(latestLog().getAlertType()).isEqualTo(AlertType.NONE);
        assertThat(alertNotifier.sent()).isEmpty();

        crawlerService.crawlAllBoards();
        assertThat(latestLog().getAlertType()).isEqualTo(AlertType.INCIDENT);
        assertThat(alertNotifier.sent()).hasSize(1);
    }

    @Test
    void undeliveredAlertIsRetriedOnTheNextCycle() {
        alertNotifier.setDeliverable(false); // Discord 장애 상황
        fakePageFetcher.failWith(UNIV_URL, 403);

        crawlerService.crawlAllBoards();
        assertThat(latestLog().getAlertType()).isEqualTo(AlertType.NONE); // 전달 실패 -> 알림했다고 기록하지 않는다
        assertThat(alertNotifier.sent()).isEmpty();

        alertNotifier.setDeliverable(true);
        crawlerService.crawlAllBoards();
        assertThat(latestLog().getAlertType()).isEqualTo(AlertType.INCIDENT);
        assertThat(alertNotifier.sent()).hasSize(1);
    }

    @Test
    void zeroExtractionIsCaughtEvenDuringTheLearningPeriod() {
        fakePageFetcher.setPage(UNIV_URL, fixture("university_board_redesigned.html"));
        crawlerService.crawlAllBoards();

        assertThat(latestLog().getHealthStatus()).isEqualTo(CrawlHealth.DEGRADED);
        assertThat(alertNotifier.sent()).hasSize(1);
    }

    @Test
    void departmentBoardIsParsedWithItsOwnStrategy() {
        Department dept = saveDepartment(university, "컴퓨터정보과");
        Board deptBoard = saveBoard(university, dept, "컴퓨터정보과공지", DEPT_URL);
        fakePageFetcher.setPage(DEPT_URL, fixture("dept_board.html"));

        crawlerService.crawlAllBoards();

        CrawlLog log = latestLog(deptBoard);
        assertThat(log.getRowsFound()).isEqualTo(11);
        assertThat(log.getLinksExtracted()).isEqualTo(10);
        assertThat(log.getNewlySavedCount()).isEqualTo(10);
        assertThat(noticeRepository.existsByBoardIdAndExternalId(deptBoard.getId(), "55510")).isTrue();
    }

    @Test
    void inactiveBoardsAreNotCrawled() {
        // 예전 크롤러는 findAll()이라 is_active 플래그를 무시하고 비활성 게시판도 크롤링했다.
        jdbcTemplate.update("update board set is_active = false where id = ?", univBoard.getId());

        crawlerService.crawlAllBoards();

        assertThat(crawlLogRepository.count()).isZero();
        assertThat(noticeRepository.count()).isZero();
    }

    @Test
    void resettingTheBaselineRestartsTheLearningPeriod() {
        crawlTimes(4);
        assertThat(latestLog().getHealthStatus()).isEqualTo(CrawlHealth.HEALTHY);

        crawlHealthService.resetBaseline(univBoard.getId());
        crawlerService.crawlAllBoards();

        assertThat(latestLog().getHealthStatus()).isEqualTo(CrawlHealth.LEARNING);
    }

    @Test
    void parserExceptionIsAlertedImmediately_notTreatedAsTransientFetchFailure() {
        fakePageFetcher.returnNullDocument(UNIV_URL); // 파서가 NPE를 던지는 상황

        crawlerService.crawlAllBoards();

        CrawlLog log = latestLog();
        assertThat(log.isSuccess()).isFalse();
        assertThat(log.getHealthStatus()).isEqualTo(CrawlHealth.DOWN);
        assertThat(log.getIssue()).isEqualTo(CrawlIssue.PARSE_FAILED);
        assertThat(log.getAlertType()).isEqualTo(AlertType.INCIDENT); // 접속 실패처럼 2사이클을 기다리지 않는다
        assertThat(log.getErrorMessage()).contains("파싱 실패");
        assertThat(alertNotifier.sent()).hasSize(1);
    }

    @Test
    void dbFailureOnAHealthySiteIsRecordedAndAlertedAfterTwoCycles() {
        // 저장 단계만 실패시키기 위해 notice 테이블 이름을 잠시 바꾼다 (crawl_log 기록은 정상 동작)
        jdbcTemplate.execute("alter table notice rename to notice_bak");
        try {
            crawlerService.crawlAllBoards();
            CrawlLog first = latestLog();
            assertThat(first.isSuccess()).isFalse();
            assertThat(first.getRowsFound()).isEqualTo(11);
            assertThat(first.getLinksExtracted()).isEqualTo(10); // 사이트 파싱은 정상이었다는 사실이 남는다
            assertThat(first.getNewlySavedCount()).isZero();
            assertThat(first.getIssue()).isEqualTo(CrawlIssue.INGEST_FAILED);
            assertThat(first.getHealthStatus()).isEqualTo(CrawlHealth.DEGRADED);
            assertThat(first.getAlertType()).isEqualTo(AlertType.NONE);
            assertThat(first.getErrorMessage()).contains("DB 저장 실패");
            assertThat(alertNotifier.sent()).isEmpty();

            crawlerService.crawlAllBoards();
            assertThat(latestLog().getAlertType()).isEqualTo(AlertType.INCIDENT);
            assertThat(alertNotifier.sent()).hasSize(1);
        } finally {
            jdbcTemplate.execute("alter table notice_bak rename to notice");
        }
    }
}
