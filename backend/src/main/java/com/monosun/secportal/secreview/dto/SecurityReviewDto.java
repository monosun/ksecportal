package com.monosun.secportal.secreview.dto;

import com.monosun.secportal.secreview.entity.SecurityReview;
import com.monosun.secportal.secreview.entity.SecurityReviewItem;
import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public class SecurityReviewDto {

    @Getter
    @Setter
    @NoArgsConstructor
    public static class CreateRequest {
        @NotBlank private String title;
        @NotBlank private String systemName;
        private String reviewType;
        private String department;
        private String description;
        private Boolean handlesPersonalData;
        private Boolean internetFacing;
        private LocalDate targetDate;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class UpdateRequest {
        private String title;
        private String systemName;
        private String reviewType;
        private String department;
        private String description;
        private Boolean handlesPersonalData;
        private Boolean internetFacing;
        private LocalDate targetDate;
        private String status;
    }

    /** 심의 완료 처리 — 결과와 의견을 함께 확정한다 */
    @Getter
    @Setter
    @NoArgsConstructor
    public static class DecisionRequest {
        private String decision;        // APPROVED / CONDITIONAL / REJECTED
        private String reviewComment;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class ItemRequest {
        private String category;
        private String itemName;
        private String criteria;
        private String result;
        private String comment;
        private Integer sortOrder;
    }

    @Getter
    @Builder
    public static class ItemResponse {
        private Long id;
        private String category;
        private String itemName;
        private String criteria;
        private String result;
        private String comment;
        private int sortOrder;

        public static ItemResponse from(SecurityReviewItem i) {
            return ItemResponse.builder()
                    .id(i.getId())
                    .category(i.getCategory())
                    .itemName(i.getItemName())
                    .criteria(i.getCriteria())
                    .result(i.getResult().name())
                    .comment(i.getComment())
                    .sortOrder(i.getSortOrder())
                    .build();
        }
    }

    @Getter
    @Builder
    public static class Response {
        private Long id;
        private String title;
        private String systemName;
        private String reviewType;
        private String department;
        private Long requesterId;
        private String requesterName;
        private String description;
        private boolean handlesPersonalData;
        private boolean internetFacing;
        private LocalDate targetDate;
        private String status;
        private String decision;
        private String reviewComment;
        private String reviewerName;
        private LocalDateTime reviewedAt;
        private String fileName;
        private boolean hasFile;
        private LocalDateTime createdAt;
        /** 검토 진행률 계산용 (해당없음 제외) */
        private int itemTotal;
        private int itemChecked;
        private int itemFailed;
        private List<ItemResponse> items;

        public static Response from(SecurityReview r, boolean withItems) {
            List<SecurityReviewItem> items = r.getItems();
            int total = items.size();
            int checked = (int) items.stream().filter(i -> i.getResult() != SecurityReviewItem.Result.PENDING).count();
            int failed = (int) items.stream().filter(i -> i.getResult() == SecurityReviewItem.Result.FAIL).count();
            return Response.builder()
                    .id(r.getId())
                    .title(r.getTitle())
                    .systemName(r.getSystemName())
                    .reviewType(r.getReviewType().name())
                    .department(r.getDepartment())
                    .requesterId(r.getRequester() != null ? r.getRequester().getId() : null)
                    .requesterName(r.getRequester() != null ? r.getRequester().getName() : null)
                    .description(r.getDescription())
                    .handlesPersonalData(r.isHandlesPersonalData())
                    .internetFacing(r.isInternetFacing())
                    .targetDate(r.getTargetDate())
                    .status(r.getStatus().name())
                    .decision(r.getDecision() != null ? r.getDecision().name() : null)
                    .reviewComment(r.getReviewComment())
                    .reviewerName(r.getReviewer() != null ? r.getReviewer().getName() : null)
                    .reviewedAt(r.getReviewedAt())
                    .fileName(r.getFileName())
                    .hasFile(r.getFilePath() != null)
                    .createdAt(r.getCreatedAt())
                    .itemTotal(total)
                    .itemChecked(checked)
                    .itemFailed(failed)
                    .items(withItems ? items.stream().map(ItemResponse::from).toList() : null)
                    .build();
        }
    }

    @Getter
    @Builder
    public static class Summary {
        private long requested;
        private long inReview;
        private long revision;
        private long completed;
        private long approved;
        private long conditional;
        private long rejected;
    }
}
