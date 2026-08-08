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
            case "SLACK" -> sendSlack(slackText);
            case "BOTH" -> {
                emailService.send(config.getApprovalEmail(), emailSubject, emailHtml);
                sendSlack(slackText);
            }
            default -> emailService.send(config.getApprovalEmail(), emailSubject, emailHtml);
        }
    }

    /** 설정된 Slack 연동 방식(Webhook / 소켓 모드 봇 토큰)에 맞춰 발송한다. */
    private void sendSlack(String slackText) {
        if ("SOCKET".equals(config.getSlackMode())) {
            slackService.sendApi(config.getSlackBotToken(), config.getSlackChannel(), slackText);
        } else {
            slackService.send(config.getSlackWebhookUrl(), slackText);
        }
    }

    public String getApprovalEmail() {
        return config.getApprovalEmail();
    }
}
