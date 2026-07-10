package com.monosun.secportal.admin.dto;

import com.monosun.secportal.auth.entity.User;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

public class UserAdminDto {

    @Getter
    public static class UpdateRequest {
        private String name;
        private User.Role role;
        private Boolean active;
        private String department;
        private String newPassword;
    }

    @Getter
    public static class CreateRequest {
        private String email;
        private String name;
        private String password;
        private User.Role role;
        private String department;
    }

    @Getter
    @Builder
    public static class Response {
        private Long id;
        private String email;
        private String name;
        private String role;
        private String department;
        private boolean active;
        private int failedLoginAttempts;
        private boolean locked;
        private LocalDateTime lockedUntil;
        private LocalDateTime createdAt;

        public static Response from(User user) {
            boolean locked = user.getLockedUntil() != null
                    && user.getLockedUntil().isAfter(LocalDateTime.now());
            return Response.builder()
                    .id(user.getId())
                    .email(user.getEmail())
                    .name(user.getName())
                    .role(user.getRole().name())
                    .department(user.getDepartment())
                    .active(user.isActive())
                    .failedLoginAttempts(user.getFailedLoginAttempts())
                    .locked(locked)
                    .lockedUntil(user.getLockedUntil())
                    .createdAt(user.getCreatedAt())
                    .build();
        }
    }

    @Getter
    @Builder
    public static class SimpleResponse {
        private Long id;
        private String name;
        private String department;

        public static SimpleResponse from(User user) {
            return SimpleResponse.builder()
                    .id(user.getId())
                    .name(user.getName())
                    .department(user.getDepartment())
                    .build();
        }
    }

    @Getter
    @Builder
    public static class PendingResponse {
        private boolean pending;
        private String actionType;
        private String message;
    }
}
