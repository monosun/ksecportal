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
        /** MANUAL(직접 등록) / ISMS_DEFECT(ISMS 결함관리에서 가져옴) */
        private String sourceType;
        /** 원본 ISMS 결함 id — 화면에서 원본으로 이동하는 데 쓴다 */
        private Long sourceDefectId;
        /** 출처 표시용 라벨 — 예: "ISMS-P 갱신심사 · 결함-01" */
        private String sourceLabel;
        /** 이 화면에서 수정·삭제할 수 있는지 (가져온 건은 false) */
        private boolean editable;

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
                    .sourceType(f.resolvedSourceType().name())
                    .sourceDefectId(f.getSourceDefectId())
                    .sourceLabel(f.getSourceLabel())
                    .editable(!f.isFromIsmsDefect())
                    .build();
        }
    }
}
