package com.kkamar.cnh.support;

import com.kkamar.cnh.exception.PageFetchException;
import com.kkamar.cnh.service.PageFetcher;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** 네트워크 대신 미리 등록한 HTML(또는 오류)을 돌려주는 테스트용 수집기. */
public class FakePageFetcher implements PageFetcher {

    private record Failure(Integer status) {
    }

    private static final String NULL_MARKER = "\u0000null-document\u0000";

    private final Map<String, String> pages = new ConcurrentHashMap<>();
    private final Map<String, Failure> failures = new ConcurrentHashMap<>();

    public void setPage(String url, String html) {
        failures.remove(url);
        pages.put(url, html);
    }

    /** status가 null이면 타임아웃 같은 네트워크 오류, 값이 있으면 해당 HTTP 오류 응답 */
    public void failWith(String url, Integer status) {
        failures.put(url, new Failure(status));
    }

    /** 파서가 NPE를 던지도록 null 문서를 돌려준다 (파싱 중 예외 시나리오용) */
    public void returnNullDocument(String url) {
        failures.remove(url);
        pages.put(url, NULL_MARKER);
    }

    public void reset() {
        pages.clear();
        failures.clear();
    }

    @Override
    public Document fetch(String url) throws PageFetchException {
        Failure failure = failures.get(url);
        if (failure != null) {
            throw new PageFetchException("fake failure for " + url, failure.status(), null);
        }
        String html = pages.get(url);
        if (html == null) {
            throw new PageFetchException("no fake page registered for " + url, 404, null);
        }
        if (NULL_MARKER.equals(html)) {
            return null;
        }
        return Jsoup.parse(html, url);
    }
}
