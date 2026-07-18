package com.monosun.secportal.privacy.dto;

import com.monosun.secportal.privacy.entity.PrivacyDpia;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class PrivacyDpiaDto {

    @Getter
    @Setter
    public static class Request {
        @NotBlank @Size(max = 200)
        private String title;
        @Size(max = 200)
        private String targetSystem;
        @Size(max = 100)
        private String department;
        private String infoItems;
        private Integer subjectCount;
        private LocalDate assessmentDate;
        @Size(max = 100)
        private String assessor;
        private String checklist;
        private String riskLevel;
        private String improvementPlan;
        private LocalDate improvementDueDate;
        private String completionReport;
        private LocalDate completedDate;
        private String status;
        private String notes;
    }

    @Getter
    @Builder
    public static class Response {
        private Long id;
        private String title;
        private String targetSystem;
        private String department;
        private String infoItems;
        private Integer subjectCount;
        private LocalDate assessmentDate;
        private String assessor;
        private String checklist;
        private String riskLevel;
        private String improvementPlan;
        private LocalDate improvementDueDate;
        private String completionReport;
        private LocalDate completedDate;
        private String status;
        private String notes;
        /** 첨부된 보고서 등 파일 건수 */
        private long fileCount;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        public static Response from(PrivacyDpia d) {
            return from(d, 0L);
        }

        public static Response from(PrivacyDpia d, long fileCount) {
            return Response.builder()
                    .fileCount(fileCount)
                    .id(d.getId())
                    .title(d.getTitle())
                    .targetSystem(d.getTargetSystem())
                    .department(d.getDepartment())
                    .infoItems(d.getInfoItems())
                    .subjectCount(d.getSubjectCount())
                    .assessmentDate(d.getAssessmentDate())
                    .assessor(d.getAssessor())
                    .checklist(d.getChecklist())
                    .riskLevel(d.getRiskLevel() != null ? d.getRiskLevel().name() : null)
                    .improvementPlan(d.getImprovementPlan())
                    .improvementDueDate(d.getImprovementDueDate())
                    .completionReport(d.getCompletionReport())
                    .completedDate(d.getCompletedDate())
                    .status(d.getStatus().name())
                    .notes(d.getNotes())
                    .createdAt(d.getCreatedAt())
                    .updatedAt(d.getUpdatedAt())
                    .build();
        }
    }
}
