package com.monosun.secportal.internalaudit.dto;

import com.monosun.secportal.internalaudit.entity.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

public class InternalAuditDto {

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AuditRequest {
        private int year;
        private String title;
        private LocalDate auditStart;
        private LocalDate auditEnd;
        private String auditor;
        private String status;
        private String description;
    }

    @Getter
    @Builder
    public static class AuditResponse {
        private Long id;
        private int year;
        private String title;
        private LocalDate auditStart;
        private LocalDate auditEnd;
        private String auditor;
        private String status;
        private String description;
        private String createdByName;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
        private List<TargetResponse> targets;
        private List<ItemResponse> items;
        private List<FileResponse> files;

        public static AuditResponse from(InternalAudit a) {
            return AuditResponse.builder()
                    .id(a.getId())
                    .year(a.getYear())
                    .title(a.getTitle())
                    .auditStart(a.getAuditStart())
                    .auditEnd(a.getAuditEnd())
                    .auditor(a.getAuditor())
                    .status(a.getStatus().name())
                    .description(a.getDescription())
                    .createdByName(a.getCreatedBy() != null ? a.getCreatedBy().getName() : null)
                    .createdAt(a.getCreatedAt())
                    .updatedAt(a.getUpdatedAt())
                    .targets(a.getTargets().stream().map(TargetResponse::from).collect(Collectors.toList()))
                    .items(a.getItems().stream().map(ItemResponse::from).collect(Collectors.toList()))
                    .files(a.getFiles().stream().map(FileResponse::from).collect(Collectors.toList()))
                    .build();
        }

        public static AuditResponse summary(InternalAudit a) {
            return AuditResponse.builder()
                    .id(a.getId())
                    .year(a.getYear())
                    .title(a.getTitle())
                    .auditStart(a.getAuditStart())
                    .auditEnd(a.getAuditEnd())
                    .auditor(a.getAuditor())
                    .status(a.getStatus().name())
                    .description(a.getDescription())
                    .createdByName(a.getCreatedBy() != null ? a.getCreatedBy().getName() : null)
                    .createdAt(a.getCreatedAt())
                    .updatedAt(a.getUpdatedAt())
                    .build();
        }
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TargetRequest {
        private String targetName;
        private String targetType;
        private String description;
        private int sortOrder;
    }

    @Getter
    @Builder
    public static class TargetResponse {
        private Long id;
        private String targetName;
        private String targetType;
        private String description;
        private int sortOrder;
        private LocalDateTime createdAt;

        public static TargetResponse from(AuditTarget t) {
            return TargetResponse.builder()
                    .id(t.getId())
                    .targetName(t.getTargetName())
                    .targetType(t.getTargetType())
                    .description(t.getDescription())
                    .sortOrder(t.getSortOrder())
                    .createdAt(t.getCreatedAt())
                    .build();
        }
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ItemRequest {
        private Long targetId;
        private String itemName;
        private String checkMethod;
        private String result;
        private String finding;
        private String actionRequired;
        private int sortOrder;
    }

    @Getter
    @Builder
    public static class ItemResponse {
        private Long id;
        private Long targetId;
        private String targetName;
        private String itemName;
        private String checkMethod;
        private String result;
        private String finding;
        private String actionRequired;
        private int sortOrder;
        private LocalDateTime createdAt;

        public static ItemResponse from(AuditItem i) {
            return ItemResponse.builder()
                    .id(i.getId())
                    .targetId(i.getTarget() != null ? i.getTarget().getId() : null)
                    .targetName(i.getTarget() != null ? i.getTarget().getTargetName() : null)
                    .itemName(i.getItemName())
                    .checkMethod(i.getCheckMethod())
                    .result(i.getResult() != null ? i.getResult().name() : null)
                    .finding(i.getFinding())
                    .actionRequired(i.getActionRequired())
                    .sortOrder(i.getSortOrder())
                    .createdAt(i.getCreatedAt())
                    .build();
        }
    }

    @Getter
    @Builder
    public static class FileResponse {
        private Long id;
        private String title;
        private String fileName;
        private Long fileSize;
        private String uploaderName;
        private LocalDateTime createdAt;

        public static FileResponse from(AuditFile f) {
            return FileResponse.builder()
                    .id(f.getId())
                    .title(f.getTitle())
                    .fileName(f.getFileName())
                    .fileSize(f.getFileSize())
                    .uploaderName(f.getUploader() != null ? f.getUploader().getName() : null)
                    .createdAt(f.getCreatedAt())
                    .build();
        }
    }
}
