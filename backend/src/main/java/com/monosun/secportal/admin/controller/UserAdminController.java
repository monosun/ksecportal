package com.monosun.secportal.admin.controller;

import com.monosun.secportal.admin.dto.UserAdminDto;
import com.monosun.secportal.admin.service.UserAdminService;
import com.monosun.secportal.admin.service.UserBulkService;
import com.monosun.secportal.asset.dto.AssetBulkUploadResult;
import com.monosun.secportal.auth.entity.User;
import com.monosun.secportal.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/admin/users")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class UserAdminController {

    private final UserAdminService userAdminService;
    private final UserBulkService userBulkService;

    @GetMapping
    public ApiResponse<Page<UserAdminDto.Response>> list(
            @PageableDefault(size = 50) Pageable pageable) {
        return ApiResponse.ok(userAdminService.list(pageable));
    }

    @GetMapping("/simple")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<List<UserAdminDto.SimpleResponse>> listSimple() {
        return ApiResponse.ok(userAdminService.listSimple());
    }

    @PostMapping
    public ApiResponse<UserAdminDto.Response> create(
            @AuthenticationPrincipal User currentUser,
            @RequestBody UserAdminDto.CreateRequest request) {
        return ApiResponse.ok(userAdminService.create(request, currentUser));
    }

    @PatchMapping("/{id}")
    public ApiResponse<?> update(
            @AuthenticationPrincipal User currentUser,
            @PathVariable Long id,
            @RequestBody UserAdminDto.UpdateRequest request) {
        if (request.getRole() == User.Role.ADMIN && !userAdminService.isAlreadyAdmin(id)) {
            return ApiResponse.ok("승인 이메일이 발송되었습니다.",
                    userAdminService.requestPromoteAdmin(id, currentUser));
        }
        return ApiResponse.ok(userAdminService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<UserAdminDto.PendingResponse> delete(
            @AuthenticationPrincipal User currentUser,
            @PathVariable Long id) {
        return ApiResponse.ok("승인 이메일이 발송되었습니다.",
                userAdminService.requestDelete(id, currentUser));
    }

    @DeleteMapping("/{id}/permanent")
    public ApiResponse<UserAdminDto.PendingResponse> hardDelete(
            @AuthenticationPrincipal User currentUser,
            @PathVariable Long id) {
        return ApiResponse.ok("완전 삭제 승인 요청이 전송되었습니다.",
                userAdminService.requestHardDelete(id, currentUser));
    }

    @GetMapping("/bulk/template")
    public ResponseEntity<byte[]> downloadTemplate() throws IOException {
        byte[] data = userBulkService.generateTemplate();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"user-upload-template.xlsx\"")
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(data);
    }

    @PostMapping("/bulk")
    public ApiResponse<AssetBulkUploadResult> bulkUpload(
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal User currentUser) throws IOException {
        return ApiResponse.ok(userBulkService.upload(file, currentUser));
    }
}
