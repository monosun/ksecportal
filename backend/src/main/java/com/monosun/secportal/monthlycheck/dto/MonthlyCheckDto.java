package com.monosun.secportal.monthlycheck.dto;

import com.monosun.secportal.monthlycheck.entity.MonthlyCheckEvidence;
import com.monosun.secportal.monthlycheck.entity.MonthlyCheckItem;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.time.LocalDateTime;

public class MonthlyCheckDto {

    @Getter
    @Builder
    public static class CheckItemResponse {
        private Long id;
        private String yearMonth;
        private String priority;
        private String category;
        private String itemName;
        private String checkMethod;
        private String checkExample;
        private String result;
        private String notes;
        private int sortOrder;
        private Long assigneeId;
        private String assigneeName;
        private long evidenceCount;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        public static CheckItemResponse from(MonthlyCheckItem item) {
            return CheckItemResponse.builder()
                    .id(item.getId())
                    .yearMonth(item.getYearMonth())
                    .priority(item.getPriority().name())
                    .category(item.getCategory())
                    .itemName(item.getItemName())
                    .checkMethod(item.getCheckMethod())
                    .checkExample(item.getCheckExample())
                    .result(item.getResult().name())
                    .notes(item.getNotes())
                    .sortOrder(item.getSortOrder())
                    .assigneeId(item.getAssignee() != null ? item.getAssignee().getId() : null)
                    .assigneeName(item.getAssignee() != null ? item.getAssignee().getName()
                            : item.getAssigneeText())
                    .evidenceCount(item.getEvidences().size())
                    .createdAt(item.getCreatedAt())
                    .updatedAt(item.getUpdatedAt())
                    .build();
        }
    }

    @Getter
    @Builder
    public static class EvidenceResponse {
        private Long id;
        private Long checkItemId;
        private String title;
        private String content;
        private String fileName;
        private Long uploadedById;
        private String uploadedByName;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        public static EvidenceResponse from(MonthlyCheckEvidence e) {
            return EvidenceResponse.builder()
                    .id(e.getId())
                    .checkItemId(e.getCheckItem().getId())
                    .title(e.getTitle())
                    .content(e.getContent())
                    .fileName(e.getFileName())
                    .uploadedById(e.getUploadedBy() != null ? e.getUploadedBy().getId() : null)
                    .uploadedByName(e.getUploadedBy() != null ? e.getUploadedBy().getName() : null)
                    .createdAt(e.getCreatedAt())
                    .updatedAt(e.getUpdatedAt())
                    .build();
        }
    }

    @Getter
    @NoArgsConstructor
    public static class CreateRequest {
        @NotBlank
        private String yearMonth;
        private String priority;
        @NotBlank
        private String category;
        @NotBlank
        private String itemName;
        private String checkMethod;
        private String checkExample;
        private String result;
        private String notes;
        private int sortOrder;
        private Long assigneeId;
        private String assigneeText;
    }

    @Getter
    @NoArgsConstructor
    public static class UpdateRequest {
        private String priority;
        private String category;
        private String itemName;
        private String checkMethod;
        private String checkExample;
        private String result;
        private String notes;
        private Integer sortOrder;
        private Long assigneeId;
        private String assigneeText;
        private boolean clearAssignee;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class EvidenceCreateRequest {
        @NotBlank
        private String title;
        private String content;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class EvidenceUpdateRequest {
        private String title;
        private String content;
    }

    @Getter
    @Builder
    public static class SummaryResponse {
        private int total;
        private int completed;
        private int incomplete;
        private int na;
    }
}
