package com.monosun.secportal.privacy.dto;

import com.monosun.secportal.privacy.entity.PrivacyDisposal;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class PrivacyDisposalDto {

    @Getter
    @Setter
    public static class Request {
        @NotBlank @Size(max = 200)
        private String title;
        @NotBlank @Size(max = 200)
        private String targetName;
        private Long retentionId;
        @Size(max = 100)
        private String department;
        private String infoItems;
        private Integer recordCount;
        @Size(max = 200)
        private String method;
        private LocalDate plannedDate;
        private LocalDate disposalDate;
        @Size(max = 100)
        private String performer;
        @Size(max = 100)
        private String approver;
        private String approvalStatus;
        private LocalDate approvedDate;
        private String evidence;
        private String status;
        private String notes;
    }

    @Getter
    @Builder
    public static class Response {
        private Long id;
        private String title;
        private String targetName;
        private Long retentionId;
        private String department;
        private String infoItems;
        private Integer recordCount;
        private String method;
        private LocalDate plannedDate;
        private LocalDate disposalDate;
        private String performer;
        private String approver;
        private String approvalStatus;
        private LocalDate approvedDate;
        private String evidence;
        private String status;
        private String notes;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        public static Response from(PrivacyDisposal d) {
            return Response.builder()
                    .id(d.getId())
                    .title(d.getTitle())
                    .targetName(d.getTargetName())
                    .retentionId(d.getRetentionId())
                    .department(d.getDepartment())
                    .infoItems(d.getInfoItems())
                    .recordCount(d.getRecordCount())
                    .method(d.getMethod())
                    .plannedDate(d.getPlannedDate())
                    .disposalDate(d.getDisposalDate())
                    .performer(d.getPerformer())
                    .approver(d.getApprover())
                    .approvalStatus(d.getApprovalStatus().name())
                    .approvedDate(d.getApprovedDate())
                    .evidence(d.getEvidence())
                    .status(d.getStatus().name())
                    .notes(d.getNotes())
                    .createdAt(d.getCreatedAt())
                    .updatedAt(d.getUpdatedAt())
                    .build();
        }
    }
}
