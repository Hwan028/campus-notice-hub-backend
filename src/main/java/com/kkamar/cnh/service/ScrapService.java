package com.kkamar.cnh.service;

import com.kkamar.cnh.dto.ScrapResponse;

import java.util.List;

public interface ScrapService {

    /** 이미 스크랩한 공지면 기존 스크랩을 그대로 돌려준다(멱등). colorIndex가 null이면 자동 배정. */
    ScrapResponse scrap(Long deviceId, Long noticeId, Integer colorIndex);

    /** 스크랩이 없어도 오류 없이 끝난다(멱등). */
    void unscrap(Long deviceId, Long noticeId);

    List<ScrapResponse> getScraps(Long deviceId);
}
