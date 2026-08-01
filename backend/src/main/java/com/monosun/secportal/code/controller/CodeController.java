package com.monosun.secportal.code.controller;

import com.monosun.secportal.audit.service.AuditLogService;
import com.monosun.secportal.code.dto.CodeDto;
import com.monosun.secportal.code.service.CodeService;
import com.monosun.secportal.common.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class CodeController {

    private final CodeService codeService;
    private final AuditLogService auditLogService;

    // ── 공개 API (인증된 모든 사용자) ──────────────────────────────────────

    @GetMapping("/codes/{groupCode}")
    public ApiResponse<List<CodeDto.SimpleValue>> getActiveValues(@PathVariable String groupCode) {
        return ApiResponse.ok(codeService.listActiveValues(groupCode));
    }

    /** 화면 목록의 개인정보 마스킹에 사용할 항목별 기준 */
    @GetMapping("/codes/pi-masking")
    public ApiResponse<List<CodeDto.MaskingRule>> getPiMaskingRules() {
        return ApiResponse.ok(codeService.listPiMaskingRules());
    }

    /** 목록 화면의 마스킹 해제(원문 열람) 이력 — 관리자만 가능하며 감사로그로 남긴다 */
    @PostMapping("/codes/pi-masking/reveal")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    public void logPiUnmask(@RequestBody(required = false) PiUnmaskRequest req) {
        String screen = req != null && req.screen() != null && !req.screen().isBlank() ? req.screen() : "알 수 없는 화면";
        String reason = req != null && req.reason() != null && !req.reason().isBlank() ? req.reason() : "사유 미입력";
        auditLogService.log("PI_UNMASK", "PrivacyMasking", null,
                "개인정보 마스킹 해제 — 화면: " + screen + ", 사유: " + reason);
    }

    public record PiUnmaskRequest(String screen, String reason) {}

    // ── 관리자 API ──────────────────────────────────────────────────────────

    @GetMapping("/admin/codes")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<List<CodeDto.GroupResponse>> listGroups() {
        return ApiResponse.ok(codeService.listGroups());
    }

    @PostMapping("/admin/codes")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<CodeDto.GroupResponse> createGroup(@Valid @RequestBody CodeDto.GroupRequest req) {
        return ApiResponse.created(codeService.createGroup(req));
    }

    @PatchMapping("/admin/codes/{groupCode}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<CodeDto.GroupResponse> updateGroup(
            @PathVariable String groupCode,
            @Valid @RequestBody CodeDto.GroupRequest req) {
        return ApiResponse.ok(codeService.updateGroup(groupCode, req));
    }

    @DeleteMapping("/admin/codes/{groupCode}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    public void deleteGroup(@PathVariable String groupCode) {
        codeService.deleteGroup(groupCode);
    }

    @GetMapping("/admin/codes/{groupCode}/values")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<List<CodeDto.ValueResponse>> listValues(@PathVariable String groupCode) {
        return ApiResponse.ok(codeService.listValues(groupCode));
    }

    @PostMapping("/admin/codes/{groupCode}/values")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<CodeDto.ValueResponse> createValue(
            @PathVariable String groupCode,
            @Valid @RequestBody CodeDto.ValueRequest req) {
        return ApiResponse.created(codeService.createValue(groupCode, req));
    }

    @PatchMapping("/admin/codes/{groupCode}/values/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<CodeDto.ValueResponse> updateValue(
            @PathVariable String groupCode,
            @PathVariable Long id,
            @Valid @RequestBody CodeDto.ValueRequest req) {
        return ApiResponse.ok(codeService.updateValue(groupCode, id, req));
    }

    @DeleteMapping("/admin/codes/{groupCode}/values/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    public void deleteValue(@PathVariable String groupCode, @PathVariable Long id) {
        codeService.deleteValue(groupCode, id);
    }
}
