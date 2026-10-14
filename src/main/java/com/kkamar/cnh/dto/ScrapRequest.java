package com.kkamar.cnh.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ScrapRequest {

    @NotNull
    private Long deviceId;

    @NotNull
    private Long noticeId;

    /** 0~7. 생략하면 서버가 디바이스의 스크랩 개수 기준으로 순환 배정한다. */
    @Min(0)
    @Max(7)
    private Integer colorIndex;
}
