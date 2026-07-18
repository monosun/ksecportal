package com.monosun.secportal.privacy.controller;

import com.monosun.secportal.auth.entity.User;
import com.monosun.secportal.common.response.ApiResponse;
import com.monosun.secportal.privacy.dto.PrivacyDpiaDto;
import com.monosun.secportal.privacy.dto.PrivacyDpiaFileDto;
import com.monosun.secportal.privacy.entity.PrivacyDpiaFile;
import com.monosun.secportal.privacy.service.PrivacyDpiaService;
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
@RequestMapping("/privacy/dpia")
@RequiredArgsConstructor
public class PrivacyDpiaController {

    private final PrivacyDpiaService service;

    @GetMapping
    public ApiResponse<List<PrivacyDpiaDto.Response>> list() {
        return ApiResponse.ok(service.listAll());
    }

    @GetMapping("/{id}")
    public ApiResponse<PrivacyDpiaDto.Response> get(@PathVariable Long id) {
        return ApiResponse.ok(service.get(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ApiResponse<PrivacyDpiaDto.Response> create(@Valid @RequestBody PrivacyDpiaDto.Request req) {
        return ApiResponse.created(service.create(req));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ApiResponse<PrivacyDpiaDto.Response> update(
            @PathVariable Long id, @RequestBody PrivacyDpiaDto.Request req) {
        return ApiResponse.ok(service.update(id, req));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ApiResponse<Void> delete(@PathVariable Long id) throws IOException {
        service.delete(id);
        return ApiResponse.noContent();
    }

    // ── 첨부파일 (영향평가 결과보고서 등) ──────────────────────────

    @GetMapping("/{id}/files")
    public ApiResponse<List<PrivacyDpiaFileDto.Response>> files(@PathVariable Long id) {
        return ApiResponse.ok(service.listFiles(id));
    }

    @PostMapping("/{id}/files")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ApiResponse<PrivacyDpiaFileDto.Response> addFile(
            @PathVariable Long id,
            @RequestPart(value = "data", required = false) PrivacyDpiaFileDto.CreateRequest req,
            @RequestPart("file") MultipartFile file,
            @AuthenticationPrincipal User user) throws IOException {
        return ApiResponse.created(service.addFile(id, req, file, user));
    }

    @DeleteMapping("/files/{fileId}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ApiResponse<Void> deleteFile(@PathVariable Long fileId) throws IOException {
        service.deleteFile(fileId);
        return ApiResponse.noContent();
    }

    @GetMapping("/files/{fileId}/download")
    public ResponseEntity<Resource> downloadFile(@PathVariable Long fileId) {
        PrivacyDpiaFile f = service.getFile(fileId);
        Resource resource = service.downloadFile(fileId);
        String encoded = URLEncoder.encode(f.getFileName() != null ? f.getFileName() : "attachment",
                StandardCharsets.UTF_8).replace("+", "%20");
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encoded)
                .body(resource);
    }
}
