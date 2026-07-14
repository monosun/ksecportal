package com.monosun.secportal.notification.controller;

import com.monosun.secportal.common.response.ApiResponse;
import com.monosun.secportal.notification.dto.MailConfigDto;
import com.monosun.secportal.notification.service.MailConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 발송 메일서버(SMTP) 설정 — ADMIN 전용. 비밀번호는 마스킹 응답.
 */
@RestController
@RequestMapping("/admin/mail-config")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class MailConfigController {

    private final MailConfigService service;

    @GetMapping
    public ApiResponse<MailConfigDto.ConfigResponse> get() {
        return ApiResponse.ok(service.getConfig());
    }

    @PutMapping
    public ApiResponse<MailConfigDto.ConfigResponse> save(@RequestBody MailConfigDto.ConfigRequest req) {
        return ApiResponse.ok(service.saveConfig(req));
    }

    @PostMapping("/test")
    public ApiResponse<MailConfigDto.TestResult> test(@RequestBody(required = false) MailConfigDto.TestRequest req) {
        return ApiResponse.ok(service.testConnection(req != null ? req : new MailConfigDto.TestRequest()));
    }
}
