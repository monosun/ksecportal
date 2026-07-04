package com.monosun.secportal.secfinding.dto;

import com.monosun.secportal.secfinding.entity.SecurityFinding;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class SecurityFindingDto {

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Request {
        private int year;
        private String auditType;
        private String domain;
        private String requirementCode;
        private String requirementName;
        private String findingSummary;
        private String findingDetail;
        private String riskLevel;
        private String correctiveAction;
        private LocalDate actionDeadline;
        private String status;
        private LocalDate resolvedAt;
        private String resolver;
    }

    @Getter
    @Builder
    public static class Response {
        private Long id;
        private int year;
        private String auditType;
        private String domain;
        private String requirementCode;
        private String requirementName;
        private String findingSummary;
        private String findingDetail;
        private String riskLevel;
        private String correctiveAction;
        private LocalDate actionDeadline;
        private String status;
        private LocalDate resolvedAt;
        private String resolver;
        private String fileName;
        private Long fileSize;
        private String createdByName;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        public static Response from(SecurityFinding f) {
            return Response.builder()
                    .id(f.getId())
                    .year(f.getYear())
                    .auditType(f.getAuditType().name())
                    .domain(f.getDomain())
                    .requirementCode(f.getRequirementCode())
                    .requirementName(f.getRequirementName())
                    .findingSummary(f.getFindingSummary())
                    .findingDetail(f.getFindingDetail())
                    .riskLevel(f.getRiskLevel().name())
                    .correctiveAction(f.getCorrectiveAction())
                    .actionDeadline(f.getActionDeadline())
                    .status(f.getStatus().name())
                    .resolvedAt(f.getResolvedAt())
                    .resolver(f.getResolver())
                    .fileName(f.getFileName())
                    .fileSize(f.getFileSize())
                    .createdByName(f.getCreatedBy() != null ? f.getCreatedBy().getName() : null)
                    .createdAt(f.getCreatedAt())
                    .updatedAt(f.getUpdatedAt())
                    .build();
        }
    }
}
