package com.kkamar.cnh.service.impl;

import com.kkamar.cnh.component.CrawlerProperties;
import com.kkamar.cnh.exception.PageFetchException;
import com.kkamar.cnh.service.PageFetcher;
import lombok.RequiredArgsConstructor;
import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JsoupPageFetcher implements PageFetcher {

    private final CrawlerProperties properties;

    @Override
    public Document fetch(String url) throws PageFetchException {
        try {
            return Jsoup.connect(url)
                    .userAgent(properties.getFetch().getUserAgent())
                    .timeout(properties.getFetch().getTimeoutMs())
                    .get();
        } catch (HttpStatusException e) {
            // 4xx/5xx 응답: 상태 코드를 보존해야 "차단(403/429)"과 "일시 장애"를 구분할 수 있다.
            throw new PageFetchException("HTTP " + e.getStatusCode() + " for " + url, e.getStatusCode(), e);
        } catch (IOException e) {
            throw new PageFetchException(e.getClass().getSimpleName() + ": " + e.getMessage(), null, e);
        }
    }
}
