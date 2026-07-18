package com.monosun.secportal.privacy.controller;

import com.monosun.secportal.auth.entity.User;
import com.monosun.secportal.common.response.ApiResponse;
import com.monosun.secportal.privacy.dto.PrivacyConsentDto;
import com.monosun.secportal.privacy.dto.PrivacyConsentVersionDto;
import com.monosun.secportal.privacy.entity.PrivacyConsentVersion;
import com.monosun.secportal.privacy.service.PrivacyConsentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/privacy/consents")
@RequiredArgsConstructor
public class PrivacyConsentController {

    private final PrivacyConsentService service;

    @GetMapping
    public ApiResponse<List<PrivacyConsentDto.Response>> list() {
        return ApiResponse.ok(service.listAll());
    }

    @GetMapping("/{id}")
    public ApiResponse<PrivacyConsentDto.Response> get(@PathVariable Long id) {
        return ApiResponse.ok(service.get(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ApiResponse<PrivacyConsentDto.Response> create(@Valid @RequestBody PrivacyConsentDto.Request req) {
        return ApiResponse.created(service.create(req));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ApiResponse<PrivacyConsentDto.Response> update(
            @PathVariable Long id, @RequestBody PrivacyConsentDto.Request req) {
        return ApiResponse.ok(service.update(id, req));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ApiResponse<Void> delete(@PathVariable Long id) throws IOException {
        service.delete(id);
        return ApiResponse.noContent();
    }

    // ── 버전 이력 ─────────────────────────────────────────────────

    @GetMapping("/{id}/versions")
    public ApiResponse<List<PrivacyConsentVersionDto.Response>> versions(@PathVariable Long id) {
        return ApiResponse.ok(service.listVersions(id));
    }

    /** 새 버전 이력 등록 (변경사유 + 첨부파일) */
    @PostMapping("/{id}/versions")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ApiResponse<PrivacyConsentVersionDto.Response> addVersion(
            @PathVariable Long id,
            @RequestPart("data") PrivacyConsentVersionDto.CreateRequest req,
            @RequestPart(value = "file", required = false) MultipartFile file,
            @AuthenticationPrincipal User user) throws IOException {
        return ApiResponse.created(service.addVersion(id, req, file, user));
    }

    @DeleteMapping("/versions/{versionId}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ApiResponse<Void> deleteVersion(@PathVariable Long versionId) throws IOException {
        service.deleteVersion(versionId);
        return ApiResponse.noContent();
    }

    @GetMapping("/versions/{versionId}/download")
    public ResponseEntity<Resource> downloadVersionFile(@PathVariable Long versionId) {
        PrivacyConsentVersion v = service.getVersion(versionId);
        Resource resource = service.downloadVersionFile(versionId);
        String encoded = URLEncoder.encode(v.getFileName() != null ? v.getFileName() : "attachment",
                StandardCharsets.UTF_8).replace("+", "%20");
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encoded)
                .body(resource);
    }
}
