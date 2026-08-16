package com.monosun.secportal.policy.dto;

import com.monosun.secportal.policy.entity.Policy;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class PolicyDto {

    @Getter
    public static class CreateRequest {
        @NotBlank
        private String title;
        @NotBlank
        private String content;
        @NotNull
        private Policy.Category category;
        private String version;
        private LocalDate effectiveDate;
    }

    @Getter
    public static class UpdateRequest {
        private String title;
        private String content;
        private Policy.Category category;
        private Policy.Status status;
        private String version;
        private LocalDate effectiveDate;
    }

    @Getter
    @Builder
    public static class Response {
        private Long id;
        private String title;
        private String content;
        private String category;
        private String status;
        private String version;
        private LocalDate effectiveDate;
        private String authorName;
        private Long authorId;
        private String guidelineName;
        private Integer chapterNo;
        private String chapterLabel;
        private String chapterTitle;
        private long acknowledgmentCount;
        private boolean acknowledgedByMe;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        public static Response from(Policy policy, long ackCount, boolean acknowledgedByMe) {
            return Response.builder()
                    .id(policy.getId())
                    .title(policy.getTitle())
                    .content(policy.getContent())
                    .guidelineName(policy.getGuidelineName())
                    .chapterNo(policy.getChapterNo())
                    .chapterLabel(policy.getChapterLabel())
                    .chapterTitle(policy.getChapterTitle())
                    .category(policy.getCategory().name())
                    .status(policy.getStatus().name())
                    .version(policy.getVersion())
                    .effectiveDate(policy.getEffectiveDate())
                    .authorName(policy.getAuthor().getName())
                    .authorId(policy.getAuthor().getId())
                    .acknowledgmentCount(ackCount)
                    .acknowledgedByMe(acknowledgedByMe)
                    .createdAt(policy.getCreatedAt())
                    .updatedAt(policy.getUpdatedAt())
                    .build();
        }
    }

    @Getter
    @Builder
    public static class Summary {
        private Long id;
        private String title;
        private String category;
        private String status;
        private String version;
        private LocalDate effectiveDate;
        private String authorName;
        private String guidelineName;
        private Integer chapterNo;
        private String chapterLabel;
        private String chapterTitle;
        /** 이 장에 세분화 등록된 조 개수 */
        private long articleCount;
        private LocalDateTime updatedAt;

        public static Summary from(Policy policy, long articleCount) {
            return Summary.builder()
                    .id(policy.getId())
                    .title(policy.getTitle())
                    .category(policy.getCategory().name())
                    .status(policy.getStatus().name())
                    .version(policy.getVersion())
                    .effectiveDate(policy.getEffectiveDate())
                    .authorName(policy.getAuthor().getName())
                    .guidelineName(policy.getGuidelineName())
                    .chapterNo(policy.getChapterNo())
                    .chapterLabel(policy.getChapterLabel())
                    .chapterTitle(policy.getChapterTitle())
                    .articleCount(articleCount)
                    .updatedAt(policy.getUpdatedAt())
                    .build();
        }
    }
}
