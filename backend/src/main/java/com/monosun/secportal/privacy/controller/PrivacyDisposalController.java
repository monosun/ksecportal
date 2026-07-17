package com.monosun.secportal.privacy.controller;

import com.monosun.secportal.common.response.ApiResponse;
import com.monosun.secportal.privacy.dto.PrivacyDisposalDto;
import com.monosun.secportal.privacy.service.PrivacyDisposalService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/privacy/disposals")
@RequiredArgsConstructor
public class PrivacyDisposalController {

    private final PrivacyDisposalService service;

    @GetMapping
    public ApiResponse<List<PrivacyDisposalDto.Response>> list() {
        return ApiResponse.ok(service.listAll());
    }

    @GetMapping("/{id}")
    public ApiResponse<PrivacyDisposalDto.Response> get(@PathVariable Long id) {
        return ApiResponse.ok(service.get(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ApiResponse<PrivacyDisposalDto.Response> create(@Valid @RequestBody PrivacyDisposalDto.Request req) {
        return ApiResponse.created(service.create(req));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ApiResponse<PrivacyDisposalDto.Response> update(
            @PathVariable Long id, @RequestBody PrivacyDisposalDto.Request req) {
        return ApiResponse.ok(service.update(id, req));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ApiResponse.noContent();
    }
}
