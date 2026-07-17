package com.monosun.secportal.privacy.dto;

import com.monosun.secportal.privacy.entity.PrivacyConsent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class PrivacyConsentDto {

    @Getter
    @Setter
    public static class Request {
        @NotBlank @Size(max = 200)
        private String title;
        @Size(max = 50)
        private String version;
        private String consentType;
        private String infoItems;
        private String purpose;
        @Size(max = 500)
        private String legalBasis;
        @Size(max = 200)
        private String retentionPeriod;
        @Size(max = 200)
        private String channel;
        private LocalDate effectiveDate;
        private String content;
        private String status;
        private String notes;
    }

    @Getter
    @Builder
    public static class Response {
        private Long id;
        private String title;
        private String version;
        private String consentType;
        private String infoItems;
        private String purpose;
        private String legalBasis;
        private String retentionPeriod;
        private String channel;
        private LocalDate effectiveDate;
        private String content;
        private String status;
        private String notes;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        public static Response from(PrivacyConsent c) {
            return Response.builder()
                    .id(c.getId())
                    .title(c.getTitle())
                    .version(c.getVersion())
                    .consentType(c.getConsentType().name())
                    .infoItems(c.getInfoItems())
                    .purpose(c.getPurpose())
                    .legalBasis(c.getLegalBasis())
                    .retentionPeriod(c.getRetentionPeriod())
                    .channel(c.getChannel())
                    .effectiveDate(c.getEffectiveDate())
                    .content(c.getContent())
                    .status(c.getStatus().name())
                    .notes(c.getNotes())
                    .createdAt(c.getCreatedAt())
                    .updatedAt(c.getUpdatedAt())
                    .build();
        }
    }
}
