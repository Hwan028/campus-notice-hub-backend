package com.kkamar.cnh.dto;

import lombok.Builder;
import lombok.Getter;
import org.springframework.data.domain.Page;

import java.util.List;

/** 목록 API 공통 페이지 응답. 앱이 "더 불러올 게 있는지"를 hasNext로 판단할 수 있다. */
@Getter
@Builder
public class PageResponse<T> {
    private List<T> content;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
    private boolean hasNext;

    public static <T> PageResponse<T> from(Page<T> page) {
        return PageResponse.<T>builder()
                .content(page.getContent())
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .hasNext(page.hasNext())
                .build();
    }
}
