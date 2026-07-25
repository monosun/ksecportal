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

    /** 엑셀 일괄 등록 결과 */
    @Getter @Builder
    public static class BulkResult {
        private int total;          // 읽어들인 데이터 행 수
        private int success;        // 신규 등록 건수
        private int updated;        // 기존 용어를 갱신한 건수
        private int skipped;        // 동일 내용이라 건너뛴 건수
        private int failed;
        private List<RowError> errors;

        @Getter @AllArgsConstructor
        public static class RowError {
            private int row;
            private String message;
        }
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
