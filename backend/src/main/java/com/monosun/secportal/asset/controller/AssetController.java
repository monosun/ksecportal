package com.monosun.secportal.asset.controller;

import com.monosun.secportal.asset.dto.AssetBulkUploadResult;
import com.monosun.secportal.asset.dto.AssetDto;
import com.monosun.secportal.asset.entity.Asset;
import com.monosun.secportal.asset.service.AssetBulkService;
import com.monosun.secportal.asset.service.AssetService;
import com.monosun.secportal.common.response.ApiResponse;
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

@RestController
@RequestMapping("/assets")
@RequiredArgsConstructor
public class AssetController {

    private final AssetService assetService;
    private final AssetBulkService assetBulkService;

    @GetMapping
    public ApiResponse<Page<AssetDto.Response>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) Asset.Criticality criticality,
            @RequestParam(required = false) Asset.CloudProvider cloudProvider,
            @RequestParam(required = false) Asset.Environment environment,
            @RequestParam(required = false) Boolean active,
            @RequestParam(required = false) Asset.Status status,
            @PageableDefault(size = 20) Pageable pageable) {
        return ApiResponse.ok(assetService.list(keyword, type, criticality, cloudProvider, environment, active, status, pageable));
    }

    @GetMapping("/{id}")
    public ApiResponse<AssetDto.Response> get(@PathVariable Long id) {
        return ApiResponse.ok(assetService.get(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ApiResponse<AssetDto.Response> create(@Valid @RequestBody AssetDto.CreateRequest request) {
        return ApiResponse.created(assetService.create(request));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ApiResponse<AssetDto.Response> update(@PathVariable Long id, @RequestBody AssetDto.UpdateRequest request) {
        return ApiResponse.ok(assetService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        assetService.delete(id);
        return ApiResponse.noContent();
    }

    @GetMapping("/bulk/template")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<byte[]> downloadTemplate() throws IOException {
        byte[] data = assetBulkService.generateTemplate();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"asset-upload-template.xlsx\"")
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(data);
    }

    @PostMapping("/bulk")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ApiResponse<AssetBulkUploadResult> bulkUpload(
            @RequestParam("file") MultipartFile file) throws IOException {
        return ApiResponse.ok(assetBulkService.upload(file));
    }
}
