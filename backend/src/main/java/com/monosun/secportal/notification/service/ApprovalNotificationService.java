package com.monosun.secportal.notification.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ApprovalNotificationService {

    private final EmailService emailService;
    private final SlackService slackService;
    private final NotificationConfigService config;

    public void send(String emailSubject, String emailHtml, String slackText) {
        String method = config.getNotificationMethod();
        switch (method) {
            case "INBOX" -> { /* 수신함 전용 — 이메일·Slack 발송 생략 */ }
            case "SLACK" -> slackService.send(config.getSlackWebhookUrl(), slackText);
            case "BOTH" -> {
                emailService.send(config.getApprovalEmail(), emailSubject, emailHtml);
                slackService.send(config.getSlackWebhookUrl(), slackText);
            }
            default -> emailService.send(config.getApprovalEmail(), emailSubject, emailHtml);
        }
    }

    public String getApprovalEmail() {
        return config.getApprovalEmail();
    }
}
