package com.monosun.secportal.rbac.controller;

import com.monosun.secportal.auth.entity.User;
import com.monosun.secportal.common.response.ApiResponse;
import com.monosun.secportal.rbac.dto.RbacDto;
import com.monosun.secportal.rbac.service.RbacService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class RbacController {

    private final RbacService service;

    // ── 내 권한 조회 (로그인한 모든 사용자) ──────────────────────────────────

    @GetMapping("/auth/my-permissions")
    public ApiResponse<RbacDto.MyPermissions> myPermissions(@AuthenticationPrincipal User user) {
        return ApiResponse.ok(service.getMyPermissions(user));
    }

    // ── Role 관리 (ADMIN 전용) ─────────────────────────────────────────────

    @GetMapping("/admin/roles")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<List<RbacDto.RoleResponse>> list() {
        return ApiResponse.ok(service.listRoles());
    }

    @GetMapping("/admin/roles/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<RbacDto.RoleResponse> get(@PathVariable Long id) {
        return ApiResponse.ok(service.getRole(id));
    }

    @PostMapping("/admin/roles")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<RbacDto.RoleResponse> create(@RequestBody RbacDto.RoleCreateRequest req) {
        return ApiResponse.ok(service.createRole(req));
    }

    @PutMapping("/admin/roles/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<RbacDto.RoleResponse> update(@PathVariable Long id,
                                                     @RequestBody RbacDto.RoleUpdateRequest req) {
        return ApiResponse.ok(service.updateRole(id, req));
    }

    @DeleteMapping("/admin/roles/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        service.deleteRole(id);
        return ApiResponse.ok(null);
    }

    // ── Role 사용자 관리 ──────────────────────────────────────────────────

    @GetMapping("/admin/roles/{id}/users")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<List<RbacDto.UserSummary>> getUsers(@PathVariable Long id) {
        return ApiResponse.ok(service.getRoleUsers(id));
    }

    @PostMapping("/admin/roles/{id}/users/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> assignUser(@PathVariable Long id, @PathVariable Long userId) {
        service.assignUser(id, userId);
        return ApiResponse.ok(null);
    }

    @DeleteMapping("/admin/roles/{id}/users/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> removeUser(@PathVariable Long id, @PathVariable Long userId) {
        service.removeUser(id, userId);
        return ApiResponse.ok(null);
    }
}
