package com.monosun.secportal.privacy.controller;

import com.monosun.secportal.common.response.ApiResponse;
import com.monosun.secportal.privacy.dto.PrivacyRetentionDto;
import com.monosun.secportal.privacy.service.PrivacyRetentionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/privacy/retentions")
@RequiredArgsConstructor
public class PrivacyRetentionController {

    private final PrivacyRetentionService service;

    @GetMapping
    public ApiResponse<List<PrivacyRetentionDto.Response>> list() {
        return ApiResponse.ok(service.listAll());
    }

    /** 만료예정 목록 — 기본 30일 이내 */
    @GetMapping("/expiring")
    public ApiResponse<List<PrivacyRetentionDto.Response>> listExpiring(
            @RequestParam(defaultValue = "30") int days) {
        return ApiResponse.ok(service.listExpiring(days));
    }

    @GetMapping("/{id}")
    public ApiResponse<PrivacyRetentionDto.Response> get(@PathVariable Long id) {
        return ApiResponse.ok(service.get(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ApiResponse<PrivacyRetentionDto.Response> create(@Valid @RequestBody PrivacyRetentionDto.Request req) {
        return ApiResponse.created(service.create(req));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ApiResponse<PrivacyRetentionDto.Response> update(
            @PathVariable Long id, @RequestBody PrivacyRetentionDto.Request req) {
        return ApiResponse.ok(service.update(id, req));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ApiResponse.noContent();
    }
}
