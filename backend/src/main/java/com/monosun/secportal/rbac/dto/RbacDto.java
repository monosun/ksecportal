package com.monosun.secportal.rbac.dto;

import com.monosun.secportal.auth.entity.User;
import com.monosun.secportal.rbac.entity.CustomRole;
import com.monosun.secportal.rbac.entity.RolePermission;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class RbacDto {

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor
    public static class PermissionEntry {
        private String menuKey;
        private boolean canRead;
        private boolean canWrite;
        private boolean canDelete;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor
    public static class RoleCreateRequest {
        private String name;
        private String description;
        private List<PermissionEntry> permissions;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor
    public static class RoleUpdateRequest {
        private String name;
        private String description;
        private List<PermissionEntry> permissions;
    }

    @Getter @Builder
    public static class RoleResponse {
        private Long id;
        private String name;
        private String description;
        private List<PermissionEntry> permissions;
        private int userCount;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        public static RoleResponse from(CustomRole role) {
            List<PermissionEntry> perms = role.getPermissions().stream()
                    .map(p -> new PermissionEntry(p.getMenuKey(), p.isCanRead(), p.isCanWrite(), p.isCanDelete()))
                    .collect(Collectors.toList());
            return RoleResponse.builder()
                    .id(role.getId())
                    .name(role.getName())
                    .description(role.getDescription())
                    .permissions(perms)
                    .userCount(role.getUsers().size())
                    .createdAt(role.getCreatedAt())
                    .updatedAt(role.getUpdatedAt())
                    .build();
        }
    }

    @Getter @Builder
    public static class UserSummary {
        private Long id;
        private String name;
        private String email;
        private String role;
        private String department;

        public static UserSummary from(User user) {
            return UserSummary.builder()
                    .id(user.getId())
                    .name(user.getName())
                    .email(user.getEmail())
                    .role(user.getRole().name())
                    .department(user.getDepartment())
                    .build();
        }
    }

    /** 기본 역할(ADMIN·MANAGER·USER)의 현재 권한 */
    @Getter @Builder
    public static class BuiltinRoleResponse {
        private String role;                      // ADMIN / MANAGER / USER
        private String label;                     // 화면 표시명
        private String description;
        private boolean editable;                 // ADMIN 은 항상 전체 권한이라 수정 불가
        private int userCount;                    // 해당 역할을 가진 계정 수
        private List<PermissionEntry> permissions;
        private LocalDateTime updatedAt;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor
    public static class BuiltinRoleUpdateRequest {
        private List<PermissionEntry> permissions;
    }

    @Getter @Builder
    public static class MyPermissions {
        private Map<String, PermissionEntry> permissions;
        private boolean isAdmin;
        /**
         * 메뉴 권한 검사를 건너뛰고 전체를 허용해야 하는 계정.
         * ADMIN 이거나, 기본 역할 권한 행이 아직 없어 판단 근거가 없는 경우(안전 폴백) true.
         */
        private boolean fullAccess;
    }
}
