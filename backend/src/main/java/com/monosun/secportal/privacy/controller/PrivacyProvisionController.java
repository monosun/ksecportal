package com.monosun.secportal.privacy.controller;

import com.monosun.secportal.common.response.ApiResponse;
import com.monosun.secportal.privacy.dto.PrivacyProvisionDto;
import com.monosun.secportal.privacy.service.PrivacyProvisionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/privacy/provisions")
@RequiredArgsConstructor
public class PrivacyProvisionController {

    private final PrivacyProvisionService service;

    /** 제공 목록 — processingId를 주면 해당 처리업무에 연계된 건만 조회한다. */
    @GetMapping
    public ApiResponse<List<PrivacyProvisionDto.Response>> list(
            @RequestParam(required = false) Long processingId) {
        return ApiResponse.ok(processingId != null
                ? service.listByProcessing(processingId)
                : service.listAll());
    }

    @GetMapping("/{id}")
    public ApiResponse<PrivacyProvisionDto.Response> get(@PathVariable Long id) {
        return ApiResponse.ok(service.get(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ApiResponse<PrivacyProvisionDto.Response> create(@Valid @RequestBody PrivacyProvisionDto.Request req) {
        return ApiResponse.created(service.create(req));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ApiResponse<PrivacyProvisionDto.Response> update(
            @PathVariable Long id, @RequestBody PrivacyProvisionDto.Request req) {
        return ApiResponse.ok(service.update(id, req));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ApiResponse.noContent();
    }
}
