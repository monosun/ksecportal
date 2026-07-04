package com.monosun.secportal.privacy.dto;

import com.monosun.secportal.privacy.entity.ContractorCheckItem;
import com.monosun.secportal.privacy.entity.ContractorCheckItemDefault;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

public class ContractorCheckItemDto {

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ItemRequest {
        @NotBlank
        private String category;
        @NotBlank
        private String itemName;
        private String checkMethod;
        private String checkStandard;
        private boolean required = true;
        private int sortOrder = 0;
    }

    @Getter
    @Builder
    public static class ItemResponse {
        private Long id;
        private String category;
        private String itemName;
        private String checkMethod;
        private String checkStandard;
        private boolean required;
        private int sortOrder;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        public static ItemResponse from(ContractorCheckItem e) {
            return ItemResponse.builder()
                    .id(e.getId())
                    .category(e.getCategory())
                    .itemName(e.getItemName())
                    .checkMethod(e.getCheckMethod())
                    .checkStandard(e.getCheckStandard())
                    .required(e.isRequired())
                    .sortOrder(e.getSortOrder())
                    .createdAt(e.getCreatedAt())
                    .updatedAt(e.getUpdatedAt())
                    .build();
        }
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DefaultItemRequest {
        @NotBlank
        private String category;
        @NotBlank
        private String itemName;
        private String checkMethod;
        private String checkStandard;
        private int sortOrder = 0;
    }

    @Getter
    @Builder
    public static class DefaultItemResponse {
        private Long id;
        private String category;
        private String itemName;
        private String checkMethod;
        private String checkStandard;
        private int sortOrder;

        public static DefaultItemResponse from(ContractorCheckItemDefault e) {
            return DefaultItemResponse.builder()
                    .id(e.getId())
                    .category(e.getCategory())
                    .itemName(e.getItemName())
                    .checkMethod(e.getCheckMethod())
                    .checkStandard(e.getCheckStandard())
                    .sortOrder(e.getSortOrder())
                    .build();
        }
    }

    @Getter
    @Builder
    public static class DefaultCheckResponse {
        private boolean alreadyLoaded;
        private int existingCount;
        private int defaultCount;
    }

    @Getter
    @Builder
    public static class ItemListResponse {
        private long total;
        private List<ItemResponse> items;
    }
}
