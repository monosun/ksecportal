package com.monosun.secportal.secfinding.controller;

import com.monosun.secportal.auth.entity.User;
import com.monosun.secportal.common.response.ApiResponse;
import com.monosun.secportal.secfinding.dto.SecurityFindingDto;
import com.monosun.secportal.secfinding.entity.SecurityFinding;
import com.monosun.secportal.secfinding.service.SecurityFindingService;
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

@RestController
@RequestMapping("/security-findings")
@RequiredArgsConstructor
public class SecurityFindingController {

    private final SecurityFindingService service;

    @GetMapping("/years")
    public ApiResponse<List<Integer>> getYears() {
        return ApiResponse.ok(service.getYears());
    }

    @GetMapping
    public ApiResponse<Page<SecurityFindingDto.Response>> list(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String riskLevel,
            @RequestParam(required = false) String auditType,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(service.list(year, status, riskLevel, auditType, keyword, page, size));
    }

    @GetMapping("/{id}")
    public ApiResponse<SecurityFindingDto.Response> get(@PathVariable Long id) {
        return ApiResponse.ok(service.get(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ApiResponse<SecurityFindingDto.Response> create(
            @RequestPart("data") SecurityFindingDto.Request req,
            @RequestPart(value = "file", required = false) MultipartFile file,
            @AuthenticationPrincipal User user) throws IOException {
        return ApiResponse.ok(service.create(req, file, user));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ApiResponse<SecurityFindingDto.Response> update(
            @PathVariable Long id,
            @RequestPart("data") SecurityFindingDto.Request req,
            @RequestPart(value = "file", required = false) MultipartFile file) throws IOException {
        return ApiResponse.ok(service.update(id, req, file));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ApiResponse<Void> delete(@PathVariable Long id) throws IOException {
        service.delete(id);
        return ApiResponse.ok(null);
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<Resource> download(@PathVariable Long id) {
        SecurityFindingDto.Response meta = service.get(id);
        Resource resource = service.download(id);
        String encoded = URLEncoder.encode(meta.getFileName() != null ? meta.getFileName() : "file",
                StandardCharsets.UTF_8).replace("+", "%20");
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encoded)
                .body(resource);
    }
}
