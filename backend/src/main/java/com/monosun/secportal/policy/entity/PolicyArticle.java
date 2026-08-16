package com.monosun.secportal.policy.entity;

import com.monosun.secportal.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

/**
 * 보안정책(장, 章) 아래로 세분화된 조(條).
 * 정책 본문의 "### 제N조(제목)" 머리글을 기준으로 분해되어 저장되며,
 * 지침 > 장 > 조 계층 검색의 최소 단위가 된다.
 */
@Entity
@Table(name = "policy_articles", indexes = {
        @Index(name = "idx_policy_article_policy", columnList = "policy_id"),
        @Index(name = "idx_policy_article_no", columnList = "article_no")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PolicyArticle extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "policy_id", nullable = false)
    private Policy policy;

    /** 조 번호 — 예: 1. 전문(前文)처럼 번호가 없는 구간은 null */
    @Column(name = "article_no")
    private Integer articleNo;

    /** 조 가지번호 — "제3조의2" 의 2. 없으면 null */
    @Column(name = "article_sub_no")
    private Integer articleSubNo;

    /** 조 표기 — 예: "제1조", "제3조의2", "전문" */
    @Column(name = "article_label", length = 50, nullable = false)
    private String articleLabel;

    /** 조 제목 — 예: "목적". 괄호 제목이 없으면 빈 문자열 */
    @Column(name = "title", length = 300, nullable = false)
    @Builder.Default
    private String title = "";

    /** 조 제목 뒤 꼬리말 — 예: "&lt;개정 2024.5.28&gt;". 없으면 null */
    @Column(name = "note", length = 200)
    private String note;

    /** 조 본문 */
    @Column(name = "content", columnDefinition = "LONGTEXT")
    private String content;

    /** 장 안에서의 노출 순서 */
    @Column(name = "sort_order", nullable = false)
    @Builder.Default
    private Integer sortOrder = 0;

    /** "제1조(목적)" 형태의 표시용 이름 */
    @Transient
    public String getDisplayName() {
        return (title == null || title.isBlank()) ? articleLabel : articleLabel + "(" + title + ")";
    }
}
