package com.monosun.secportal.sbom.controller;

import com.monosun.secportal.common.response.ApiResponse;
import com.monosun.secportal.sbom.dto.SbomBulkUploadResult;
import com.monosun.secportal.sbom.dto.SbomDto;
import com.monosun.secportal.sbom.service.SbomBulkService;
import com.monosun.secportal.sbom.service.SbomCycloneDxService;
import com.monosun.secportal.sbom.service.SbomService;
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
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/sbom")
@RequiredArgsConstructor
public class SbomController {

    private final SbomService sbomService;
    private final SbomBulkService sbomBulkService;
    private final SbomCycloneDxService sbomCycloneDxService;

    @GetMapping("/software")
    public ApiResponse<Page<SbomDto.Response>> list(
            @RequestParam(required = false) String keyword,
            @PageableDefault(size = 20) Pageable pageable) {
        return ApiResponse.ok(sbomService.list(keyword, pageable));
    }

    @GetMapping("/software/all")
    public ApiResponse<List<SbomDto.SimpleResponse>> listAll() {
        return ApiResponse.ok(sbomService.listAll());
    }

    @GetMapping("/software/{id}")
    public ApiResponse<SbomDto.DetailResponse> get(@PathVariable Long id) {
        return ApiResponse.ok(sbomService.get(id));
    }

    @PostMapping("/software")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ApiResponse<SbomDto.DetailResponse> create(@Valid @RequestBody SbomDto.CreateRequest request) {
        return ApiResponse.created(sbomService.create(request));
    }

    @PatchMapping("/software/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ApiResponse<SbomDto.DetailResponse> update(@PathVariable Long id, @RequestBody SbomDto.UpdateRequest request) {
        return ApiResponse.ok(sbomService.update(id, request));
    }

    @DeleteMapping("/software/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        sbomService.delete(id);
        return ApiResponse.noContent();
    }

    @PostMapping("/software/{id}/components")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ApiResponse<SbomDto.ComponentResponse> addComponent(
            @PathVariable Long id, @Valid @RequestBody SbomDto.ComponentRequest request) {
        return ApiResponse.created(sbomService.addComponent(id, request));
    }

    @PatchMapping("/components/{componentId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ApiResponse<SbomDto.ComponentResponse> updateComponent(
            @PathVariable Long componentId, @RequestBody SbomDto.ComponentRequest request) {
        return ApiResponse.ok(sbomService.updateComponent(componentId, request));
    }

    @DeleteMapping("/components/{componentId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ApiResponse<Void> deleteComponent(@PathVariable Long componentId) {
        sbomService.deleteComponent(componentId);
        return ApiResponse.noContent();
    }

    @GetMapping("/software/{id}/cyclonedx")
    public ResponseEntity<byte[]> exportCycloneDx(@PathVariable Long id) throws IOException {
        SbomDto.DetailResponse sw = sbomService.get(id);
        byte[] data = sbomCycloneDxService.export(id);
        String filename = (sw.getName() + "-" + sw.getVersion() + ".cdx.json")
                .replaceAll("[\\\\/:*?\"<>|\\s]+", "_");
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.APPLICATION_JSON)
                .body(data);
    }

    @PostMapping("/import/cyclonedx")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ApiResponse<SbomBulkUploadResult> importCycloneDx(
            @RequestParam("file") MultipartFile file) throws IOException {
        return ApiResponse.ok(sbomCycloneDxService.importBom(file));
    }

    @GetMapping("/bulk/template")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<byte[]> downloadTemplate() throws IOException {
        byte[] data = sbomBulkService.generateTemplate();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"sbom-upload-template.xlsx\"")
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(data);
    }

    @PostMapping("/bulk")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ApiResponse<SbomBulkUploadResult> bulkUpload(
            @RequestParam("file") MultipartFile file) throws IOException {
        return ApiResponse.ok(sbomBulkService.upload(file));
    }
}
