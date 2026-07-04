package com.monosun.secportal.audit.dto;

import com.monosun.secportal.audit.entity.AuditLog;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

public class AuditLogDto {

    @Getter
    @Builder
    public static class Response {
        private Long id;
        private Long userId;
        private String userName;
        private String action;
        private String resourceType;
        private Long resourceId;
        private String detail;
        private String ipAddress;
        private LocalDateTime createdAt;

        public static Response from(AuditLog log) {
            return Response.builder()
                    .id(log.getId())
                    .userId(log.getUser() != null ? log.getUser().getId() : null)
                    .userName(log.getUser() != null ? log.getUser().getName() : "System")
                    .action(log.getAction())
                    .resourceType(log.getResourceType())
                    .resourceId(log.getResourceId())
                    .detail(log.getDetail())
                    .ipAddress(log.getIpAddress())
                    .createdAt(log.getCreatedAt())
                    .build();
        }
    }
}
