package com.kkamar.cnh.service.impl;

import com.kkamar.cnh.domain.Board;
import com.kkamar.cnh.domain.CrawlLog;
import com.kkamar.cnh.domain.Notice;
import com.kkamar.cnh.dto.CrawlAlert;
import com.kkamar.cnh.enums.AlertType;
import com.kkamar.cnh.enums.CrawlHealth;
import com.kkamar.cnh.enums.CrawlIssue;
import com.kkamar.cnh.exception.PageFetchException;
import com.kkamar.cnh.repository.BoardRepository;
import com.kkamar.cnh.repository.CrawlLogRepository;
import com.kkamar.cnh.repository.NoticeRepository;
import com.kkamar.cnh.service.AlertNotifier;
import com.kkamar.cnh.service.CrawlHealthService;
import com.kkamar.cnh.service.CrawlHealthService.Assessment;
import com.kkamar.cnh.service.CrawlerService;
import com.kkamar.cnh.service.PageFetcher;
import com.kkamar.cnh.util.BoardHtmlParser;
import com.kkamar.cnh.util.BoardHtmlParser.ParseResult;
import com.kkamar.cnh.util.BoardHtmlParser.ParsedNotice;
import com.kkamar.cnh.util.CrawlHealthPolicy.Sample;
import com.kkamar.cnh.util.CrawlHealthPolicy.Verdict;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 한 사이클의 흐름 (게시판마다 독립적으로 수행):
 *   수집(fetch) -> 파싱(parse) -> 신규 저장(ingest, 게시판 단위 트랜잭션)
 *   -> 건강 판정(assess) -> 알림 발송(deliver) -> crawl_log 기록
 *
 * 예전에는 crawlAllBoards() 전체가 하나의 @Transactional이었다. 네트워크 대기(타임아웃 5초 x 게시판 수)
 * 동안 DB 커넥션을 쥐고 있고, 한 게시판의 DB 오류가 나머지 게시판의 저장까지 롤백시킬 수 있었기 때문에
 * 트랜잭션 경계를 "게시판 1개의 저장 구간"으로 좁혔다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CrawlerServiceImpl implements CrawlerService {

    private static final int MAX_TITLE_LENGTH = 500;
    private static final int MAX_URL_LENGTH = 500;
    private static final int MAX_EXTERNAL_ID_LENGTH = 255;
    private static final int MAX_ERROR_LENGTH = 500;

    private final BoardRepository boardRepository;
    private final NoticeRepository noticeRepository;
    private final CrawlLogRepository crawlLogRepository;
    private final PageFetcher pageFetcher;
    private final CrawlHealthService crawlHealthService;
    private final AlertNotifier alertNotifier;
    private final PlatformTransactionManager transactionManager;

    /** 스케줄러와 관리자 수동 실행이 겹쳐 같은 게시판을 동시에 크롤링하는 것을 막는다. */
    private final ReentrantLock runLock = new ReentrantLock();

    @Override
    public void crawlAllBoards() {
        if (!runLock.tryLock()) {
            log.warn("Crawl is already running, skipping this trigger");
            return;
        }
        try {
            List<Board> boards = boardRepository.findAllActiveWithAssociations();
            log.info("Found {} active board(s) to crawl", boards.size());

            for (Board board : boards) {
                try {
                    crawlBoard(board);
                } catch (RuntimeException e) {
                    // 한 게시판의 예기치 못한 오류가 다른 게시판 크롤링을 막지 않도록 격리
                    log.error("Unexpected failure while crawling board={}", board.getName(), e);
                }
            }
        } finally {
            runLock.unlock();
        }
    }

    private void crawlBoard(Board board) {
        LocalDateTime startedAt = LocalDateTime.now();
        long beginNanos = System.nanoTime();
        log.info("Processing board: id={}, name='{}', departmentId={}",
                board.getId(), board.getName(),
                board.getDepartment() == null ? "null" : board.getDepartment().getId());

        boolean fetchOk = false;
        boolean parseFailed = false;
        boolean ingestOk = true;
        Integer httpStatus = null;
        String errorMessage = null;
        int rowsFound = 0;
        int linksExtracted = 0;
        int newlySaved = 0;

        try {
            Document doc = pageFetcher.fetch(board.getCrawlUrl());
            fetchOk = true;

            ParseResult parsed = null;
            try {
                parsed = parse(board, doc);
            } catch (RuntimeException e) {
                // 페이지는 받았지만 파서가 예외를 던짐: 접속 실패(일시적일 수 있음)와 구분해 즉시 알림 대상으로 분류
                parseFailed = true;
                errorMessage = truncate("파싱 실패: " + e.getClass().getSimpleName() + ": " + e.getMessage(), MAX_ERROR_LENGTH);
                log.error("Parse failed: board={}", board.getName(), e);
            }

            if (parsed != null) {
                rowsFound = parsed.rowsFound();
                linksExtracted = parsed.linksExtracted();
                try {
                    newlySaved = ingest(board, parsed.notices());
                } catch (RuntimeException e) {
                    // 사이트는 정상인데 우리 DB 문제인 경우: 사이트 판정과 분리해 INGEST_FAILED로 따로 알린다.
                    ingestOk = false;
                    errorMessage = truncate("DB 저장 실패: " + e.getMessage(), MAX_ERROR_LENGTH);
                    log.error("Ingest failed: board={}", board.getName(), e);
                }
            }
        } catch (PageFetchException e) {
            httpStatus = e.getStatusCode();
            errorMessage = truncate(e.getMessage(), MAX_ERROR_LENGTH);
            log.warn("Fetch failed: board={}, status={}, message={}", board.getName(), httpStatus, e.getMessage());
        } catch (RuntimeException e) {
            // 수집기가 던진 예상 밖 예외(잘못된 URL 등)는 수집 실패로 취급
            errorMessage = truncate("수집 실패: " + e.getClass().getSimpleName() + ": " + e.getMessage(), MAX_ERROR_LENGTH);
            log.error("Unexpected fetch error: board={}", board.getName(), e);
        }

        long durationMs = (System.nanoTime() - beginNanos) / 1_000_000L;
        Sample sample = new Sample(fetchOk, httpStatus, rowsFound, linksExtracted, parseFailed, !ingestOk);

        Assessment assessment = assessSafely(board, sample);

        AlertType deliveredAlert = AlertType.NONE;
        if (assessment.alertType() != AlertType.NONE && deliver(board, sample, assessment)) {
            deliveredAlert = assessment.alertType();
        }

        Verdict verdict = assessment.verdict();
        Double extractionRate = rowsFound > 0 ? (double) linksExtracted / rowsFound : null;

        crawlLogRepository.save(CrawlLog.builder()
                .board(board)
                .startedAt(startedAt)
                .durationMs(durationMs)
                .success(fetchOk && !parseFailed && ingestOk)
                .httpStatus(httpStatus)
                .rowsFound(rowsFound)
                .linksExtracted(linksExtracted)
                .newlySavedCount(newlySaved)
                .extractionRate(extractionRate)
                .healthStatus(verdict.status())
                .issue(verdict.issue())
                .alertType(deliveredAlert)
                .errorMessage(errorMessage)
                .build());

        log.info("[{}] rows found: {}, links extracted: {}, newly saved: {}, health={}/{}, alert={}",
                board.getName(), rowsFound, linksExtracted, newlySaved,
                verdict.status(), verdict.issue(), deliveredAlert);
    }

    private ParseResult parse(Board board, Document doc) {
        if (board.getDepartment() == null) {
            return BoardHtmlParser.parseUniversityBoard(doc, board.getUniversity().getPortalBaseUrl());
        }
        return BoardHtmlParser.parseDepartmentBoard(doc);
    }

    /** 게시판 1개 분량의 신규 공지를 하나의 트랜잭션으로 저장하고, 새로 저장된 건수를 돌려준다. */
    private int ingest(Board board, List<ParsedNotice> notices) {
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        Integer saved = tx.execute(status -> {
            Board boardRef = boardRepository.getReferenceById(board.getId());
            int count = 0;
            for (ParsedNotice parsed : notices) {
                if (parsed.externalId().length() > MAX_EXTERNAL_ID_LENGTH
                        || parsed.detailUrl().length() > MAX_URL_LENGTH) {
                    log.warn("Skipping notice with oversized id/url: board={}, title='{}'", board.getName(), parsed.title());
                    continue;
                }
                if (noticeRepository.existsByBoardIdAndExternalId(board.getId(), parsed.externalId())) {
                    log.debug("Notice already exists, skipping: {}", parsed.title());
                    continue;
                }
                noticeRepository.save(Notice.builder()
                        .board(boardRef)
                        .externalId(parsed.externalId())
                        .title(truncate(parsed.title(), MAX_TITLE_LENGTH))
                        .originalUrl(parsed.detailUrl())
                        .postedAt(parsed.postedAt())
                        .build());
                count++;
                log.info("Saved new notice: {}", parsed.title());
            }
            return count;
        });
        return saved == null ? 0 : saved;
    }

    /** 진단 로직의 오류가 크롤링 결과 기록 자체를 막지 않도록 보호한다. */
    private Assessment assessSafely(Board board, Sample sample) {
        try {
            return crawlHealthService.assess(board, sample);
        } catch (RuntimeException e) {
            log.error("Health assessment failed, falling back to neutral verdict: board={}", board.getName(), e);
            Verdict fallback = sample.fetchOk()
                    ? new Verdict(CrawlHealth.LEARNING, CrawlIssue.NONE, null, null, "진단 실패 - 판정 보류")
                    : new Verdict(CrawlHealth.DOWN, CrawlIssue.FETCH_FAILED, null, null, "수집 실패 (진단 실패로 상세 판정 불가)");
            return new Assessment(fallback, AlertType.NONE, 0, null);
        }
    }

    private boolean deliver(Board board, Sample sample, Assessment assessment) {
        Verdict v = assessment.verdict();
        CrawlAlert alert = new CrawlAlert(
                assessment.alertType(), board.getName(), v.status(), v.issue(), v.message(),
                v.baselineRate(), v.currentRate(), sample.rowsFound(), sample.linksExtracted(),
                assessment.consecutiveBad(), assessment.lastSuccessAt(), LocalDateTime.now());
        try {
            return alertNotifier.send(alert);
        } catch (RuntimeException e) {
            log.error("Alert delivery threw: board={}", board.getName(), e);
            return false;
        }
    }

    private static String truncate(String value, int max) {
        if (value == null) return null;
        return value.length() <= max ? value : value.substring(0, max);
    }
}
