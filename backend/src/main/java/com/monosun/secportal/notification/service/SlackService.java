package com.monosun.secportal.notification.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@Slf4j
@Service
public class SlackService {

    private static final String ALLOWED_SLACK_HOST = "hooks.slack.com";

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    @Async
    public void send(String webhookUrl, String text) {
        if (webhookUrl == null || webhookUrl.isBlank()) {
            log.warn("Slack webhook URL is not configured");
            return;
        }
        try {
            URI parsedUri = URI.create(webhookUrl);
            if (!"https".equals(parsedUri.getScheme()) || !ALLOWED_SLACK_HOST.equals(parsedUri.getHost())) {
                log.warn("Rejected Slack webhook URL with disallowed host: {}", parsedUri.getHost());
                return;
            }
            String body = "{\"text\":" + jsonEscape(text) + "}";
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(webhookUrl))
                    .timeout(Duration.ofSeconds(10))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();
            HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());
            if (res.statusCode() != 200) {
                log.warn("Slack responded with status {}: {}", res.statusCode(), res.body());
            } else {
                log.info("Slack message sent successfully");
            }
        } catch (Exception e) {
            log.warn("Failed to send Slack message: {}", e.getMessage());
        }
    }

    private String jsonEscape(String text) {
        return "\"" + text
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "")
                + "\"";
    }
}
