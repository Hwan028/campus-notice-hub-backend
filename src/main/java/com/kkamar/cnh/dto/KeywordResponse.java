package com.kkamar.cnh.dto;

import com.kkamar.cnh.domain.Keyword;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class KeywordResponse {
    private Long id;
    private String name;
    private boolean isPreset;

    public static KeywordResponse from(Keyword keyword) {
        return KeywordResponse.builder()
                .id(keyword.getId())
                .name(keyword.getName())
                .isPreset(keyword.isPreset())
                .build();
    }
}