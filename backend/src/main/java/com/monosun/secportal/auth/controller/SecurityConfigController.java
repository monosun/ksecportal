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
    public ApiResponse<Void> update(@RequestBody Map<String, Object> body,
                                    @org.springframework.security.core.annotation.AuthenticationPrincipal
                                    com.monosun.secportal.auth.entity.User user) {
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
        if (body.containsKey("requireAdminMfa") && body.get("requireAdminMfa") != null) {
            boolean require = Boolean.parseBoolean(body.get("requireAdminMfa").toString());
            // 본인이 MFA 없이 켜면 즉시 스스로 잠기므로 막는다
            if (require && user != null && !user.isMfaEnabled()
                    && (user.getOktaId() == null || user.getOktaId().isBlank())) {
                throw new com.monosun.secportal.common.exception.BusinessException(
                        "관리자 MFA 필수를 켜기 전에 본인 계정의 MFA 를 먼저 등록하세요(설정 > MFA).");
            }
            service.setAdminMfaRequired(require);
        }
        service.save(maxAttempts, lockoutMinutes);
        return ApiResponse.ok(null);
    }
}
