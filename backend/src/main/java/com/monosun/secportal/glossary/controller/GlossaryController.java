package com.monosun.secportal.glossary.controller;

import com.monosun.secportal.common.response.ApiResponse;
import com.monosun.secportal.glossary.dto.GlossaryDto;
import com.monosun.secportal.glossary.service.GlossaryBulkService;
import com.monosun.secportal.glossary.service.GlossaryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

/**
 * 보안 용어집 — 조회는 로그인 사용자(보안 가이드 및 자료 &gt; 보안용어집),
 * 등록·수정·삭제는 ADMIN 전용(관리 &gt; 코드 관리 &gt; 용어집).
 */
@RestController
@RequestMapping("/glossary")
@RequiredArgsConstructor
public class GlossaryController {

    private final GlossaryService service;
    private final GlossaryBulkService bulkService;

    @GetMapping
    public ApiResponse<List<GlossaryDto.TermResponse>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String category,
            @RequestParam(required = false, defaultValue = "true") Boolean activeOnly) {
        return ApiResponse.ok(service.list(keyword, category, activeOnly));
    }

    @GetMapping("/summary")
    public ApiResponse<GlossaryDto.SummaryResponse> summary() {
        return ApiResponse.ok(service.summary());
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<GlossaryDto.TermResponse> create(@RequestBody GlossaryDto.TermRequest req) {
        return ApiResponse.created(service.create(req));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<GlossaryDto.TermResponse> update(@PathVariable Long id,
                                                        @RequestBody GlossaryDto.TermRequest req) {
        return ApiResponse.ok(service.update(id, req));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ApiResponse.noContent();
    }

    // ── 엑셀 일괄 등록 (ADMIN) ──────────────────────────────────────────────

    @GetMapping("/bulk/template")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<byte[]> downloadTemplate() throws IOException {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"glossary-template.xlsx\"")
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(bulkService.generateTemplate());
    }

    @PostMapping("/bulk")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<GlossaryDto.BulkResult> bulkUpload(
            @RequestParam("file") MultipartFile file,
            @RequestParam(required = false, defaultValue = "false") boolean overwrite) throws IOException {
        return ApiResponse.ok(bulkService.upload(file, overwrite));
    }
}
