package com.monosun.secportal.notification.service;

import com.monosun.secportal.audit.service.AuditLogService;
import com.monosun.secportal.notification.dto.MailConfigDto;
import com.monosun.secportal.notification.entity.MailConfig;
import com.monosun.secportal.notification.repository.MailConfigRepository;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

/**
 * 발송 메일서버(SMTP) 설정 관리 + 설정 기반 JavaMailSender 제공.
 * 설정이 활성화(enabled)되고 host가 지정된 경우 이 설정으로 메일을 발송하며,
 * 그렇지 않으면 EmailService가 application.yml 기본 설정으로 폴백한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MailConfigService {

    private final MailConfigRepository repo;
    private final AuditLogService auditLogService;

    @Transactional(readOnly = true)
    public MailConfigDto.ConfigResponse getConfig() {
        MailConfig c = repo.findFirstByOrderByIdAsc().orElse(null);
        return MailConfigDto.ConfigResponse.from(c, c != null ? mask(c.getPassword()) : null);
    }

    @Transactional
    public MailConfigDto.ConfigResponse saveConfig(MailConfigDto.ConfigRequest req) {
        MailConfig c = repo.findFirstByOrderByIdAsc().orElseGet(() -> MailConfig.builder().build());
        c.setHost(trimToNull(req.getHost()));
        c.setPort(req.getPort());
        c.setUsername(trimToNull(req.getUsername()));
        c.setFromAddress(trimToNull(req.getFromAddress()));
        c.setFromName(trimToNull(req.getFromName()));
        if (req.getUseAuth() != null) c.setUseAuth(req.getUseAuth());
        if (req.getUseStartTls() != null) c.setUseStartTls(req.getUseStartTls());
        if (req.getEnabled() != null) c.setEnabled(req.getEnabled());

        // 비밀번호: 빈 값이면 기존 유지, "-" 이면 삭제, 그 외 값은 교체
        String pw = req.getPassword();
        if (pw != null && !pw.isBlank()) {
            c.setPassword("-".equals(pw.trim()) ? null : pw);
        }

        MailConfig saved = repo.save(c);
        auditLogService.log("MAIL_CONFIG_UPDATED", "MAIL_CONFIG", saved.getId(),
                "host=" + saved.getHost() + ", enabled=" + saved.isEnabled());
        return MailConfigDto.ConfigResponse.from(saved, mask(saved.getPassword()));
    }

    /** 저장된 설정 기준으로 테스트 메일을 발송한다. */
    @Transactional(readOnly = true)
    public MailConfigDto.TestResult testConnection(MailConfigDto.TestRequest req) {
        MailConfig c = repo.findFirstByOrderByIdAsc().orElse(null);
        if (c == null || c.getHost() == null || c.getHost().isBlank()) {
            return MailConfigDto.TestResult.builder()
                    .success(false).message("메일서버 설정(호스트)이 저장되지 않았습니다.").build();
        }
        String to = req.getTo() != null && !req.getTo().isBlank() ? req.getTo().trim() : c.getUsername();
        if (to == null || to.isBlank()) {
            return MailConfigDto.TestResult.builder()
                    .success(false).message("테스트 메일 수신 주소를 입력하세요.").build();
        }
        try {
            JavaMailSender sender = buildSenderFrom(c);
            MimeMessage msg = sender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(msg, false, "UTF-8");
            helper.setFrom(fromAddressOf(c));
            helper.setTo(to);
            helper.setSubject("[SecPortal] 메일서버 설정 테스트");
            helper.setText("본 메일은 SecPortal 발송 메일서버 설정 테스트 메일입니다.\n정상적으로 수신되었다면 설정이 올바릅니다.", false);
            sender.send(msg);
            return MailConfigDto.TestResult.builder()
                    .success(true).message(to + " 주소로 테스트 메일을 발송했습니다.").build();
        } catch (Exception e) {
            log.warn("Mail config test failed: {}", e.getMessage());
            return MailConfigDto.TestResult.builder()
                    .success(false).message("발송 실패: " + e.getMessage()).build();
        }
    }

    /**
     * 활성화된 설정 기준 JavaMailSender. 미설정/비활성 시 null 반환(호출측이 기본 설정으로 폴백).
     */
    @Transactional(readOnly = true)
    public JavaMailSender resolveSender() {
        MailConfig c = repo.findFirstByOrderByIdAsc().orElse(null);
        if (c == null || !c.isEnabled() || c.getHost() == null || c.getHost().isBlank()) return null;
        return buildSenderFrom(c);
    }

    /** 활성화된 설정의 발신자 주소. 미설정 시 fallback 반환. */
    @Transactional(readOnly = true)
    public String resolveFrom(String fallback) {
        MailConfig c = repo.findFirstByOrderByIdAsc().orElse(null);
        if (c == null || !c.isEnabled()) return fallback;
        try {
            return fromAddressOf(c);
        } catch (Exception e) {
            return fallback;
        }
    }

    // ── helpers ──────────────────────────────────────────────────────────

    private JavaMailSender buildSenderFrom(MailConfig c) {
        JavaMailSenderImpl impl = new JavaMailSenderImpl();
        impl.setHost(c.getHost());
        if (c.getPort() != null) impl.setPort(c.getPort());
        if (c.getUsername() != null) impl.setUsername(c.getUsername());
        if (c.getPassword() != null) impl.setPassword(c.getPassword());
        impl.setDefaultEncoding(StandardCharsets.UTF_8.name());
        Properties props = impl.getJavaMailProperties();
        props.put("mail.smtp.auth", String.valueOf(c.isUseAuth()));
        props.put("mail.smtp.starttls.enable", String.valueOf(c.isUseStartTls()));
        props.put("mail.smtp.connectiontimeout", "5000");
        props.put("mail.smtp.timeout", "5000");
        props.put("mail.smtp.writetimeout", "5000");
        return impl;
    }

    private String fromAddressOf(MailConfig c) throws UnsupportedEncodingException {
        String addr = (c.getFromAddress() != null && !c.getFromAddress().isBlank())
                ? c.getFromAddress() : c.getUsername();
        if (c.getFromName() != null && !c.getFromName().isBlank() && addr != null) {
            return new InternetAddress(addr, c.getFromName(), "UTF-8").toString();
        }
        return addr;
    }

    private String mask(String v) {
        if (v == null || v.isBlank()) return null;
        if (v.length() <= 4) return "****";
        return v.substring(0, 2) + "****" + v.substring(v.length() - 2);
    }

    private String trimToNull(String v) {
        if (v == null) return null;
        String t = v.trim();
        return t.isEmpty() ? null : t;
    }
}
