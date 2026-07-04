package com.monosun.secportal.code.controller;

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

    // ── 공개 API (인증된 모든 사용자) ──────────────────────────────────────

    @GetMapping("/codes/{groupCode}")
    public ApiResponse<List<CodeDto.SimpleValue>> getActiveValues(@PathVariable String groupCode) {
        return ApiResponse.ok(codeService.listActiveValues(groupCode));
    }

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
