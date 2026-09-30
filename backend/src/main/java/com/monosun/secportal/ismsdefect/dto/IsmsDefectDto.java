package com.monosun.secportal.ismsdefect.dto;

import com.monosun.secportal.ismsdefect.entity.IsmsDefect;
import com.monosun.secportal.ismsdefect.entity.IsmsDefectReport;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public class IsmsDefectDto {

    // ── 결함 ────────────────────────────────────────────────────────────────

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DefectRequest {
        private Integer year;
        private String auditType;
        private String defectNo;
        private String defectType;
        private String severity;
        private String domainName;
        private String itemCode;
        private String itemName;
        private String title;
        private String content;
        private String cause;
        private String actionPlan;
        private String actionResult;
        private String preventionPlan;
        private LocalDate dueDate;
        private LocalDate completedDate;
        private String assignee;
        private String department;
        private String status;
        private Integer sortOrder;
        /** true 면 기존 첨부파일을 삭제한다(새 파일을 올리지 않는 경우) */
        private Boolean removeFile;
    }

    @Getter
    @Builder
    public static class DefectResponse {
        private Long id;
        private int year;
        private String auditType;
        private String defectNo;
        private String defectType;
        private String severity;
        private String domainName;
        private String itemCode;
        private String itemName;
        private String title;
        private String content;
        private String cause;
        private String actionPlan;
        private String actionResult;
        private String preventionPlan;
        private LocalDate dueDate;
        private LocalDate completedDate;
        private String assignee;
        private String department;
        private String status;
        private Integer sortOrder;
        private String fileName;
        private Long fileSize;
        private String registrantName;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        public static DefectResponse from(IsmsDefect d) {
            return DefectResponse.builder()
                    .id(d.getId())
                    .year(d.getYear())
                    .auditType(d.getAuditType().name())
                    .defectNo(d.getDefectNo())
                    .defectType(d.getDefectType().name())
                    .severity(d.getSeverity().name())
                    .domainName(d.getDomainName())
                    .itemCode(d.getItemCode())
                    .itemName(d.getItemName())
                    .title(d.getTitle())
                    .content(d.getContent())
                    .cause(d.getCause())
                    .actionPlan(d.getActionPlan())
                    .actionResult(d.getActionResult())
                    .preventionPlan(d.getPreventionPlan())
                    .dueDate(d.getDueDate())
                    .completedDate(d.getCompletedDate())
                    .assignee(d.getAssignee())
                    .department(d.getDepartment())
                    .status(d.getStatus().name())
                    .sortOrder(d.getSortOrder())
                    .fileName(d.getFileName())
                    .fileSize(d.getFileSize())
                    .registrantName(d.getRegistrant() != null ? d.getRegistrant().getName() : null)
                    .createdAt(d.getCreatedAt())
                    .updatedAt(d.getUpdatedAt())
                    .build();
        }
    }

    // ── 연도별 보고서 ────────────────────────────────────────────────────────

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReportRequest {
        private String title;
        private String auditType;
        private String auditOrg;
        private String auditors;
        private String auditScope;
        private LocalDate auditStartDate;
        private LocalDate auditEndDate;
        private String summary;
        private String content;
        private String conclusion;
        private LocalDate reportedAt;
        private String reporter;
        private Boolean removeFile;
    }

    @Getter
    @Builder
    public static class ReportResponse {
        private Long id;
        private int year;
        private String title;
        private String auditType;
        private String auditOrg;
        private String auditors;
        private String auditScope;
        private LocalDate auditStartDate;
        private LocalDate auditEndDate;
        private String summary;
        private String content;
        private String conclusion;
        private LocalDate reportedAt;
        private String reporter;
        private String fileName;
        private Long fileSize;
        private String registrantName;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
        /** 아직 저장된 보고서가 없으면 false — 프론트에서 "미작성" 표시에 쓴다 */
        private boolean exists;

        public static ReportResponse from(IsmsDefectReport r) {
            return ReportResponse.builder()
                    .id(r.getId())
                    .year(r.getYear())
                    .title(r.getTitle())
                    .auditType(r.getAuditType().name())
                    .auditOrg(r.getAuditOrg())
                    .auditors(r.getAuditors())
                    .auditScope(r.getAuditScope())
                    .auditStartDate(r.getAuditStartDate())
                    .auditEndDate(r.getAuditEndDate())
                    .summary(r.getSummary())
                    .content(r.getContent())
                    .conclusion(r.getConclusion())
                    .reportedAt(r.getReportedAt())
                    .reporter(r.getReporter())
                    .fileName(r.getFileName())
                    .fileSize(r.getFileSize())
                    .registrantName(r.getRegistrant() != null ? r.getRegistrant().getName() : null)
                    .createdAt(r.getCreatedAt())
                    .updatedAt(r.getUpdatedAt())
                    .exists(true)
                    .build();
        }

        public static ReportResponse empty(int year) {
            return ReportResponse.builder()
                    .year(year)
                    .auditType(IsmsDefect.AuditType.RENEWAL.name())
                    .exists(false)
                    .build();
        }
    }

    // ── 요약 ────────────────────────────────────────────────────────────────

    @Getter
    @Builder
    public static class SummaryResponse {
        private int year;
        private long total;
        private long open;
        private long inProgress;
        private long completed;
        private long hold;
        private long defect;
        private long recommendation;
        private long improvement;
        private long overdue;
        private int completionRate;
        private boolean reportWritten;
        private List<Integer> years;
    }
}
