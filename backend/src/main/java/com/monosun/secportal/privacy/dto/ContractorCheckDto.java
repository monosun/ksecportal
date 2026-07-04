package com.monosun.secportal.privacy.dto;

import com.monosun.secportal.privacy.entity.ContractorCheck;
import com.monosun.secportal.privacy.entity.ContractorCheckResult;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public class ContractorCheckDto {

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CheckRequest {
        @NotNull
        private Long contractorId;
        @NotNull
        private Integer checkYear;
        private LocalDate checkDate;
        private String inspector;
        private String status;
        private String notes;
    }

    @Getter
    @Builder
    public static class CheckResponse {
        private Long id;
        private Long contractorId;
        private String contractorName;
        private int checkYear;
        private LocalDate checkDate;
        private String inspector;
        private String status;
        private String notes;
        private int totalItems;
        private int passCount;
        private int failCount;
        private int naCount;
        private int notCheckedCount;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        public static CheckResponse from(ContractorCheck c) {
            return CheckResponse.builder()
                    .id(c.getId())
                    .contractorId(c.getContractor().getId())
                    .contractorName(c.getContractor().getName())
                    .checkYear(c.getCheckYear())
                    .checkDate(c.getCheckDate())
                    .inspector(c.getInspector())
                    .status(c.getStatus().name())
                    .notes(c.getNotes())
                    .createdAt(c.getCreatedAt())
                    .updatedAt(c.getUpdatedAt())
                    .build();
        }

        public static CheckResponse fromWithStats(ContractorCheck c) {
            long pass = c.getResults().stream().filter(r -> r.getResult() == ContractorCheckResult.Result.PASS).count();
            long fail = c.getResults().stream().filter(r -> r.getResult() == ContractorCheckResult.Result.FAIL).count();
            long na   = c.getResults().stream().filter(r -> r.getResult() == ContractorCheckResult.Result.NA).count();
            long notChecked = c.getResults().stream().filter(r -> r.getResult() == ContractorCheckResult.Result.NOT_CHECKED).count();
            return CheckResponse.builder()
                    .id(c.getId())
                    .contractorId(c.getContractor().getId())
                    .contractorName(c.getContractor().getName())
                    .checkYear(c.getCheckYear())
                    .checkDate(c.getCheckDate())
                    .inspector(c.getInspector())
                    .status(c.getStatus().name())
                    .notes(c.getNotes())
                    .totalItems(c.getResults().size())
                    .passCount((int) pass)
                    .failCount((int) fail)
                    .naCount((int) na)
                    .notCheckedCount((int) notChecked)
                    .createdAt(c.getCreatedAt())
                    .updatedAt(c.getUpdatedAt())
                    .build();
        }
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ResultRequest {
        @NotNull
        private Long checkItemId;
        @NotNull
        private String result;
        private String notes;
    }

    @Getter
    @Builder
    public static class ResultResponse {
        private Long id;
        private Long checkId;
        private Long checkItemId;
        private String checkItemCategory;
        private String checkItemName;
        private String checkMethod;
        private String checkStandard;
        private boolean required;
        private int sortOrder;
        private String result;
        private String notes;
        private LocalDateTime updatedAt;

        public static ResultResponse from(ContractorCheckResult r) {
            return ResultResponse.builder()
                    .id(r.getId())
                    .checkId(r.getCheck().getId())
                    .checkItemId(r.getCheckItem().getId())
                    .checkItemCategory(r.getCheckItem().getCategory())
                    .checkItemName(r.getCheckItem().getItemName())
                    .checkMethod(r.getCheckItem().getCheckMethod())
                    .checkStandard(r.getCheckItem().getCheckStandard())
                    .required(r.getCheckItem().isRequired())
                    .sortOrder(r.getCheckItem().getSortOrder())
                    .result(r.getResult().name())
                    .notes(r.getNotes())
                    .updatedAt(r.getUpdatedAt())
                    .build();
        }
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BulkResultRequest {
        @jakarta.validation.constraints.NotBlank
        private String result;
    }

    @Getter
    @Builder
    public static class CheckDetailResponse {
        private CheckResponse check;
        private List<ResultResponse> results;
    }
}
