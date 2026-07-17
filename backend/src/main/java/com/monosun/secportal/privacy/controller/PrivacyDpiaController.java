package com.monosun.secportal.privacy.controller;

import com.monosun.secportal.common.response.ApiResponse;
import com.monosun.secportal.privacy.dto.PrivacyDpiaDto;
import com.monosun.secportal.privacy.service.PrivacyDpiaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/privacy/dpia")
@RequiredArgsConstructor
public class PrivacyDpiaController {

    private final PrivacyDpiaService service;

    @GetMapping
    public ApiResponse<List<PrivacyDpiaDto.Response>> list() {
        return ApiResponse.ok(service.listAll());
    }

    @GetMapping("/{id}")
    public ApiResponse<PrivacyDpiaDto.Response> get(@PathVariable Long id) {
        return ApiResponse.ok(service.get(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ApiResponse<PrivacyDpiaDto.Response> create(@Valid @RequestBody PrivacyDpiaDto.Request req) {
        return ApiResponse.created(service.create(req));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ApiResponse<PrivacyDpiaDto.Response> update(
            @PathVariable Long id, @RequestBody PrivacyDpiaDto.Request req) {
        return ApiResponse.ok(service.update(id, req));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ApiResponse.noContent();
    }
}
