package com.monosun.secportal.auth.controller;

import com.monosun.secportal.auth.service.SecurityConfigService;
import com.monosun.secportal.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/admin/security-config")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class SecurityConfigController {

    private final SecurityConfigService service;

    @GetMapping
    public ApiResponse<Map<String, Object>> get() {
        return ApiResponse.ok(service.getAll());
    }

    @PutMapping
    public ApiResponse<Void> update(@RequestBody Map<String, Object> body) {
        int maxAttempts = body.containsKey("maxAttempts")
                ? Integer.parseInt(body.get("maxAttempts").toString())
                : service.getMaxAttempts();
        int lockoutMinutes = body.containsKey("lockoutMinutes")
                ? Integer.parseInt(body.get("lockoutMinutes").toString())
                : service.getLockoutMinutes();
        if (maxAttempts < 1 || maxAttempts > 5) {
            throw new com.monosun.secportal.common.exception.BusinessException("최대 로그인 실패 횟수는 1~5 사이여야 합니다.");
        }
        if (lockoutMinutes < 1 || lockoutMinutes > 1440) {
            throw new com.monosun.secportal.common.exception.BusinessException("잠금 시간은 1~1440분 사이여야 합니다.");
        }
        service.save(maxAttempts, lockoutMinutes);
        return ApiResponse.ok(null);
    }
}
