package com.kkamar.cnh.dto;

import com.kkamar.cnh.enums.AlertType;
import com.kkamar.cnh.enums.CrawlHealth;
import com.kkamar.cnh.enums.CrawlIssue;

import java.time.LocalDateTime;

/** 알림 채널(Discord 등)에 전달할 내용. 채널과 무관한 값만 담는다. */
public record CrawlAlert(
        AlertType type,
        String boardName,
        CrawlHealth status,
        CrawlIssue issue,
        String message,
        Double baselineRate,
        Double currentRate,
        int rowsFound,
        int linksExtracted,
        int consecutiveBad,
        LocalDateTime lastSuccessAt,
        LocalDateTime occurredAt
) {
}
