package com.monosun.secportal.incident.controller;

import com.monosun.secportal.asset.dto.AssetBulkUploadResult;
import com.monosun.secportal.auth.entity.User;
import com.monosun.secportal.common.response.ApiResponse;
import com.monosun.secportal.incident.dto.IncidentDto;
import com.monosun.secportal.incident.entity.Incident;
import com.monosun.secportal.incident.service.IncidentBulkService;
import com.monosun.secportal.incident.service.IncidentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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

@RestController
@RequestMapping("/incidents")
@RequiredArgsConstructor
public class IncidentController {

    private final IncidentService incidentService;
    private final IncidentBulkService incidentBulkService;

    @GetMapping
    public ApiResponse<Page<IncidentDto.Response>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Incident.Severity severity,
            @RequestParam(required = false) Incident.Status status,
            @RequestParam(required = false) Incident.IncidentType type,
            @PageableDefault(size = 20) Pageable pageable) {
        return ApiResponse.ok(incidentService.list(keyword, severity, status, type, pageable));
    }

    @GetMapping("/{id}")
    public ApiResponse<IncidentDto.Response> get(@PathVariable Long id) {
        return ApiResponse.ok(incidentService.get(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<IncidentDto.Response> create(
            @Valid @RequestBody IncidentDto.CreateRequest request,
            @AuthenticationPrincipal User user) {
        return ApiResponse.created(incidentService.create(request, user.getId()));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ApiResponse<IncidentDto.Response> update(
            @PathVariable Long id,
            @RequestBody IncidentDto.UpdateRequest request) {
        return ApiResponse.ok(incidentService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        incidentService.delete(id);
        return ApiResponse.noContent();
    }

    @GetMapping("/bulk/template")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<byte[]> downloadTemplate() throws IOException {
        byte[] data = incidentBulkService.generateTemplate();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"incident-upload-template.xlsx\"")
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(data);
    }

    @PostMapping("/bulk")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ApiResponse<AssetBulkUploadResult> bulkUpload(
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal User user) throws IOException {
        return ApiResponse.ok(incidentBulkService.upload(file, user));
    }
}
