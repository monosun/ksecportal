package com.monosun.secportal.sourcescan.controller;

import com.monosun.secportal.common.response.ApiResponse;
import com.monosun.secportal.sourcescan.dto.SourceScanDto;
import com.monosun.secportal.sourcescan.service.SourceScanService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class SourceScanController {

    private final SourceScanService sourceScanService;

    // ── GitHub 연동 설정 (ADMIN 전용 — 토큰은 마스킹 응답) ────────────────────

    @GetMapping("/admin/github-config")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<SourceScanDto.ConfigResponse> getConfig() {
        return ApiResponse.ok(sourceScanService.getConfig());
    }

    @PutMapping("/admin/github-config")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<SourceScanDto.ConfigResponse> saveConfig(@RequestBody SourceScanDto.ConfigRequest request) {
        return ApiResponse.ok(sourceScanService.saveConfig(request));
    }

    @PostMapping("/admin/github-config/test")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<SourceScanDto.TestResult> testConnection() {
        return ApiResponse.ok(sourceScanService.testConnection());
    }

    // ── 소스 취약점 점검 ─────────────────────────────────────────────────────

    @GetMapping("/source-scan/repos")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ApiResponse<List<SourceScanDto.RepoResponse>> listRepos() {
        return ApiResponse.ok(sourceScanService.listRepos());
    }

    @PostMapping("/source-scan/run")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ApiResponse<SourceScanDto.ScanDetailResponse> run(@Valid @RequestBody SourceScanDto.ScanRequest request) {
        return ApiResponse.ok(sourceScanService.runScan(request.getRepository()));
    }

    @GetMapping("/source-scan/scans")
    public ApiResponse<Page<SourceScanDto.ScanResponse>> listScans(
            @PageableDefault(size = 20) Pageable pageable) {
        return ApiResponse.ok(sourceScanService.listScans(pageable));
    }

    @GetMapping("/source-scan/scans/{id}")
    public ApiResponse<SourceScanDto.ScanDetailResponse> getScan(@PathVariable Long id) {
        return ApiResponse.ok(sourceScanService.getScan(id));
    }

    @DeleteMapping("/source-scan/scans/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ApiResponse<Void> deleteScan(@PathVariable Long id) {
        sourceScanService.deleteScan(id);
        return ApiResponse.noContent();
    }
}
