package com.monosun.secportal.privacy.dto;

import com.monosun.secportal.privacy.entity.PrivacyRetention;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class PrivacyRetentionDto {

    @Getter
    @Setter
    public static class Request {
        @NotBlank @Size(max = 200)
        private String targetName;
        private Long privacyFileId;
        @Size(max = 100)
        private String department;
        private String infoItems;
        @Size(max = 200)
        private String retentionPeriod;
        @Size(max = 500)
        private String legalBasis;
        private LocalDate startDate;
        private LocalDate expiryDate;
        private LocalDate disposalDueDate;
        private String extensionReason;
        private Boolean notifyEnabled;
        private Integer notifyDaysBefore;
        private String status;
        private String notes;
    }

    @Getter
    @Builder
    public static class Response {
        private Long id;
        private String targetName;
        private Long privacyFileId;
        private String department;
        private String infoItems;
        private String retentionPeriod;
        private String legalBasis;
        private LocalDate startDate;
        private LocalDate expiryDate;
        private LocalDate disposalDueDate;
        private String extensionReason;
        private Boolean notifyEnabled;
        private Integer notifyDaysBefore;
        private String status;
        private String notes;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
        /** 만료까지 남은 일수 — 음수면 이미 만료 경과 */
        private Long daysUntilExpiry;

        public static Response from(PrivacyRetention r) {
            Long days = r.getExpiryDate() == null ? null
                    : java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), r.getExpiryDate());
            return Response.builder()
                    .id(r.getId())
                    .targetName(r.getTargetName())
                    .privacyFileId(r.getPrivacyFileId())
                    .department(r.getDepartment())
                    .infoItems(r.getInfoItems())
                    .retentionPeriod(r.getRetentionPeriod())
                    .legalBasis(r.getLegalBasis())
                    .startDate(r.getStartDate())
                    .expiryDate(r.getExpiryDate())
                    .disposalDueDate(r.getDisposalDueDate())
                    .extensionReason(r.getExtensionReason())
                    .notifyEnabled(r.getNotifyEnabled())
                    .notifyDaysBefore(r.getNotifyDaysBefore())
                    .status(r.getStatus().name())
                    .notes(r.getNotes())
                    .createdAt(r.getCreatedAt())
                    .updatedAt(r.getUpdatedAt())
                    .daysUntilExpiry(days)
                    .build();
        }
    }
}
