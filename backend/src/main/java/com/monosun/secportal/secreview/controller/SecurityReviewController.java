package com.monosun.secportal.secreview.controller;

import com.monosun.secportal.auth.entity.User;
import com.monosun.secportal.common.response.ApiResponse;
import com.monosun.secportal.secreview.dto.SecurityReviewDto;
import com.monosun.secportal.secreview.service.SecurityReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/** 보안성 심의 — 신규 구축·변경 시스템의 보안 설계 검토 */
@RestController
@RequestMapping("/security-reviews")
@RequiredArgsConstructor
public class SecurityReviewController {

    private final SecurityReviewService service;

    @GetMapping
    public ApiResponse<Page<SecurityReviewDto.Response>> list(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String reviewType,
            @RequestParam(required = false) String keyword,
            @PageableDefault(size = 20) Pageable pageable) {
        return ApiResponse.ok(service.list(status, reviewType, keyword, pageable));
    }

    @GetMapping("/summary")
    public ApiResponse<SecurityReviewDto.Summary> summary() {
        return ApiResponse.ok(service.summary());
    }

    @GetMapping("/{id}")
    public ApiResponse<SecurityReviewDto.Response> get(@PathVariable Long id) {
        return ApiResponse.ok(service.get(id));
    }

    /** 심의 요청 등록 — 설계서 첨부 가능 (요청은 일반 사용자도 가능) */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<SecurityReviewDto.Response> create(
            @Valid @ModelAttribute SecurityReviewDto.CreateRequest request,
            @RequestParam(value = "file", required = false) MultipartFile file,
            @AuthenticationPrincipal User user) throws IOException {
        return ApiResponse.created(service.create(request, file, user));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ApiResponse<SecurityReviewDto.Response> update(
            @PathVariable Long id,
            @RequestBody SecurityReviewDto.UpdateRequest request) {
        return ApiResponse.ok(service.update(id, request));
    }

    /** 심의 결과 등록(승인·조건부승인·반려) */
    @PostMapping("/{id}/decision")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ApiResponse<SecurityReviewDto.Response> decide(
            @PathVariable Long id,
            @RequestBody SecurityReviewDto.DecisionRequest request,
            @AuthenticationPrincipal User user) {
        return ApiResponse.ok(service.decide(id, request, user));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ApiResponse<Void> delete(@PathVariable Long id) throws IOException {
        service.delete(id);
        return ApiResponse.noContent();
    }

    // ── 첨부 ────────────────────────────────────────────────────────────────

    @PostMapping(value = "/{id}/file", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<SecurityReviewDto.Response> uploadFile(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file) throws IOException {
        return ApiResponse.ok(service.uploadFile(id, file));
    }

    @GetMapping("/{id}/file")
    public ResponseEntity<Resource> downloadFile(@PathVariable Long id) {
        Resource resource = service.downloadFile(id);
        String encoded = URLEncoder.encode(service.fileName(id), StandardCharsets.UTF_8).replace("+", "%20");
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encoded)
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(resource);
    }

    // ── 검토 항목 ────────────────────────────────────────────────────────────

    @PostMapping("/{id}/items")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<SecurityReviewDto.ItemResponse> addItem(
            @PathVariable Long id,
            @RequestBody SecurityReviewDto.ItemRequest request) {
        return ApiResponse.created(service.addItem(id, request));
    }

    @PatchMapping("/items/{itemId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ApiResponse<SecurityReviewDto.ItemResponse> updateItem(
            @PathVariable Long itemId,
            @RequestBody SecurityReviewDto.ItemRequest request) {
        return ApiResponse.ok(service.updateItem(itemId, request));
    }

    @DeleteMapping("/items/{itemId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ApiResponse<Void> deleteItem(@PathVariable Long itemId) {
        service.deleteItem(itemId);
        return ApiResponse.noContent();
    }
}
