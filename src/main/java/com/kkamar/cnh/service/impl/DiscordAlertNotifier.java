package com.kkamar.cnh.service.impl;

import com.kkamar.cnh.component.CrawlerProperties;
import com.kkamar.cnh.dto.CrawlAlert;
import com.kkamar.cnh.service.AlertNotifier;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.net.URI;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Map;

/**
 * Discord 웹훅으로 알림을 보낸다.
 * 웹훅 주소가 설정되지 않았으면 로그에만 남기고 false를 반환한다
 * (=전달 안 됨으로 취급되어, 설정 전까지 매 사이클 로그로 계속 경고가 남는다).
 * 웹훅 URL 자체는 비밀 값이므로 로그에 절대 출력하지 않는다.
 */
@Slf4j
@Component
public class DiscordAlertNotifier implements AlertNotifier {

    private static final int DISCORD_CONTENT_LIMIT = 1900;
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final String webhookUrl;
    private final RestClient restClient;

    public DiscordAlertNotifier(CrawlerProperties properties) {
        this.webhookUrl = properties.getAlert().getDiscordWebhookUrl();

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(3_000);
        factory.setReadTimeout(5_000);
        this.restClient = RestClient.builder().requestFactory(factory).build();
    }

    @Override
    public boolean send(CrawlAlert alert) {
        String text = format(alert);

        if (webhookUrl == null || webhookUrl.isBlank()) {
            log.warn("[ALERT not delivered - discord webhook not configured]\n{}", text);
            return false;
        }

        try {
            restClient.post()
                    .uri(URI.create(webhookUrl))
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("username", "Campus Notice Hub", "content", text))
                    .retrieve()
                    .toBodilessEntity();
            log.info("Discord alert delivered: type={}, board={}", alert.type(), alert.boardName());
            return true;
        } catch (RuntimeException e) {
            log.error("Discord alert delivery failed: type={}, board={}, cause={}",
                    alert.type(), alert.boardName(), e.getClass().getSimpleName());
            return false;
        }
    }

    String format(CrawlAlert a) {
        String title = switch (a.type()) {
            case INCIDENT -> "🚨 크롤러 이상 감지";
            case REMINDER -> "⏰ 크롤러 이상 지속 중";
            case RECOVERED -> "✅ 크롤러 정상 복구";
            case NONE -> "ℹ️ 크롤러 상태";
        };

        StringBuilder sb = new StringBuilder();
        sb.append("**").append(title).append("** — ").append(a.boardName()).append('\n');
        sb.append("• 상태: ").append(a.status()).append(" / ").append(a.issue()).append('\n');
        sb.append("• 내용: ").append(a.message()).append('\n');
        sb.append("• rows found / links extracted: ").append(a.rowsFound()).append(" / ").append(a.linksExtracted()).append('\n');
        if (a.currentRate() != null) {
            sb.append("• extraction_rate: ").append(percent(a.currentRate()));
            if (a.baselineRate() != null) {
                sb.append(" (최근 정상 평균 ").append(percent(a.baselineRate())).append(')');
            }
            sb.append('\n');
        }
        if (a.consecutiveBad() > 0) {
            sb.append("• 연속 이상 횟수: ").append(a.consecutiveBad()).append("회\n");
        }
        sb.append("• 마지막 정상 수집: ")
                .append(a.lastSuccessAt() != null ? a.lastSuccessAt().format(TIME_FORMAT) : "기록 없음")
                .append('\n');
        sb.append("• 발생 시각: ").append(a.occurredAt().format(TIME_FORMAT));

        String text = sb.toString();
        return text.length() > DISCORD_CONTENT_LIMIT ? text.substring(0, DISCORD_CONTENT_LIMIT) : text;
    }

    private static String percent(double rate) {
        return String.format(Locale.ROOT, "%.0f%%", rate * 100);
    }
}
