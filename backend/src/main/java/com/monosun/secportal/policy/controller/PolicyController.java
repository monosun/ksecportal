package com.monosun.secportal.policy.controller;

import com.monosun.secportal.asset.dto.AssetBulkUploadResult;
import com.monosun.secportal.auth.entity.User;
import com.monosun.secportal.common.response.ApiResponse;
import com.monosun.secportal.policy.dto.PolicyDto;
import com.monosun.secportal.policy.entity.Policy;
import com.monosun.secportal.policy.service.PolicyBulkService;
import com.monosun.secportal.policy.service.PolicyService;
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
@RequestMapping("/policies")
@RequiredArgsConstructor
public class PolicyController {

    private final PolicyService policyService;
    private final PolicyBulkService policyBulkService;

    @GetMapping
    public ApiResponse<Page<PolicyDto.Summary>> list(
            @RequestParam(required = false) Policy.Status status,
            @RequestParam(required = false) Policy.Category category,
            @RequestParam(required = false) String keyword,
            @PageableDefault(size = 20) Pageable pageable) {
        return ApiResponse.ok(policyService.list(status, category, keyword, pageable));
    }

    @GetMapping("/{id}")
    public ApiResponse<PolicyDto.Response> get(
            @PathVariable Long id,
            @AuthenticationPrincipal User user) {
        return ApiResponse.ok(policyService.get(id, user.getId()));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ApiResponse<PolicyDto.Response> create(
            @Valid @RequestBody PolicyDto.CreateRequest request,
            @AuthenticationPrincipal User user) {
        return ApiResponse.created(policyService.create(request, user.getId()));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ApiResponse<PolicyDto.Response> update(
            @PathVariable Long id,
            @RequestBody PolicyDto.UpdateRequest request) {
        return ApiResponse.ok(policyService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        policyService.delete(id);
        return ApiResponse.noContent();
    }

    @PostMapping("/{id}/acknowledge")
    public ApiResponse<Void> acknowledge(
            @PathVariable Long id,
            @AuthenticationPrincipal User user) {
        policyService.acknowledge(id, user.getId());
        return ApiResponse.ok("Policy acknowledged", null);
    }

    @GetMapping("/bulk/template")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<byte[]> downloadTemplate() throws IOException {
        byte[] data = policyBulkService.generateTemplate();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"policy-upload-template.xlsx\"")
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(data);
    }

    @PostMapping("/bulk")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ApiResponse<AssetBulkUploadResult> bulkUpload(
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal User user) throws IOException {
        return ApiResponse.ok(policyBulkService.upload(file, user));
    }
}
