package com.monosun.secportal.opstatus.controller;

import com.monosun.secportal.common.response.ApiResponse;
import com.monosun.secportal.opstatus.dto.OperationStatusDto;
import com.monosun.secportal.opstatus.service.OperationStatusService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 운영현황관리 — 연도별 정보보호/개인정보보호 관리체계 운영 점검 현황.
 * 조회는 로그인 사용자, 쓰기는 MANAGER 이상.
 */
@RestController
@RequestMapping("/operation-status")
@RequiredArgsConstructor
public class OperationStatusController {

    private final OperationStatusService service;

    @GetMapping("/years")
    public ApiResponse<List<Integer>> years() {
        return ApiResponse.ok(service.years());
    }

    @GetMapping
    public ApiResponse<List<OperationStatusDto.ItemResponse>> list(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) String type) {
        return ApiResponse.ok(service.list(resolveYear(year), type));
    }

    @GetMapping("/summary")
    public ApiResponse<OperationStatusDto.SummaryResponse> summary(@RequestParam(required = false) Integer year) {
        return ApiResponse.ok(service.summary(resolveYear(year)));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ApiResponse<OperationStatusDto.ItemResponse> create(@RequestBody OperationStatusDto.ItemRequest req) {
        return ApiResponse.created(service.create(req));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ApiResponse<OperationStatusDto.ItemResponse> update(@PathVariable Long id,
                                                               @RequestBody OperationStatusDto.ItemRequest req) {
        return ApiResponse.ok(service.update(id, req));
    }

    @PatchMapping("/{id}/month")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ApiResponse<OperationStatusDto.ItemResponse> toggleMonth(
            @PathVariable Long id, @RequestBody OperationStatusDto.MonthToggleRequest req) {
        return ApiResponse.ok(service.toggleMonth(id, req));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ApiResponse.noContent();
    }

    @PostMapping("/defaults")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ApiResponse<Map<String, Object>> loadDefaults(@RequestParam(required = false) Integer year,
                                                          @RequestParam(defaultValue = "ISMS") String type) {
        int y = resolveYear(year);
        int created = service.loadDefaults(y, type);
        return ApiResponse.ok(Map.of("year", y, "type", type.toUpperCase(), "created", created));
    }

    @PostMapping("/copy")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ApiResponse<Map<String, Object>> copy(@RequestParam int fromYear,
                                                 @RequestParam int toYear,
                                                 @RequestParam(defaultValue = "ISMS") String type) {
        int copied = service.copyFromYear(fromYear, toYear, type);
        return ApiResponse.ok(Map.of("fromYear", fromYear, "toYear", toYear,
                "type", type.toUpperCase(), "copied", copied));
    }

    @DeleteMapping
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ApiResponse<Map<String, Object>> clear(@RequestParam(required = false) Integer year,
                                                  @RequestParam(defaultValue = "ISMS") String type) {
        int y = resolveYear(year);
        int deleted = service.clear(y, type);
        return ApiResponse.ok(Map.of("year", y, "type", type.toUpperCase(), "deleted", deleted));
    }

    private int resolveYear(Integer year) {
        return year != null ? year : LocalDate.now().getYear();
    }
}
