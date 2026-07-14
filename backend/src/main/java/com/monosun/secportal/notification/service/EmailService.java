package com.monosun.secportal.notification.service;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    /** application.yml(spring.mail.*) 기반 기본 발송기 (설정관리 메일서버 미설정 시 폴백) */
    private final JavaMailSender defaultMailSender;
    private final MailConfigService mailConfigService;

    @Value("${notification.from-address}")
    private String fromAddress;

    /** 설정관리 메일서버가 활성화되어 있으면 그 발송기를, 아니면 기본 발송기를 사용 */
    private JavaMailSender sender() {
        JavaMailSender configured = mailConfigService.resolveSender();
        return configured != null ? configured : defaultMailSender;
    }

    private String from() {
        return mailConfigService.resolveFrom(fromAddress);
    }

    /** 비동기 발송 — 실패해도 예외를 던지지 않고 로그만 남긴다(일반 알림용). */
    @Async
    public void send(String to, String subject, String htmlBody) {
        try {
            sendSync(to, subject, htmlBody);
        } catch (Exception e) {
            log.warn("Failed to send email to {}: {}", to, e.getMessage());
        }
    }

    /** 동기 발송 — 실패 시 예외를 던진다(발송 결과를 기록해야 하는 모의훈련·테스트용). */
    public void sendSync(String to, String subject, String htmlBody) throws Exception {
        JavaMailSender mailSender = sender();
        MimeMessage msg = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(msg, false, "UTF-8");
        helper.setFrom(from());
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(htmlBody, true);
        mailSender.send(msg);
        log.info("Email sent to {}: {}", to, subject);
    }
}
