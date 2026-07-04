package com.monosun.secportal.secdoc.controller;

import com.monosun.secportal.auth.entity.User;
import com.monosun.secportal.common.response.ApiResponse;
import com.monosun.secportal.secdoc.dto.SecDocDto;
import com.monosun.secportal.secdoc.service.SecDocService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
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
import java.util.Map;

@RestController
@RequestMapping("/sec-docs")
@RequiredArgsConstructor
public class SecDocController {

    private final SecDocService service;

    @GetMapping
    public ApiResponse<Page<SecDocDto.Response>> list(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String searchField,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(service.list(category, keyword, searchField, page, size));
    }

    @GetMapping("/{id}")
    public ApiResponse<SecDocDto.Response> get(@PathVariable Long id) {
        return ApiResponse.ok(service.get(id));
    }

    @GetMapping("/{id}/versions")
    public ApiResponse<List<SecDocDto.Response>> versions(@PathVariable Long id) {
        return ApiResponse.ok(service.getVersions(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ApiResponse<SecDocDto.Response> create(
            @RequestPart("data") SecDocDto.CreateRequest req,
            @RequestPart(value = "file", required = false) MultipartFile file,
            @AuthenticationPrincipal User user) throws IOException {
        return ApiResponse.ok(service.create(req, file, user));
    }

    @PostMapping("/{id}/versions")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ApiResponse<SecDocDto.Response> addVersion(
            @PathVariable Long id,
            @RequestPart("data") SecDocDto.NewVersionRequest req,
            @RequestPart(value = "file", required = false) MultipartFile file,
            @AuthenticationPrincipal User user) throws IOException {
        return ApiResponse.ok(service.addVersion(id, req, file, user));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ApiResponse<SecDocDto.Response> update(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        return ApiResponse.ok(service.updateMeta(id, body.get("title"), body.get("description"), body.get("producingOrg")));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ApiResponse<Void> delete(@PathVariable Long id) throws IOException {
        service.delete(id);
        return ApiResponse.ok(null);
    }

    @DeleteMapping("/{id}/version")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ApiResponse<Void> deleteVersion(@PathVariable Long id) throws IOException {
        service.deleteVersion(id);
        return ApiResponse.ok(null);
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<Resource> download(@PathVariable Long id) {
        SecDocDto.Response meta = service.get(id);
        Resource resource = service.download(id);
        String encoded = URLEncoder.encode(meta.getFileName() != null ? meta.getFileName() : "document",
                StandardCharsets.UTF_8).replace("+", "%20");
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encoded)
                .body(resource);
    }
}
