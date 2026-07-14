package com.monosun.secportal.privacy.controller;

import com.monosun.secportal.auth.entity.User;
import com.monosun.secportal.common.response.ApiResponse;
import com.monosun.secportal.privacy.dto.ContractorDto;
import com.monosun.secportal.privacy.entity.ContractorInspectionFile;
import com.monosun.secportal.privacy.service.ContractorService;
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
@RequestMapping("/privacy/contractors")
@RequiredArgsConstructor
public class ContractorController {

    private final ContractorService service;

    // ── Contractors ───────────────────────────────────────────────────

    @GetMapping
    public ApiResponse<List<ContractorDto.ContractorResponse>> list() {
        return ApiResponse.ok(service.listAll());
    }

    @GetMapping("/{id}")
    public ApiResponse<ContractorDto.ContractorResponse> get(@PathVariable Long id) {
        return ApiResponse.ok(service.getDetail(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ApiResponse<ContractorDto.ContractorResponse> create(
            @RequestBody ContractorDto.ContractorRequest req) {
        return ApiResponse.ok(service.create(req));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ApiResponse<ContractorDto.ContractorResponse> update(
            @PathVariable Long id,
            @RequestBody ContractorDto.ContractorRequest req) {
        return ApiResponse.ok(service.update(id, req));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ApiResponse<Void> delete(@PathVariable Long id) throws IOException {
        service.delete(id);
        return ApiResponse.ok(null);
    }

    // ── 개인정보처리방침 일괄등록 ────────────────────────────────────────

    /** 개인정보처리방침 URL을 읽어 수탁사·위탁업무·재수탁사를 추출한다 (저장하지 않고 미리보기만). */
    @PostMapping("/parse-policy")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ApiResponse<ContractorDto.PolicyParseResponse> parsePolicy(
            @Valid @RequestBody ContractorDto.PolicyImportRequest req) {
        return ApiResponse.ok(service.parseFromPolicy(req.getUrl()));
    }

    /** 팝업에서 확인·수정한 수탁사 목록을 일괄 등록한다. */
    @PostMapping("/bulk")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ApiResponse<ContractorDto.BulkCreateResponse> bulkCreate(
            @RequestBody ContractorDto.BulkCreateRequest req) {
        return ApiResponse.ok(service.bulkCreate(req.getItems()));
    }

    // ── Inspections ───────────────────────────────────────────────────

    @GetMapping("/{contractorId}/inspections")
    public ApiResponse<List<ContractorDto.InspectionResponse>> listInspections(@PathVariable Long contractorId) {
        return ApiResponse.ok(service.listInspections(contractorId));
    }

    @PostMapping("/{contractorId}/inspections")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ApiResponse<ContractorDto.InspectionResponse> createInspection(
            @PathVariable Long contractorId,
            @RequestBody ContractorDto.InspectionRequest req) {
        return ApiResponse.ok(service.createInspection(contractorId, req));
    }

    @PatchMapping("/{contractorId}/inspections/{inspectionId}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ApiResponse<ContractorDto.InspectionResponse> updateInspection(
            @PathVariable Long contractorId,
            @PathVariable Long inspectionId,
            @RequestBody ContractorDto.InspectionRequest req) {
        return ApiResponse.ok(service.updateInspection(contractorId, inspectionId, req));
    }

    @DeleteMapping("/{contractorId}/inspections/{inspectionId}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ApiResponse<Void> deleteInspection(
            @PathVariable Long contractorId,
            @PathVariable Long inspectionId) throws IOException {
        service.deleteInspection(contractorId, inspectionId);
        return ApiResponse.ok(null);
    }

    // ── Inspection Files ──────────────────────────────────────────────

    @PostMapping("/inspections/{inspectionId}/files")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ApiResponse<ContractorDto.FileResponse> addFile(
            @PathVariable Long inspectionId,
            @RequestParam("title") String title,
            @RequestPart(value = "file", required = false) MultipartFile file,
            @AuthenticationPrincipal User user) throws IOException {
        return ApiResponse.ok(service.addFile(inspectionId, title, file, user));
    }

    @DeleteMapping("/inspections/files/{fileId}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ApiResponse<Void> deleteFile(@PathVariable Long fileId) throws IOException {
        service.deleteFile(fileId);
        return ApiResponse.ok(null);
    }

    @GetMapping("/inspections/files/{fileId}/download")
    public ResponseEntity<Resource> downloadFile(@PathVariable Long fileId) {
        ContractorInspectionFile f = service.getFile(fileId);
        Resource resource = service.downloadFile(fileId);
        String encoded = URLEncoder.encode(
                f.getFileName() != null ? f.getFileName() : "file",
                StandardCharsets.UTF_8).replace("+", "%20");
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encoded)
                .body(resource);
    }
}
