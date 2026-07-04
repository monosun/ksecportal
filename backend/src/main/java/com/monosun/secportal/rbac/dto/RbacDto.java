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

    @Getter @Builder
    public static class MyPermissions {
        private Map<String, PermissionEntry> permissions;
        private boolean isAdmin;
    }
}
