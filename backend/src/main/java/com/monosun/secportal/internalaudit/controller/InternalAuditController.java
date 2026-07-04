package com.monosun.secportal.internalaudit.controller;

import com.monosun.secportal.auth.entity.User;
import com.monosun.secportal.common.response.ApiResponse;
import com.monosun.secportal.internalaudit.dto.InternalAuditDto;
import com.monosun.secportal.internalaudit.entity.AuditFile;
import com.monosun.secportal.internalaudit.service.InternalAuditService;
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
@RequestMapping("/internal-audit")
@RequiredArgsConstructor
public class InternalAuditController {

    private final InternalAuditService service;

    @GetMapping("/years")
    public ApiResponse<List<Integer>> getYears() {
        return ApiResponse.ok(service.getYears());
    }

    @GetMapping
    public ApiResponse<List<InternalAuditDto.AuditResponse>> list(@RequestParam int year) {
        return ApiResponse.ok(service.listByYear(year));
    }

    @GetMapping("/{id}")
    public ApiResponse<InternalAuditDto.AuditResponse> get(@PathVariable Long id) {
        return ApiResponse.ok(service.get(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ApiResponse<InternalAuditDto.AuditResponse> create(
            @RequestBody InternalAuditDto.AuditRequest req,
            @AuthenticationPrincipal User user) {
        return ApiResponse.ok(service.create(req, user));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ApiResponse<InternalAuditDto.AuditResponse> update(
            @PathVariable Long id,
            @RequestBody InternalAuditDto.AuditRequest req) {
        return ApiResponse.ok(service.update(id, req));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ApiResponse<Void> delete(@PathVariable Long id) throws IOException {
        service.delete(id);
        return ApiResponse.ok(null);
    }

    // ── Targets ──

    @PostMapping("/{auditId}/targets")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ApiResponse<InternalAuditDto.TargetResponse> addTarget(
            @PathVariable Long auditId,
            @RequestBody InternalAuditDto.TargetRequest req) {
        return ApiResponse.ok(service.addTarget(auditId, req));
    }

    @PatchMapping("/targets/{targetId}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ApiResponse<InternalAuditDto.TargetResponse> updateTarget(
            @PathVariable Long targetId,
            @RequestBody InternalAuditDto.TargetRequest req) {
        return ApiResponse.ok(service.updateTarget(targetId, req));
    }

    @DeleteMapping("/targets/{targetId}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ApiResponse<Void> deleteTarget(@PathVariable Long targetId) {
        service.deleteTarget(targetId);
        return ApiResponse.ok(null);
    }

    // ── Items ──

    @PostMapping("/{auditId}/items")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ApiResponse<InternalAuditDto.ItemResponse> addItem(
            @PathVariable Long auditId,
            @RequestBody InternalAuditDto.ItemRequest req) {
        return ApiResponse.ok(service.addItem(auditId, req));
    }

    @PatchMapping("/items/{itemId}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ApiResponse<InternalAuditDto.ItemResponse> updateItem(
            @PathVariable Long itemId,
            @RequestBody InternalAuditDto.ItemRequest req) {
        return ApiResponse.ok(service.updateItem(itemId, req));
    }

    @DeleteMapping("/items/{itemId}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ApiResponse<Void> deleteItem(@PathVariable Long itemId) {
        service.deleteItem(itemId);
        return ApiResponse.ok(null);
    }

    // ── Files ──

    @PostMapping("/{auditId}/files")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ApiResponse<InternalAuditDto.FileResponse> addFile(
            @PathVariable Long auditId,
            @RequestParam("title") String title,
            @RequestPart(value = "file", required = false) MultipartFile file,
            @AuthenticationPrincipal User user) throws IOException {
        return ApiResponse.ok(service.addFile(auditId, title, file, user));
    }

    @DeleteMapping("/files/{fileId}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ApiResponse<Void> deleteFile(@PathVariable Long fileId) throws IOException {
        service.deleteFile(fileId);
        return ApiResponse.ok(null);
    }

    @GetMapping("/files/{fileId}/download")
    public ResponseEntity<Resource> downloadFile(@PathVariable Long fileId) {
        AuditFile af = service.getFile(fileId);
        Resource resource = service.downloadFile(fileId);
        String encoded = URLEncoder.encode(af.getFileName() != null ? af.getFileName() : "file",
                StandardCharsets.UTF_8).replace("+", "%20");
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encoded)
                .body(resource);
    }
}
