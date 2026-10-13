package com.kkamar.cnh.domain;

import com.kkamar.cnh.component.BaseTimeEntity;
import com.kkamar.cnh.enums.AlertType;
import com.kkamar.cnh.enums.CrawlHealth;
import com.kkamar.cnh.enums.CrawlIssue;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 게시판 1개 x 크롤링 1사이클의 결과 기록.
 *
 * linksExtracted  : 파싱에 성공한 링크 수 (건강 체크용 지표. 새 글 유무와 무관하게 거의 일정해야 정상)
 * newlySavedCount : 이번 사이클에 DB에 새로 저장된 공지 수 (나중에 푸시 알림 트리거로 쓸 지표)
 * 두 값을 섞으면 "이미 있던 글"을 신규로 오인해 알림이 중복될 수 있어 컬럼을 분리했다.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "crawl_log", indexes = {
        @Index(name = "idx_crawl_log_board_started", columnList = "board_id, started_at")
})
public class CrawlLog extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "board_id", nullable = false)
    private Board board;

    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    @Column(name = "duration_ms", nullable = false)
    private long durationMs;

    /** 수집+파싱+저장이 모두 오류 없이 끝났는지 */
    @Column(nullable = false)
    private boolean success;

    /** HTTP 오류 응답이었다면 그 상태 코드 (차단 진단용) */
    @Column(name = "http_status")
    private Integer httpStatus;

    @Column(name = "rows_found", nullable = false)
    private int rowsFound;

    @Column(name = "links_extracted", nullable = false)
    private int linksExtracted;

    @Column(name = "newly_saved_count", nullable = false)
    private int newlySavedCount;

    /** linksExtracted / rowsFound. rowsFound가 0이면 null */
    @Column(name = "extraction_rate")
    private Double extractionRate;

    @Enumerated(EnumType.STRING)
    @Column(name = "health_status", nullable = false, length = 20, columnDefinition = "varchar(20)")
    private CrawlHealth healthStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "issue", nullable = false, length = 30, columnDefinition = "varchar(30)")
    private CrawlIssue issue;

    /** 이 사이클에서 실제로 "전달에 성공한" 알림 종류 */
    @Enumerated(EnumType.STRING)
    @Column(name = "alert_type", nullable = false, length = 20, columnDefinition = "varchar(20)")
    private AlertType alertType;

    @Column(name = "error_message", length = 500)
    private String errorMessage;

    @Builder
    public CrawlLog(Board board, LocalDateTime startedAt, long durationMs, boolean success,
                    Integer httpStatus, int rowsFound, int linksExtracted, int newlySavedCount,
                    Double extractionRate, CrawlHealth healthStatus, CrawlIssue issue,
                    AlertType alertType, String errorMessage) {
        this.board = board;
        this.startedAt = startedAt;
        this.durationMs = durationMs;
        this.success = success;
        this.httpStatus = httpStatus;
        this.rowsFound = rowsFound;
        this.linksExtracted = linksExtracted;
        this.newlySavedCount = newlySavedCount;
        this.extractionRate = extractionRate;
        this.healthStatus = healthStatus;
        this.issue = issue;
        this.alertType = alertType;
        this.errorMessage = errorMessage;
    }
}
