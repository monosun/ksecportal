package com.monosun.secportal.privacy.controller;

import com.monosun.secportal.common.response.ApiResponse;
import com.monosun.secportal.privacy.dto.PrivacyConsentDto;
import com.monosun.secportal.privacy.service.PrivacyConsentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/privacy/consents")
@RequiredArgsConstructor
public class PrivacyConsentController {

    private final PrivacyConsentService service;

    @GetMapping
    public ApiResponse<List<PrivacyConsentDto.Response>> list() {
        return ApiResponse.ok(service.listAll());
    }

    @GetMapping("/{id}")
    public ApiResponse<PrivacyConsentDto.Response> get(@PathVariable Long id) {
        return ApiResponse.ok(service.get(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ApiResponse<PrivacyConsentDto.Response> create(@Valid @RequestBody PrivacyConsentDto.Request req) {
        return ApiResponse.created(service.create(req));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ApiResponse<PrivacyConsentDto.Response> update(
            @PathVariable Long id, @RequestBody PrivacyConsentDto.Request req) {
        return ApiResponse.ok(service.update(id, req));
    }

    /** 동의서 개정본 생성 — 기존 버전을 복사해 DRAFT 상태의 새 버전으로 등록 */
    @PostMapping("/{id}/versions")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ApiResponse<PrivacyConsentDto.Response> newVersion(
            @PathVariable Long id, @RequestBody Map<String, String> body) {
        return ApiResponse.created(service.newVersion(id, body.get("version")));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ApiResponse.noContent();
    }
}
