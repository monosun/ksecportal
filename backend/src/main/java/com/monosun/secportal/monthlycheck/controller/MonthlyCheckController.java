package com.monosun.secportal.monthlycheck.controller;

import com.monosun.secportal.auth.entity.User;
import com.monosun.secportal.common.response.ApiResponse;
import com.monosun.secportal.monthlycheck.dto.MonthlyCheckDto;
import com.monosun.secportal.monthlycheck.entity.MonthlyCheckEvidence;
import com.monosun.secportal.monthlycheck.service.MonthlyCheckService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/monthly-checks")
@RequiredArgsConstructor
public class MonthlyCheckController {

    private final MonthlyCheckService service;

    // ── 항목 ──────────────────────────────────────────────────────────────

    @GetMapping
    public ApiResponse<List<MonthlyCheckDto.CheckItemResponse>> list(@RequestParam String yearMonth) {
        return ApiResponse.ok(service.listByMonth(yearMonth));
    }

    @GetMapping("/months")
    public ApiResponse<List<String>> availableMonths() {
        return ApiResponse.ok(service.listAvailableMonths());
    }

    @GetMapping("/summary")
    public ApiResponse<MonthlyCheckDto.SummaryResponse> summary(@RequestParam String yearMonth) {
        return ApiResponse.ok(service.summary(yearMonth));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<MonthlyCheckDto.CheckItemResponse> create(
            @Valid @RequestBody MonthlyCheckDto.CreateRequest request,
            @AuthenticationPrincipal User user) {
        return ApiResponse.created(service.create(request, user));
    }

    @DeleteMapping
    public ApiResponse<Void> clearAll(@RequestParam String yearMonth) {
        service.clearByYearMonth(yearMonth);
        return ApiResponse.noContent();
    }

    @GetMapping("/defaults/check")
    public ApiResponse<Boolean> checkDefaults(@RequestParam String yearMonth) {
        return ApiResponse.ok(service.hasAllDefaults(yearMonth));
    }

    @PostMapping("/defaults")
    public ApiResponse<List<MonthlyCheckDto.CheckItemResponse>> loadDefaults(
            @RequestParam String yearMonth,
            @AuthenticationPrincipal User user) {
        return ApiResponse.ok(service.loadDefaults(yearMonth, user));
    }

    /** 대상 월 이전에 점검 내역이 있는 가장 최근 월(없으면 data=null) */
    @GetMapping("/previous-month")
    public ApiResponse<String> previousMonth(@RequestParam String yearMonth) {
        return ApiResponse.ok("ok", service.findPreviousMonthWithItems(yearMonth));
    }

    /** 이전 점검 월의 항목 구성을 대상 월로 복사 */
    @PostMapping("/copy-previous")
    public ApiResponse<List<MonthlyCheckDto.CheckItemResponse>> copyPrevious(
            @RequestParam String yearMonth,
            @AuthenticationPrincipal User user) {
        return ApiResponse.ok(service.copyFromPreviousMonth(yearMonth, user));
    }

    @PatchMapping("/{id}")
    public ApiResponse<MonthlyCheckDto.CheckItemResponse> update(
            @PathVariable Long id,
            @RequestBody MonthlyCheckDto.UpdateRequest request) {
        return ApiResponse.ok(service.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) throws IOException {
        service.delete(id);
        return ApiResponse.noContent();
    }

    // ── 증적 ──────────────────────────────────────────────────────────────

    @GetMapping("/{itemId}/evidences")
    public ApiResponse<List<MonthlyCheckDto.EvidenceResponse>> listEvidences(@PathVariable Long itemId) {
        return ApiResponse.ok(service.listEvidences(itemId));
    }

    @PostMapping(value = "/{itemId}/evidences", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<MonthlyCheckDto.EvidenceResponse> createEvidence(
            @PathVariable Long itemId,
            @Valid @ModelAttribute MonthlyCheckDto.EvidenceCreateRequest request,
            @RequestParam(value = "file", required = false) MultipartFile file,
            @AuthenticationPrincipal User user) throws IOException {
        return ApiResponse.created(service.createEvidence(itemId, request, file, user));
    }

    @PatchMapping(value = "/evidences/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<MonthlyCheckDto.EvidenceResponse> updateEvidence(
            @PathVariable Long id,
            @ModelAttribute MonthlyCheckDto.EvidenceUpdateRequest request,
            @RequestParam(value = "file", required = false) MultipartFile file) throws IOException {
        return ApiResponse.ok(service.updateEvidence(id, request, file));
    }

    @DeleteMapping("/evidences/{id}")
    public ApiResponse<Void> deleteEvidence(@PathVariable Long id) throws IOException {
        service.deleteEvidence(id);
        return ApiResponse.noContent();
    }

    @GetMapping("/evidences/{id}/file")
    public ResponseEntity<Resource> downloadFile(@PathVariable Long id) {
        Resource resource = service.downloadEvidenceFile(id);
        MonthlyCheckEvidence evidence = service.getEvidence(id);
        String displayName = evidence.getFileName() != null ? evidence.getFileName() : "attachment";
        String encoded = URLEncoder.encode(displayName, StandardCharsets.UTF_8).replace("+", "%20");
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encoded)
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(resource);
    }
}
