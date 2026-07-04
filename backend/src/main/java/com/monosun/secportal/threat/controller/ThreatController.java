package com.monosun.secportal.threat.controller;

import com.monosun.secportal.common.response.ApiResponse;
import com.monosun.secportal.threat.dto.ThreatDto;
import com.monosun.secportal.threat.service.ThreatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/threats")
@RequiredArgsConstructor
public class ThreatController {

    private final ThreatService threatService;

    @GetMapping
    public ApiResponse<List<ThreatDto.ThreatResponse>> list() {
        return ApiResponse.ok(threatService.list());
    }

    @GetMapping("/{id}")
    public ApiResponse<ThreatDto.ThreatResponse> get(@PathVariable Long id) {
        return ApiResponse.ok(threatService.get(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ThreatDto.ThreatResponse> create(@Valid @RequestBody ThreatDto.ThreatRequest request) {
        return ApiResponse.created(threatService.create(request));
    }

    @PatchMapping("/{id}")
    public ApiResponse<ThreatDto.ThreatResponse> update(
            @PathVariable Long id,
            @RequestBody ThreatDto.ThreatRequest request) {
        return ApiResponse.ok(threatService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        threatService.delete(id);
        return ApiResponse.noContent();
    }

    @GetMapping("/defaults/check")
    public ApiResponse<ThreatDto.DefaultCheckResponse> checkDefaults() {
        return ApiResponse.ok(threatService.checkDefaults());
    }

    @PostMapping("/defaults")
    public ApiResponse<Map<String, Integer>> loadDefaults() {
        int loaded = threatService.loadDefaults();
        return ApiResponse.ok(Map.of("loaded", loaded));
    }

    @GetMapping("/defaults")
    public ApiResponse<ThreatDto.DefaultListResponse> listDefaults(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(threatService.listDefaults(page, size));
    }

    @PostMapping("/defaults/item")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<ThreatDto.DefaultResponse> createDefault(
            @Valid @RequestBody ThreatDto.DefaultRequest request) {
        return ApiResponse.created(threatService.createDefault(request));
    }

    @PatchMapping("/defaults/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<ThreatDto.DefaultResponse> updateDefault(
            @PathVariable Long id,
            @RequestBody ThreatDto.DefaultRequest request) {
        return ApiResponse.ok(threatService.updateDefault(id, request));
    }

    @DeleteMapping("/defaults/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> deleteDefault(@PathVariable Long id) {
        threatService.deleteDefault(id);
        return ApiResponse.noContent();
    }

    @PostMapping("/reset")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ApiResponse<Map<String, Integer>> resetToDefaults() {
        int loaded = threatService.resetToDefaults();
        return ApiResponse.ok(Map.of("loaded", loaded));
    }
}
