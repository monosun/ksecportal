package com.monosun.secportal.policy.entity;

import com.monosun.secportal.auth.entity.User;
import com.monosun.secportal.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "policies", indexes = {
        @Index(name = "idx_policies_guideline", columnList = "guideline_name, chapter_no")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Policy extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "LONGTEXT", nullable = false)
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Category category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private Status status = Status.DRAFT;

    @Column(nullable = false)
    @Builder.Default
    private String version = "1.0";

    private LocalDate effectiveDate;

    // ── 문서 구조(지침 > 장) ────────────────────────────────────────────────
    // 제목 "개인정보보호 지침 - 제1장 총칙" 에서 파생되어 저장 시 자동 채워진다.
    // 검색 필터(지침별·장별)에서 LIKE 대신 동등 비교로 쓰기 위한 정규화 컬럼.

    /** 지침명 — 예: "개인정보보호 지침" */
    @Column(name = "guideline_name", length = 200)
    private String guidelineName;

    /** 장 번호 — 예: 1. 부칙처럼 번호가 없는 장은 null */
    @Column(name = "chapter_no")
    private Integer chapterNo;

    /** 장 표기 — 예: "제1장", "부칙" */
    @Column(name = "chapter_label", length = 50)
    private String chapterLabel;

    /** 장 제목 — 예: "총칙" */
    @Column(name = "chapter_title", length = 200)
    private String chapterTitle;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id")
    private User author;

    /** 장 아래 세분화된 조(條) — 본문 파싱으로 동기화된다. */
    @OneToMany(mappedBy = "policy", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC")
    @Builder.Default
    private List<PolicyArticle> articles = new ArrayList<>();

    @OneToMany(mappedBy = "policy", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<PolicyAcknowledgment> acknowledgments = new ArrayList<>();

    public enum Category {
        GENERAL, ACCESS_CONTROL, DATA_PROTECTION, INCIDENT_RESPONSE, NETWORK, PHYSICAL, VENDOR, OTHER
    }

    public enum Status {
        DRAFT, REVIEW, PUBLISHED, ARCHIVED
    }
}
