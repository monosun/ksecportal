package com.monosun.secportal.glossary.dto;

import com.monosun.secportal.glossary.entity.GlossaryTerm;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

public class GlossaryDto {

    @Getter @Builder
    public static class TermResponse {
        private Long id;
        private String name;
        private String nameEn;
        private String abbreviation;
        private String category;
        private String definition;
        private String keywords;
        private Integer sortOrder;
        private boolean active;
        private LocalDateTime updatedAt;

        public static TermResponse from(GlossaryTerm t) {
            return TermResponse.builder()
                    .id(t.getId())
                    .name(t.getName())
                    .nameEn(t.getNameEn())
                    .abbreviation(t.getAbbreviation())
                    .category(t.getCategory())
                    .definition(t.getDefinition())
                    .keywords(t.getKeywords())
                    .sortOrder(t.getSortOrder())
                    .active(t.isActive())
                    .updatedAt(t.getUpdatedAt())
                    .build();
        }
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor
    public static class TermRequest {
        private String name;
        private String nameEn;
        private String abbreviation;
        private String category;
        private String definition;
        private String keywords;
        private Integer sortOrder;
        private Boolean active;
    }

    /** 분류별 용어 수 — 용어집 화면의 분류 필터·현황에 쓴다 */
    @Getter @Builder
    public static class CategoryCount {
        private String category;
        private long count;
    }

    @Getter @Builder
    public static class SummaryResponse {
        private long total;
        private List<CategoryCount> byCategory;
        /** 약어가 있는 용어 수 */
        private long abbreviations;
    }
}
