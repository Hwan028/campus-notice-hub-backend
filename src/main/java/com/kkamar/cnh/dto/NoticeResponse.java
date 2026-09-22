package com.kkamar.cnh.dto;

import com.kkamar.cnh.domain.Notice;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@Builder
public class NoticeResponse {
    private Long id;
    private String title;
    private String originalUrl;
    private LocalDate postedAt;
    private String boardName;

    public static NoticeResponse from(Notice notice) {
        return NoticeResponse.builder()
                .id(notice.getId())
                .title(notice.getTitle())
                .originalUrl(notice.getOriginalUrl())
                .postedAt(notice.getPostedAt())
                .boardName(notice.getBoard().getName())
                .build();
    }
}