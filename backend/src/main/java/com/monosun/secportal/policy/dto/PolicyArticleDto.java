package com.monosun.secportal.policy.dto;

import com.monosun.secportal.policy.entity.Policy;
import com.monosun.secportal.policy.entity.PolicyArticle;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

public class PolicyArticleDto {

    /** 검색 결과 한 건 — 지침 &gt; 장 &gt; 조 경로를 함께 실어 보낸다. */
    @Getter
    @Builder
    public static class Response {
        private Long id;
        private Long policyId;
        private String policyTitle;
        private String guidelineName;
        private Integer chapterNo;
        private String chapterLabel;
        private String chapterTitle;
        private Integer articleNo;
        private Integer articleSubNo;
        private String articleLabel;
        private String title;
        private String displayName;
        /** "&lt;개정 2024.5.28&gt;" 같은 조 꼬리말 */
        private String note;
        private String content;
        private String category;
        private String status;
        private String version;

        public static Response from(PolicyArticle a) {
            Policy p = a.getPolicy();
            return Response.builder()
                    .id(a.getId())
                    .policyId(p.getId())
                    .policyTitle(p.getTitle())
                    .guidelineName(p.getGuidelineName())
                    .chapterNo(p.getChapterNo())
                    .chapterLabel(p.getChapterLabel())
                    .chapterTitle(p.getChapterTitle())
                    .articleNo(a.getArticleNo())
                    .articleSubNo(a.getArticleSubNo())
                    .articleLabel(a.getArticleLabel())
                    .title(a.getTitle())
                    .displayName(a.getDisplayName())
                    .note(a.getNote())
                    .content(a.getContent())
                    .category(p.getCategory().name())
                    .status(p.getStatus().name())
                    .version(p.getVersion())
                    .build();
        }
    }

    /** 검색 필터 드롭다운용 — 지침 목록과 그 아래 장 목록 */
    @Getter
    @Builder
    public static class Facets {
        private List<Guideline> guidelines;
        private int totalArticles;
    }

    @Getter
    @Builder
    public static class Guideline {
        private String name;
        private List<Chapter> chapters;
    }

    @Getter
    @Builder
    public static class Chapter {
        private Long policyId;
        private Integer chapterNo;
        private String chapterLabel;
        private String chapterTitle;
        /** 드롭다운 표시용 — "제1장 총칙" */
        private String label;
    }
}
