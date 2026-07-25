package com.monosun.secportal.opstatus.controller;

import com.monosun.secportal.common.response.ApiResponse;
import com.monosun.secportal.opstatus.dto.OperationStatusDto;
import com.monosun.secportal.opstatus.service.OperationStatusDefaultService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 운영현황관리 기본 항목 마스터 — 관리 &gt; 코드 관리에서 관리한다.
 *
 * 조회는 MANAGER 이상(운영현황 화면의 항목 추가에서 선택 목록으로 쓰기 때문),
 * 등록·수정·삭제는 ADMIN 전용으로 다른 기본항목 마스터(월간 점검·수탁사)와 동일하게 맞춘다.
 */
@RestController
@RequestMapping("/operation-status/default-items")
@RequiredArgsConstructor
public class OperationStatusDefaultController {

    private final OperationStatusDefaultService service;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ApiResponse<List<OperationStatusDto.DefaultResponse>> list(
            @RequestParam(required = false) String type,
            @RequestParam(required = false, defaultValue = "false") Boolean activeOnly) {
        return ApiResponse.ok(service.list(type, activeOnly));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<OperationStatusDto.DefaultResponse> create(
            @RequestBody OperationStatusDto.DefaultRequest req) {
        return ApiResponse.created(service.create(req));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<OperationStatusDto.DefaultResponse> update(
            @PathVariable Long id, @RequestBody OperationStatusDto.DefaultRequest req) {
        return ApiResponse.ok(service.update(id, req));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ApiResponse.noContent();
    }
}
