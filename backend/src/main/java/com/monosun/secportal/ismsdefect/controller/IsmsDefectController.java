package com.monosun.secportal.ismsdefect.controller;

import com.monosun.secportal.auth.entity.User;
import com.monosun.secportal.common.response.ApiResponse;
import com.monosun.secportal.ismsdefect.dto.IsmsDefectDto;
import com.monosun.secportal.ismsdefect.entity.IsmsDefect;
import com.monosun.secportal.ismsdefect.entity.IsmsDefectReport;
import com.monosun.secportal.ismsdefect.service.IsmsDefectService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

/**
 * ISMS 결함관리 — 연도별 결함 내역과 연도별 결함 조치 보고서.
 *
 * 증적관리(/isms)와 같은 "연도 단위" 운영 모델을 따른다.
 */
@RestController
@RequestMapping("/isms-defects")
@RequiredArgsConstructor
public class IsmsDefectController {

    private final IsmsDefectService service;

    // ── 결함 ────────────────────────────────────────────────────────────────

    @GetMapping
    public ApiResponse<List<IsmsDefectDto.DefectResponse>> list(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) String auditType,
            @RequestParam(required = false) String defectType,
            @RequestParam(required = false) String severity,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword) {
        return ApiResponse.ok(service.list(resolveYear(year), auditType, defectType, severity, status, keyword));
    }

    @GetMapping("/years")
    public ApiResponse<List<Integer>> years() {
        return ApiResponse.ok(service.years());
    }

    @GetMapping("/summary")
    public ApiResponse<IsmsDefectDto.SummaryResponse> summary(@RequestParam(required = false) Integer year) {
        return ApiResponse.ok(service.summary(resolveYear(year)));
    }

    @GetMapping("/{id}")
    public ApiResponse<IsmsDefectDto.DefectResponse> get(@PathVariable Long id) {
        return ApiResponse.ok(service.get(id));
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ApiResponse<IsmsDefectDto.DefectResponse> create(
            @RequestPart("data") IsmsDefectDto.DefectRequest data,
            @RequestPart(value = "file", required = false) MultipartFile file,
            @AuthenticationPrincipal User user) throws IOException {
        return ApiResponse.created(service.create(data, file, user));
    }

    @PatchMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ApiResponse<IsmsDefectDto.DefectResponse> update(
            @PathVariable Long id,
            @RequestPart("data") IsmsDefectDto.DefectRequest data,
            @RequestPart(value = "file", required = false) MultipartFile file) throws IOException {
        return ApiResponse.ok(service.update(id, data, file));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ApiResponse<Void> delete(@PathVariable Long id) throws IOException {
        service.delete(id);
        return ApiResponse.noContent();
    }

    @GetMapping("/{id}/file")
    public ResponseEntity<Resource> downloadDefectFile(@PathVariable Long id) {
        Resource resource = service.downloadDefectFile(id);
        IsmsDefect d = service.getDefectEntity(id);
        return fileResponse(d.getFileName(), resource);
    }

    // ── 연도별 보고서 ────────────────────────────────────────────────────────

    @GetMapping("/report")
    public ApiResponse<IsmsDefectDto.ReportResponse> getReport(@RequestParam(required = false) Integer year) {
        return ApiResponse.ok(service.getReport(resolveYear(year)));
    }

    @PutMapping(value = "/report", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ApiResponse<IsmsDefectDto.ReportResponse> saveReport(
            @RequestParam(required = false) Integer year,
            @RequestPart("data") IsmsDefectDto.ReportRequest data,
            @RequestPart(value = "file", required = false) MultipartFile file,
            @AuthenticationPrincipal User user) throws IOException {
        return ApiResponse.ok(service.saveReport(resolveYear(year), data, file, user));
    }

    @DeleteMapping("/report")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ApiResponse<Void> deleteReport(@RequestParam(required = false) Integer year) throws IOException {
        service.deleteReport(resolveYear(year));
        return ApiResponse.noContent();
    }

    @GetMapping("/report/file")
    public ResponseEntity<Resource> downloadReportFile(@RequestParam(required = false) Integer year) {
        int y = resolveYear(year);
        Resource resource = service.downloadReportFile(y);
        IsmsDefectReport r = service.getReportEntity(y);
        return fileResponse(r.getFileName(), resource);
    }

    // ── 내보내기 ────────────────────────────────────────────────────────────

    @GetMapping("/export/csv")
    public ResponseEntity<byte[]> exportCsv(@RequestParam(required = false) Integer year) {
        int y = resolveYear(year);
        byte[] csv = service.exportCsv(y);
        String filename = "ISMS_결함관리_" + y + ".csv";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encode(filename))
                .contentType(MediaType.parseMediaType("text/csv;charset=UTF-8"))
                .body(csv);
    }

    private static int resolveYear(Integer year) {
        return year != null && year > 0 ? year : LocalDate.now().getYear();
    }

    private static ResponseEntity<Resource> fileResponse(String fileName, Resource resource) {
        String displayName = fileName != null ? fileName : "attachment";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encode(displayName))
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(resource);
    }

    private static String encode(String name) {
        return URLEncoder.encode(name, StandardCharsets.UTF_8).replace("+", "%20");
    }
}
