package com.kkamar.cnh.support;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/** 운영용 Jsoup 수집기/Discord 알림 대신 가짜 구현을 주입한다. */
@TestConfiguration
public class TestFakesConfig {

    @Bean
    @Primary
    public FakePageFetcher fakePageFetcher() {
        return new FakePageFetcher();
    }

    @Bean
    @Primary
    public RecordingAlertNotifier recordingAlertNotifier() {
        return new RecordingAlertNotifier();
    }
}
