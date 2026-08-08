package com.monosun.secportal.notification.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Slack 발송기 — 두 가지 연동 방식을 지원한다.
 *
 * <ul>
 *   <li><b>WEBHOOK</b>: Incoming Webhook URL 로 POST (hooks.slack.com 만 허용)</li>
 *   <li><b>SOCKET</b>: 소켓 모드 앱의 봇 토큰(xoxb-)으로 Web API {@code chat.postMessage} 호출.
 *       소켓 모드의 WebSocket 은 Slack → 앱 방향(이벤트 수신)의 전송 수단이고,
 *       앱 → Slack 방향의 메시지 발송은 소켓 모드 앱도 Web API 를 사용한다.
 *       앱 레벨 토큰(xapp-)은 소켓 연결 발급 가능 여부 점검(연결 테스트)에 사용한다.</li>
 * </ul>
 */
@Slf4j
@Service
public class SlackService {

    private static final String ALLOWED_SLACK_HOST = "hooks.slack.com";
    private static final String SLACK_API_BASE = "https://slack.com/api/";

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    private final ObjectMapper mapper = new ObjectMapper();

    /** 발송/점검 결과. message 는 사용자에게 그대로 보여줄 수 있는 문구. */
    public record Result(boolean success, String message) {}

    // ── Webhook 방식 ─────────────────────────────────────────────────────

    @Async
    public void send(String webhookUrl, String text) {
        sendWebhook(webhookUrl, text);
    }

    /** 동기 발송 — 연결 테스트에서 결과를 즉시 확인하기 위해 사용한다. */
    public Result sendWebhook(String webhookUrl, String text) {
        if (webhookUrl == null || webhookUrl.isBlank()) {
            log.warn("Slack webhook URL is not configured");
            return new Result(false, "Slack Webhook URL이 설정되지 않았습니다.");
        }
        try {
            URI parsedUri = URI.create(webhookUrl);
            if (!"https".equals(parsedUri.getScheme()) || !ALLOWED_SLACK_HOST.equals(parsedUri.getHost())) {
                log.warn("Rejected Slack webhook URL with disallowed host: {}", parsedUri.getHost());
                return new Result(false, "허용되지 않은 Webhook 주소입니다. https://hooks.slack.com/... 형식이어야 합니다.");
            }
            String body = "{\"text\":" + jsonEscape(text) + "}";
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(parsedUri)
                    .timeout(Duration.ofSeconds(10))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();
            HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());
            if (res.statusCode() != 200) {
                log.warn("Slack responded with status {}: {}", res.statusCode(), res.body());
                return new Result(false, "Slack 응답 오류(HTTP " + res.statusCode() + "): " + res.body());
            }
            log.info("Slack message sent successfully");
            return new Result(true, "Webhook으로 메시지를 전송했습니다.");
        } catch (Exception e) {
            log.warn("Failed to send Slack message: {}", e.getMessage());
            return new Result(false, "전송 실패: " + e.getMessage());
        }
    }

    // ── 소켓 모드(봇 토큰) 방식 ──────────────────────────────────────────

    @Async
    public void sendApi(String botToken, String channel, String text) {
        Result r = postMessage(botToken, channel, text);
        if (!r.success()) log.warn("Slack chat.postMessage failed: {}", r.message());
    }

    /** 봇 토큰으로 채널에 메시지를 발송한다. */
    public Result postMessage(String botToken, String channel, String text) {
        if (botToken == null || botToken.isBlank()) {
            return new Result(false, "Slack 봇 토큰(xoxb-)이 설정되지 않았습니다.");
        }
        if (channel == null || channel.isBlank()) {
            return new Result(false, "Slack 채널이 설정되지 않았습니다.");
        }
        String body = "{\"channel\":" + jsonEscape(channel) + ",\"text\":" + jsonEscape(text) + "}";
        ApiResult r = callApi("chat.postMessage", botToken, body);
        return r.success()
                ? new Result(true, channel + " 채널로 메시지를 전송했습니다.")
                : new Result(false, "메시지 전송 실패: " + r.detail());
    }

    /** 봇 토큰 유효성 확인 — 성공 시 워크스페이스/봇 이름을 안내 문구로 돌려준다. */
    public Result authTest(String botToken) {
        if (botToken == null || botToken.isBlank()) {
            return new Result(false, "Slack 봇 토큰(xoxb-)이 설정되지 않았습니다.");
        }
        ApiResult r = callApi("auth.test", botToken, "{}");
        if (!r.success()) return new Result(false, "봇 토큰 확인 실패: " + r.detail());
        String team = text(r.body(), "team");
        String user = text(r.body(), "user");
        return new Result(true, "봇 토큰 정상 (워크스페이스: " + (team.isBlank() ? "-" : team)
                + ", 봇: " + (user.isBlank() ? "-" : user) + ")");
    }

    /**
     * 앱 레벨 토큰(xapp-)으로 소켓 연결 발급이 가능한지 확인한다.
     * 실제 WebSocket 을 열지는 않고 연결 URL 발급 여부만 점검한다.
     */
    public Result checkSocketConnection(String appToken) {
        if (appToken == null || appToken.isBlank()) {
            return new Result(false, "앱 레벨 토큰(xapp-)이 설정되지 않았습니다.");
        }
        ApiResult r = callApi("apps.connections.open", appToken, "{}");
        return r.success()
                ? new Result(true, "소켓 연결 발급 정상 (앱 레벨 토큰 유효)")
                : new Result(false, "소켓 연결 발급 실패: " + r.detail());
    }

    // ── helpers ──────────────────────────────────────────────────────────

    private record ApiResult(boolean success, String detail, JsonNode body) {}

    private ApiResult callApi(String method, String token, String jsonBody) {
        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(SLACK_API_BASE + method))
                    .timeout(Duration.ofSeconds(10))
                    .header("Content-Type", "application/json; charset=utf-8")
                    .header("Authorization", "Bearer " + token)
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();
            HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());
            if (res.statusCode() != 200) {
                return new ApiResult(false, "HTTP " + res.statusCode(), null);
            }
            JsonNode node = mapper.readTree(res.body());
            if (node.path("ok").asBoolean(false)) {
                return new ApiResult(true, "", node);
            }
            // Slack 은 오류도 HTTP 200 + {"ok":false,"error":"..."} 로 응답한다
            return new ApiResult(false, node.path("error").asText("unknown_error"), node);
        } catch (Exception e) {
            log.warn("Slack API call {} failed: {}", method, e.getMessage());
            return new ApiResult(false, e.getMessage(), null);
        }
    }

    private String text(JsonNode node, String field) {
        return node == null ? "" : node.path(field).asText("");
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
