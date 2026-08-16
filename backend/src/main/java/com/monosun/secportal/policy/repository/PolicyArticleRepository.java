package com.monosun.secportal.policy.repository;

import com.monosun.secportal.policy.entity.Policy;
import com.monosun.secportal.policy.entity.PolicyArticle;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface PolicyArticleRepository extends JpaRepository<PolicyArticle, Long> {

    List<PolicyArticle> findByPolicyIdOrderBySortOrderAsc(Long policyId);

    /** 목록 화면의 "조 N개" 표시용 — 반환 배열: [0]=정책 id, [1]=조 개수 */
    @Query("SELECT a.policy.id, COUNT(a) FROM PolicyArticle a WHERE a.policy.id IN :policyIds GROUP BY a.policy.id")
    List<Object[]> countByPolicyIds(@Param("policyIds") Collection<Long> policyIds);

    /**
     * 지침 &gt; 장 &gt; 조 계층 검색.
     * 검색어는 호출 측에서 소문자 + '%' 로 감싸 넘기고, 검색 범위는 in* 플래그로 지정한다.
     */
    @Query(value = """
            SELECT a FROM PolicyArticle a JOIN a.policy p
            WHERE (:guideline IS NULL OR p.guidelineName = :guideline)
              AND (:chapterNo IS NULL OR p.chapterNo = :chapterNo)
              AND (:policyId IS NULL OR p.id = :policyId)
              AND (:articleNo IS NULL OR a.articleNo = :articleNo)
              AND (:status IS NULL OR p.status = :status)
              AND (:category IS NULL OR p.category = :category)
              AND (:keyword IS NULL
                   OR (:inTitle = TRUE AND LOWER(a.title) LIKE :keyword)
                   OR (:inContent = TRUE AND (LOWER(a.content) LIKE :keyword OR LOWER(a.note) LIKE :keyword))
                   OR (:inGuideline = TRUE AND LOWER(p.guidelineName) LIKE :keyword)
                   OR (:inChapter = TRUE AND (LOWER(p.chapterTitle) LIKE :keyword OR LOWER(p.chapterLabel) LIKE :keyword))
                   OR (:inArticleLabel = TRUE AND LOWER(a.articleLabel) LIKE :keyword))
            ORDER BY p.guidelineName ASC,
                     CASE WHEN p.chapterNo IS NULL THEN 1 ELSE 0 END ASC,
                     p.chapterNo ASC, p.id ASC, a.sortOrder ASC
            """,
            countQuery = """
            SELECT COUNT(a) FROM PolicyArticle a JOIN a.policy p
            WHERE (:guideline IS NULL OR p.guidelineName = :guideline)
              AND (:chapterNo IS NULL OR p.chapterNo = :chapterNo)
              AND (:policyId IS NULL OR p.id = :policyId)
              AND (:articleNo IS NULL OR a.articleNo = :articleNo)
              AND (:status IS NULL OR p.status = :status)
              AND (:category IS NULL OR p.category = :category)
              AND (:keyword IS NULL
                   OR (:inTitle = TRUE AND LOWER(a.title) LIKE :keyword)
                   OR (:inContent = TRUE AND (LOWER(a.content) LIKE :keyword OR LOWER(a.note) LIKE :keyword))
                   OR (:inGuideline = TRUE AND LOWER(p.guidelineName) LIKE :keyword)
                   OR (:inChapter = TRUE AND (LOWER(p.chapterTitle) LIKE :keyword OR LOWER(p.chapterLabel) LIKE :keyword))
                   OR (:inArticleLabel = TRUE AND LOWER(a.articleLabel) LIKE :keyword))
            """)
    Page<PolicyArticle> search(
            @Param("guideline") String guideline,
            @Param("chapterNo") Integer chapterNo,
            @Param("policyId") Long policyId,
            @Param("articleNo") Integer articleNo,
            @Param("status") Policy.Status status,
            @Param("category") Policy.Category category,
            @Param("keyword") String keyword,
            @Param("inTitle") boolean inTitle,
            @Param("inContent") boolean inContent,
            @Param("inGuideline") boolean inGuideline,
            @Param("inChapter") boolean inChapter,
            @Param("inArticleLabel") boolean inArticleLabel,
            Pageable pageable);
}
