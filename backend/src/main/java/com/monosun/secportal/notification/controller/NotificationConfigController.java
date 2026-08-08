package com.monosun.secportal.notification.controller;

import com.monosun.secportal.notification.service.NotificationConfigService;
import com.monosun.secportal.notification.service.SlackService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/admin/notification-config")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class NotificationConfigController {

    private final NotificationConfigService configService;
    private final SlackService slackService;

    @GetMapping
    public ResponseEntity<Map<String, Object>> get() {
        return ResponseEntity.ok(configService.getAll());
    }

    @PutMapping
    public ResponseEntity<Map<String, Object>> update(@RequestBody Map<String, String> body) {
        configService.save(body);
        return ResponseEntity.ok(configService.getAll());
    }

    /**
     * 저장된 Slack 설정으로 연동을 점검한다(저장 후 실행).
     * WEBHOOK 방식은 테스트 메시지를 발송하고, SOCKET 방식은 봇 토큰·앱 레벨 토큰을 확인한 뒤
     * 채널이 지정되어 있으면 테스트 메시지까지 발송한다.
     */
    @PostMapping("/slack/test")
    public ResponseEntity<Map<String, Object>> testSlack() {
        String text = "[KSecPortal] Slack 연동 테스트 메시지입니다.";
        Map<String, Object> out = new LinkedHashMap<>();
        String mode = configService.getSlackMode();
        out.put("mode", mode);

        if (!"SOCKET".equals(mode)) {
            SlackService.Result r = slackService.sendWebhook(configService.getSlackWebhookUrl(), text);
            out.put("success", r.success());
            out.put("message", r.message());
            return ResponseEntity.ok(out);
        }

        StringBuilder msg = new StringBuilder();
        SlackService.Result auth = slackService.authTest(configService.getSlackBotToken());
        msg.append(auth.message());
        if (!auth.success()) {
            out.put("success", false);
            out.put("message", msg.toString());
            return ResponseEntity.ok(out);
        }

        String appToken = configService.getSlackAppToken();
        if (!appToken.isBlank()) {
            SlackService.Result socket = slackService.checkSocketConnection(appToken);
            msg.append(" / ").append(socket.message());
            if (!socket.success()) {
                out.put("success", false);
                out.put("message", msg.toString());
                return ResponseEntity.ok(out);
            }
        }

        String channel = configService.getSlackChannel();
        if (!channel.isBlank()) {
            SlackService.Result post = slackService.postMessage(configService.getSlackBotToken(), channel, text);
            msg.append(" / ").append(post.message());
            out.put("success", post.success());
        } else {
            msg.append(" / 채널이 지정되지 않아 테스트 메시지는 발송하지 않았습니다.");
            out.put("success", true);
        }
        out.put("message", msg.toString());
        return ResponseEntity.ok(out);
    }
}
