package com.monosun.secportal.notification.service;

import com.monosun.secportal.notification.entity.NotificationConfig;
import com.monosun.secportal.notification.repository.NotificationConfigRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class NotificationConfigService {

    static final String KEY_METHOD        = "notification.method";
    static final String KEY_EMAIL         = "notification.approval-email";
    static final String KEY_SLACK_WEBHOOK = "notification.slack-webhook-url";

    static final String DEFAULT_EMAIL  = "noreply@monosun.com";
    static final String DEFAULT_METHOD = "EMAIL";

    private final NotificationConfigRepository repo;

    public String getApprovalEmail() {
        return repo.findById(KEY_EMAIL)
                .map(NotificationConfig::getConfigValue)
                .filter(v -> !v.isBlank())
                .orElse(DEFAULT_EMAIL);
    }

    public String getNotificationMethod() {
        return repo.findById(KEY_METHOD)
                .map(NotificationConfig::getConfigValue)
                .filter(v -> !v.isBlank())
                .orElse(DEFAULT_METHOD);
    }

    public String getSlackWebhookUrl() {
        return repo.findById(KEY_SLACK_WEBHOOK)
                .map(NotificationConfig::getConfigValue)
                .orElse("");
    }

    @Transactional
    public void save(Map<String, String> settings) {
        if (settings.containsKey("method"))
            upsert(KEY_METHOD, settings.get("method"));
        if (settings.containsKey("approvalEmail"))
            upsert(KEY_EMAIL, settings.get("approvalEmail"));
        if (settings.containsKey("slackWebhookUrl"))
            upsert(KEY_SLACK_WEBHOOK, settings.get("slackWebhookUrl"));
    }

    private void upsert(String key, String value) {
        repo.save(new NotificationConfig(key, value == null ? "" : value));
    }

    public Map<String, String> getAll() {
        return Map.of(
            "method",          getNotificationMethod(),
            "approvalEmail",   getApprovalEmail(),
            "slackWebhookUrl", getSlackWebhookUrl()
        );
    }
}
