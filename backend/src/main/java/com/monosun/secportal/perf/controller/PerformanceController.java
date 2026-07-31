package com.monosun.secportal.perf.controller;

import com.monosun.secportal.common.response.ApiResponse;
import com.monosun.secportal.perf.dto.PerformanceDto;
import com.monosun.secportal.perf.service.PerformanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/admin/performance")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class PerformanceController {

    private final PerformanceService performanceService;

    @GetMapping("/logs")
    public ApiResponse<Page<PerformanceDto.LogResponse>> list(
            @RequestParam(required = false) String logType,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long minMs,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateTo,
            @PageableDefault(size = 50) Pageable pageable) {
        return ApiResponse.ok(performanceService.list(logType, keyword, minMs, dateFrom, dateTo, pageable));
    }

    @GetMapping("/stats")
    public ApiResponse<PerformanceDto.Stats> stats() {
        return ApiResponse.ok(performanceService.stats());
    }

    @GetMapping("/config")
    public ApiResponse<PerformanceDto.Config> getConfig() {
        return ApiResponse.ok(performanceService.getConfig());
    }

    @PutMapping("/config")
    public ApiResponse<PerformanceDto.Config> updateConfig(@RequestBody PerformanceDto.ConfigRequest req) {
        return ApiResponse.ok(performanceService.updateConfig(req));
    }

    /** days 를 주면 그보다 오래된 기록만, 없으면 전체 삭제 */
    @DeleteMapping("/logs")
    public ApiResponse<Integer> purge(@RequestParam(required = false) Integer days) {
        return ApiResponse.ok(performanceService.purge(days));
    }
}
