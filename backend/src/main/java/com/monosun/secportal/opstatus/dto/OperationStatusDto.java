package com.monosun.secportal.opstatus.dto;

import com.monosun.secportal.opstatus.entity.OperationStatusItem;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

public class OperationStatusDto {

    @Getter @Builder
    public static class ItemResponse {
        private Long id;
        private Integer year;
        private String type;
        private String category;
        private String name;
        private String cycle;
        private String deliverable;
        private String owner;
        private String manager;
        private String note;
        private Integer sortOrder;
        /** 12개 원소, index 0 = 1월 */
        private List<Boolean> plan;
        private List<Boolean> done;
        private int planCount;
        private int doneCount;
        private LocalDateTime updatedAt;

        public static ItemResponse from(OperationStatusItem i) {
            return ItemResponse.builder()
                    .id(i.getId())
                    .year(i.getYear())
                    .type(i.getType().name())
                    .category(i.getCategory())
                    .name(i.getName())
                    .cycle(i.getCycle())
                    .deliverable(i.getDeliverable())
                    .owner(i.getOwner())
                    .manager(i.getManager())
                    .note(i.getNote())
                    .sortOrder(i.getSortOrder())
                    .plan(toList(i.getPlanMonths()))
                    .done(toList(i.getDoneMonths()))
                    .planCount(OperationStatusItem.count(i.getPlanMonths()))
                    .doneCount(OperationStatusItem.count(i.getDoneMonths()))
                    .updatedAt(i.getUpdatedAt())
                    .build();
        }

        private static List<Boolean> toList(Integer mask) {
            return java.util.stream.IntStream.rangeClosed(1, 12)
                    .mapToObj(m -> OperationStatusItem.has(mask, m))
                    .toList();
        }
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor
    public static class ItemRequest {
        private Integer year;
        private String type;
        private String category;
        private String name;
        private String cycle;
        private String deliverable;
        private String owner;
        private String manager;
        private String note;
        private Integer sortOrder;
        private List<Boolean> plan;
        private List<Boolean> done;
    }

    /** 월 단위 토글 — field 는 PLAN 또는 DONE */
    @Getter @Setter @NoArgsConstructor @AllArgsConstructor
    public static class MonthToggleRequest {
        private String field;
        private Integer month;
        private Boolean value;
    }

    // ── 기본 항목 마스터 (코드 관리) ────────────────────────────────────────

    @Getter @Builder
    public static class DefaultResponse {
        private Long id;
        private String type;
        private String category;
        private String name;
        private String cycle;
        private String deliverable;
        private String owner;
        private String manager;
        private String note;
        private Integer sortOrder;
        private boolean active;
        private List<Boolean> plan;
        private int planCount;

        public static DefaultResponse from(com.monosun.secportal.opstatus.entity.OperationStatusDefault d) {
            return DefaultResponse.builder()
                    .id(d.getId())
                    .type(d.getType().name())
                    .category(d.getCategory())
                    .name(d.getName())
                    .cycle(d.getCycle())
                    .deliverable(d.getDeliverable())
                    .owner(d.getOwner())
                    .manager(d.getManager())
                    .note(d.getNote())
                    .sortOrder(d.getSortOrder())
                    .active(d.isActive())
                    .plan(java.util.stream.IntStream.rangeClosed(1, 12)
                            .mapToObj(m -> OperationStatusItem.has(d.getPlanMonths(), m))
                            .toList())
                    .planCount(OperationStatusItem.count(d.getPlanMonths()))
                    .build();
        }
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor
    public static class DefaultRequest {
        private String type;
        private String category;
        private String name;
        private String cycle;
        private String deliverable;
        private String owner;
        private String manager;
        private String note;
        private Integer sortOrder;
        private Boolean active;
        private List<Boolean> plan;
    }

    /** 유형별 이행 집계 */
    @Getter @Builder
    public static class TypeSummary {
        private String type;
        private int items;
        /** 계획된 (항목 × 월) 칸 수 */
        private int planned;
        /** 그중 이행 완료로 표시된 칸 수 */
        private int done;
        /** 계획 없이 이행만 표시된 칸 수 (계획 외 수행) */
        private int unplannedDone;
        private int rate;
        /** 월별 계획 / 이행 건수 (index 0 = 1월) */
        private List<Integer> plannedByMonth;
        private List<Integer> doneByMonth;
    }

    @Getter @Builder
    public static class SummaryResponse {
        private int year;
        private List<TypeSummary> byType;
        private int totalItems;
        private int totalPlanned;
        private int totalDone;
        private int rate;
    }
}
