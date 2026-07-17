package com.monosun.secportal.privacy.dto;

import com.monosun.secportal.privacy.entity.PrivacyProcessing;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

public class PrivacyProcessingDto {

    @Getter
    @Setter
    public static class Request {
        @NotBlank @Size(max = 200)
        private String name;
        @Size(max = 100)
        private String department;
        private String purpose;
        private String infoItems;
        @Size(max = 200)
        private String retentionPeriod;
        @Size(max = 500)
        private String legalBasis;
        @Size(max = 200)
        private String systemName;
        private String workflow;
        @Size(max = 200)
        private String lifecycle;
        private String status;
        private String notes;
    }

    @Getter
    @Builder
    public static class Response {
        private Long id;
        private String name;
        private String department;
        private String purpose;
        private String infoItems;
        private String retentionPeriod;
        private String legalBasis;
        private String systemName;
        private String workflow;
        private String lifecycle;
        private String status;
        private String notes;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        public static Response from(PrivacyProcessing p) {
            return Response.builder()
                    .id(p.getId())
                    .name(p.getName())
                    .department(p.getDepartment())
                    .purpose(p.getPurpose())
                    .infoItems(p.getInfoItems())
                    .retentionPeriod(p.getRetentionPeriod())
                    .legalBasis(p.getLegalBasis())
                    .systemName(p.getSystemName())
                    .workflow(p.getWorkflow())
                    .lifecycle(p.getLifecycle())
                    .status(p.getStatus().name())
                    .notes(p.getNotes())
                    .createdAt(p.getCreatedAt())
                    .updatedAt(p.getUpdatedAt())
                    .build();
        }
    }
}
