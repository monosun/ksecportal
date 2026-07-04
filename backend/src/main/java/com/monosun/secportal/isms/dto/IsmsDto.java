package com.monosun.secportal.isms.dto;

import com.monosun.secportal.isms.entity.IsmsEvidence;
import com.monosun.secportal.isms.entity.IsmsItem;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

public class IsmsDto {

    @Getter
    @Builder
    public static class PolicyRef {
        private Long id;
        private String title;
        private String status;
        private String category;
    }

    @Getter
    @Builder
    public static class ItemResponse {
        private Long id;
        private String itemCode;
        private String itemName;
        private String domainCode;
        private String domainName;
        private int sectionNum;
        private String sectionName;
        private String description;
        private int sortOrder;
        private long evidenceCount;
        private String latestStatus;
        private List<PolicyRef> mappedPolicies;

        public static ItemResponse from(IsmsItem item) {
            return ItemResponse.builder()
                    .id(item.getId())
                    .itemCode(item.getItemCode())
                    .itemName(item.getItemName())
                    .domainCode(item.getDomainCode())
                    .domainName(item.getDomainName())
                    .sectionNum(item.getSectionNum())
                    .sectionName(item.getSectionName())
                    .description(item.getDescription())
                    .sortOrder(item.getSortOrder())
                    .mappedPolicies(List.of())
                    .build();
        }

        public static ItemResponse from(IsmsItem item, long evidenceCount, String latestStatus,
                                        List<PolicyRef> mappedPolicies) {
            return ItemResponse.builder()
                    .id(item.getId())
                    .itemCode(item.getItemCode())
                    .itemName(item.getItemName())
                    .domainCode(item.getDomainCode())
                    .domainName(item.getDomainName())
                    .sectionNum(item.getSectionNum())
                    .sectionName(item.getSectionName())
                    .description(item.getDescription())
                    .sortOrder(item.getSortOrder())
                    .evidenceCount(evidenceCount)
                    .latestStatus(latestStatus)
                    .mappedPolicies(mappedPolicies != null ? mappedPolicies : List.of())
                    .build();
        }
    }

    @Getter
    @Builder
    public static class EvidenceResponse {
        private Long id;
        private Long itemId;
        private String itemCode;
        private String itemName;
        private int year;
        private String title;
        private String content;
        private String fileName;
        private String filePath;
        private String status;
        private Long registrantId;
        private String registrantName;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
        private Long sourceEvidenceId;
        private String sourceItemCode;
        private String sourceItemName;
        private String sourceEvidenceTitle;
        private String sourceFileName;

        public static EvidenceResponse from(IsmsEvidence e) {
            IsmsEvidence src = e.getSourceEvidence();
            return EvidenceResponse.builder()
                    .id(e.getId())
                    .itemId(e.getItem().getId())
                    .itemCode(e.getItem().getItemCode())
                    .itemName(e.getItem().getItemName())
                    .year(e.getYear())
                    .title(e.getTitle())
                    .content(e.getContent())
                    .fileName(src != null ? src.getFileName() : e.getFileName())
                    .filePath(src != null ? src.getFilePath() : e.getFilePath())
                    .status(e.getStatus().name())
                    .registrantId(e.getRegistrant() != null ? e.getRegistrant().getId() : null)
                    .registrantName(e.getRegistrant() != null ? e.getRegistrant().getName() : null)
                    .createdAt(e.getCreatedAt())
                    .updatedAt(e.getUpdatedAt())
                    .sourceEvidenceId(src != null ? src.getId() : null)
                    .sourceItemCode(src != null ? src.getItem().getItemCode() : null)
                    .sourceItemName(src != null ? src.getItem().getItemName() : null)
                    .sourceEvidenceTitle(src != null ? src.getTitle() : null)
                    .sourceFileName(src != null ? src.getFileName() : null)
                    .build();
        }
    }

    @Getter
    @Builder
    public static class EvidenceSearchResult {
        private Long id;
        private String itemCode;
        private String itemName;
        private String title;
        private String fileName;
        private String status;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class EvidenceCreateRequest {
        @NotNull
        private Integer year;
        @NotBlank
        private String title;
        private String content;
        private String status;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class EvidenceRefRequest {
        @NotNull
        private Integer year;
        @NotBlank
        private String title;
        private String status;
        @NotNull
        private Long sourceEvidenceId;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class EvidenceUpdateRequest {
        private String title;
        private String content;
        private String status;
    }

    @Getter
    @Builder
    public static class DomainSummary {
        private String domainCode;
        private String domainName;
        private int sectionNum;
        private int total;
        private int compliant;
        private int partial;
        private int nonCompliant;
        private int na;
        private int noEvidence;
    }

    @Getter
    @Builder
    public static class BulkImportResult {
        private int total;
        private int success;
        private int failed;
        private List<RowError> errors;

        @Getter
        @Builder
        public static class RowError {
            private int row;
            private String itemCode;
            private String message;
        }
    }

    @Getter
    @Builder
    public static class SummaryResponse {
        private int year;
        private int totalItems;
        private int compliant;
        private int partial;
        private int nonCompliant;
        private int na;
        private int noEvidence;
        private List<DomainSummary> byDomain;
    }
}
