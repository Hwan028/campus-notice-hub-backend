package com.kkamar.cnh.service;

import com.kkamar.cnh.exception.PageFetchException;
import org.jsoup.nodes.Document;

/**
 * 게시판 페이지를 가져오는 책임만 분리한 인터페이스.
 * 운영에서는 Jsoup 구현체를, 테스트에서는 저장된 HTML을 돌려주는 가짜 구현체를 끼워
 * 네트워크 없이 크롤러 전체 흐름(수집 -> 파싱 -> 저장 -> 진단 -> 알림)을 검증한다.
 */
public interface PageFetcher {
    Document fetch(String url) throws PageFetchException;
}
