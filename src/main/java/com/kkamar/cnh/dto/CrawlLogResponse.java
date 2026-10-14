package com.kkamar.cnh.dto;

import com.kkamar.cnh.domain.CrawlLog;

import java.time.LocalDateTime;

public record CrawlLogResponse(
        Long id,
        Long boardId,
        String boardName,
        LocalDateTime startedAt,
        long durationMs,
        boolean success,
        Integer httpStatus,
        int rowsFound,
        int linksExtracted,
        int newlySavedCount,
        Double extractionRate,
        String healthStatus,
        String issue,
        String alertType,
        String errorMessage
) {
    /** board는 조회 쿼리에서 @EntityGraph로 함께 로딩된 상태여야 한다. */
    public static CrawlLogResponse from(CrawlLog log) {
        return new CrawlLogResponse(
                log.getId(),
                log.getBoard().getId(),
                log.getBoard().getName(),
                log.getStartedAt(),
                log.getDurationMs(),
                log.isSuccess(),
                log.getHttpStatus(),
                log.getRowsFound(),
                log.getLinksExtracted(),
                log.getNewlySavedCount(),
                log.getExtractionRate(),
                log.getHealthStatus().name(),
                log.getIssue().name(),
                log.getAlertType().name(),
                log.getErrorMessage());
    }
}
