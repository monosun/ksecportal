package com.monosun.secportal.errorlog.controller;

import com.monosun.secportal.audit.service.AuditLogService;
import com.monosun.secportal.auth.entity.User;
import com.monosun.secportal.common.excel.ExportSupport;
import com.monosun.secportal.common.response.ApiResponse;
import com.monosun.secportal.errorlog.dto.ErrorLogDto;
import com.monosun.secportal.errorlog.entity.ErrorLog;
import com.monosun.secportal.errorlog.service.ErrorLogService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** 관리 &gt; 에러 로그 관리 */
@RestController
@RequestMapping("/admin/error-logs")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class ErrorLogController {

    private final ErrorLogService errorLogService;
    private final AuditLogService auditLogService;

    @GetMapping
    public ApiResponse<Page<ErrorLogDto.Response>> list(
            @RequestParam(required = false) ErrorLog.Level level,
            @RequestParam(required = false) ErrorLog.Source source,
            @RequestParam(required = false) ErrorLog.Status status,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateTo,
            @PageableDefault(size = 50) Pageable pageable) {
        return ApiResponse.ok(errorLogService.list(level, source, status, keyword, dateFrom, dateTo, pageable));
    }

    @GetMapping("/stats")
    public ApiResponse<ErrorLogDto.Stats> stats() {
        return ApiResponse.ok(errorLogService.stats());
    }

    @GetMapping("/{id}")
    public ApiResponse<ErrorLogDto.Detail> get(@PathVariable Long id) {
        return ApiResponse.ok(errorLogService.get(id));
    }

    @PatchMapping("/{id}")
    public ApiResponse<ErrorLogDto.Response> handle(@PathVariable Long id,
                                                    @Valid @RequestBody ErrorLogDto.HandleRequest req,
                                                    @AuthenticationPrincipal User user) {
        ErrorLogDto.Response updated = errorLogService.handle(id, req, user);
        auditLogService.log("ERROR_LOG_STATUS_CHANGED", "ERROR_LOG", id, "처리 상태: " + updated.getStatus());
        return ApiResponse.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        errorLogService.delete(id);
        auditLogService.log("ERROR_LOG_DELETED", "ERROR_LOG", id, null);
        return ApiResponse.noContent();
    }

    /** 오래된 로그 정리 — days=0 이면 전체 삭제 */
    @DeleteMapping
    public ApiResponse<Long> purge(@RequestParam(defaultValue = "90") int days) {
        long removed = errorLogService.purgeOlderThan(days);
        auditLogService.log("ERROR_LOG_PURGED", "ERROR_LOG", null, days + "일 이전 " + removed + "건 삭제");
        return ApiResponse.ok(removed + "건을 삭제했습니다.", removed);
    }

    /** 처리완료·무시 상태의 로그 정리 */
    @DeleteMapping("/handled")
    public ApiResponse<Long> purgeHandled() {
        long removed = errorLogService.purgeHandled();
        auditLogService.log("ERROR_LOG_PURGED", "ERROR_LOG", null, "처리완료·무시 " + removed + "건 삭제");
        return ApiResponse.ok(removed + "건을 삭제했습니다.", removed);
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> export(
            @RequestParam(required = false) ErrorLog.Level level,
            @RequestParam(required = false) ErrorLog.Source source,
            @RequestParam(required = false) ErrorLog.Status status,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateTo)
            throws IOException {
        byte[] data = errorLogService.exportExcel(level, source, status, keyword, dateFrom, dateTo);
        return ExportSupport.xlsx(data, "에러로그_" + LocalDate.now() + ".xlsx");
    }
}
