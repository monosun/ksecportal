package com.monosun.secportal.privacy.dto;

import com.monosun.secportal.privacy.entity.PrivacyProvision;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class PrivacyProvisionDto {

    @Getter
    @Setter
    public static class Request {
        private String provisionType;
        @NotBlank @Size(max = 200)
        private String recipient;
        @Size(max = 100)
        private String country;
        private String infoItems;
        private String purpose;
        @Size(max = 500)
        private String legalBasis;
        @Size(max = 200)
        private String retentionPeriod;
        @Size(max = 200)
        private String method;
        private String contractInfo;
        private LocalDate contractStart;
        private LocalDate contractEnd;
        private LocalDate provisionDate;
        private String status;
        private String notes;
    }

    @Getter
    @Builder
    public static class Response {
        private Long id;
        private String provisionType;
        private String recipient;
        private String country;
        private String infoItems;
        private String purpose;
        private String legalBasis;
        private String retentionPeriod;
        private String method;
        private String contractInfo;
        private LocalDate contractStart;
        private LocalDate contractEnd;
        private LocalDate provisionDate;
        private String status;
        private String notes;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        public static Response from(PrivacyProvision p) {
            return Response.builder()
                    .id(p.getId())
                    .provisionType(p.getProvisionType().name())
                    .recipient(p.getRecipient())
                    .country(p.getCountry())
                    .infoItems(p.getInfoItems())
                    .purpose(p.getPurpose())
                    .legalBasis(p.getLegalBasis())
                    .retentionPeriod(p.getRetentionPeriod())
                    .method(p.getMethod())
                    .contractInfo(p.getContractInfo())
                    .contractStart(p.getContractStart())
                    .contractEnd(p.getContractEnd())
                    .provisionDate(p.getProvisionDate())
                    .status(p.getStatus().name())
                    .notes(p.getNotes())
                    .createdAt(p.getCreatedAt())
                    .updatedAt(p.getUpdatedAt())
                    .build();
        }
    }
}
