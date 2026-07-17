package com.monosun.secportal.privacy.controller;

import com.monosun.secportal.common.response.ApiResponse;
import com.monosun.secportal.privacy.dto.PrivacySafeguardDto;
import com.monosun.secportal.privacy.service.PrivacySafeguardService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/privacy/safeguards")
@RequiredArgsConstructor
public class PrivacySafeguardController {

    private final PrivacySafeguardService service;

    @GetMapping
    public ApiResponse<List<PrivacySafeguardDto.Response>> list() {
        return ApiResponse.ok(service.listAll());
    }

    @GetMapping("/{id}")
    public ApiResponse<PrivacySafeguardDto.Response> get(@PathVariable Long id) {
        return ApiResponse.ok(service.get(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ApiResponse<PrivacySafeguardDto.Response> create(@Valid @RequestBody PrivacySafeguardDto.Request req) {
        return ApiResponse.created(service.create(req));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ApiResponse<PrivacySafeguardDto.Response> update(
            @PathVariable Long id, @RequestBody PrivacySafeguardDto.Request req) {
        return ApiResponse.ok(service.update(id, req));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ApiResponse.noContent();
    }
}
