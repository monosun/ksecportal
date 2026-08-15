package com.monosun.secportal.notification.service;

import com.monosun.secportal.audit.service.AuditLogService;
import com.monosun.secportal.notification.entity.NotificationConfig;
import com.monosun.secportal.notification.repository.NotificationConfigRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class NotificationConfigService {

    static final String KEY_METHOD          = "notification.method";
    static final String KEY_EMAIL           = "notification.approval-email";
    static final String KEY_SLACK_WEBHOOK   = "notification.slack-webhook-url";
    static final String KEY_SLACK_MODE      = "notification.slack-mode";
    // 아래 두 상수는 토큰 값이 아니라 notification_config 테이블의 설정 '키 이름'이다.
    // 실제 토큰은 코드에 없고 관리자가 저장한 DB 값을 런타임에 읽는다.
    static final String KEY_SLACK_BOT_TOKEN = "notification.slack-bot-token"; // sast:ignore 설정 키 이름(자격증명 아님)
    static final String KEY_SLACK_APP_TOKEN = "notification.slack-app-token"; // sast:ignore 설정 키 이름(자격증명 아님)
    static final String KEY_SLACK_CHANNEL   = "notification.slack-channel";

    static final String DEFAULT_EMAIL      = "noreply@monosun.com";
    static final String DEFAULT_METHOD     = "EMAIL";
    /** Slack 연동 방식 — WEBHOOK(Incoming Webhook) | SOCKET(소켓 모드 앱 봇 토큰) */
    static final String DEFAULT_SLACK_MODE = "WEBHOOK";

    private final NotificationConfigRepository repo;
    private final AuditLogService auditLogService;

    public String getApprovalEmail() {
        return value(KEY_EMAIL, DEFAULT_EMAIL);
    }

    public String getNotificationMethod() {
        return value(KEY_METHOD, DEFAULT_METHOD);
    }

    public String getSlackWebhookUrl() {
        return repo.findById(KEY_SLACK_WEBHOOK)
                .map(NotificationConfig::getConfigValue)
                .orElse("");
    }

    public String getSlackMode() {
        String v = value(KEY_SLACK_MODE, DEFAULT_SLACK_MODE);
        return "SOCKET".equals(v) ? "SOCKET" : DEFAULT_SLACK_MODE;
    }

    public String getSlackBotToken() {
        return value(KEY_SLACK_BOT_TOKEN, "");
    }

    public String getSlackAppToken() {
        return value(KEY_SLACK_APP_TOKEN, "");
    }

    public String getSlackChannel() {
        return value(KEY_SLACK_CHANNEL, "");
    }

    @Transactional
    public void save(Map<String, String> settings) {
        if (settings.containsKey("method"))
            upsert(KEY_METHOD, settings.get("method"));
        if (settings.containsKey("approvalEmail"))
            upsert(KEY_EMAIL, settings.get("approvalEmail"));
        if (settings.containsKey("slackWebhookUrl"))
            upsert(KEY_SLACK_WEBHOOK, settings.get("slackWebhookUrl"));
        if (settings.containsKey("slackMode"))
            upsert(KEY_SLACK_MODE, "SOCKET".equals(settings.get("slackMode")) ? "SOCKET" : DEFAULT_SLACK_MODE);
        if (settings.containsKey("slackChannel"))
            upsert(KEY_SLACK_CHANNEL, settings.get("slackChannel"));
        // 토큰: 빈 값이면 기존 유지, "-" 이면 삭제, 그 외 값은 교체
        upsertSecret(KEY_SLACK_BOT_TOKEN, settings.get("slackBotToken"));
        upsertSecret(KEY_SLACK_APP_TOKEN, settings.get("slackAppToken"));

        auditLogService.log("NOTIFICATION_CONFIG_UPDATED", "NOTIFICATION_CONFIG", null,
                "method=" + getNotificationMethod() + ", slackMode=" + getSlackMode());
    }

    private void upsert(String key, String value) {
        repo.save(new NotificationConfig(key, value == null ? "" : value.trim()));
    }

    private void upsertSecret(String key, String value) {
        if (value == null || value.isBlank()) return;           // 미입력 — 기존 값 유지
        upsert(key, "-".equals(value.trim()) ? "" : value);     // "-" — 삭제
    }

    private String value(String key, String fallback) {
        return repo.findById(key)
                .map(NotificationConfig::getConfigValue)
                .filter(v -> !v.isBlank())
                .orElse(fallback);
    }

    /** 토큰은 마스킹해서 내보낸다 — 저장 여부(*Stored)와 미리보기(*Masked)만 제공. */
    public Map<String, Object> getAll() {
        String botToken = getSlackBotToken();
        String appToken = getSlackAppToken();
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("method",            getNotificationMethod());
        out.put("approvalEmail",     getApprovalEmail());
        out.put("slackWebhookUrl",   getSlackWebhookUrl());
        out.put("slackMode",         getSlackMode());
        out.put("slackChannel",      getSlackChannel());
        out.put("slackBotTokenStored", !botToken.isBlank());
        out.put("slackBotTokenMasked", mask(botToken));
        out.put("slackAppTokenStored", !appToken.isBlank());
        out.put("slackAppTokenMasked", mask(appToken));
        return out;
    }

    private String mask(String v) {
        if (v == null || v.isBlank()) return "";
        if (v.length() <= 8) return "****";
        return v.substring(0, 6) + "****" + v.substring(v.length() - 2);
    }
}
