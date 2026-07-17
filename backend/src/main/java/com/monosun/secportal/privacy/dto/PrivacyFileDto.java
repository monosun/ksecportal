package com.monosun.secportal.privacy.dto;

import com.monosun.secportal.privacy.entity.PrivacyFile;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

public class PrivacyFileDto {

    @Getter
    @Setter
    public static class Request {
        @NotBlank @Size(max = 200)
        private String name;
        @Size(max = 100)
        private String department;
        @Size(max = 200)
        private String systemName;
        @Size(max = 200)
        private String dbTable;
        private String infoItems;
        @Size(max = 200)
        private String retentionPeriod;
        private Boolean sensitiveInfo;
        private Boolean uniqueIdentifier;
        private Integer recordCount;
        private String status;
        private String notes;
    }

    @Getter
    @Builder
    public static class Response {
        private Long id;
        private String name;
        private String department;
        private String systemName;
        private String dbTable;
        private String infoItems;
        private String retentionPeriod;
        private Boolean sensitiveInfo;
        private Boolean uniqueIdentifier;
        private Integer recordCount;
        private String status;
        private String notes;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        public static Response from(PrivacyFile f) {
            return Response.builder()
                    .id(f.getId())
                    .name(f.getName())
                    .department(f.getDepartment())
                    .systemName(f.getSystemName())
                    .dbTable(f.getDbTable())
                    .infoItems(f.getInfoItems())
                    .retentionPeriod(f.getRetentionPeriod())
                    .sensitiveInfo(f.getSensitiveInfo())
                    .uniqueIdentifier(f.getUniqueIdentifier())
                    .recordCount(f.getRecordCount())
                    .status(f.getStatus().name())
                    .notes(f.getNotes())
                    .createdAt(f.getCreatedAt())
                    .updatedAt(f.getUpdatedAt())
                    .build();
        }
    }
}
