package com.kkamar.cnh.dto;

import com.kkamar.cnh.domain.Scrap;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class ScrapResponse {
    private Long scrapId;
    private int colorIndex;
    private LocalDateTime scrappedAt;
    private NoticeResponse notice;

    /** notice.board 까지 로딩된 상태(트랜잭션 안 또는 EntityGraph)에서만 호출해야 한다. */
    public static ScrapResponse from(Scrap scrap) {
        return ScrapResponse.builder()
                .scrapId(scrap.getId())
                .colorIndex(scrap.getColorIndex())
                .scrappedAt(scrap.getCreatedAt())
                .notice(NoticeResponse.from(scrap.getNotice()))
                .build();
    }
}
