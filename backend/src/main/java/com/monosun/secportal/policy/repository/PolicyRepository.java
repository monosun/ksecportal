package com.monosun.secportal.policy.repository;

import com.monosun.secportal.policy.entity.Policy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PolicyRepository extends JpaRepository<Policy, Long> {

    @Query("SELECT p FROM Policy p WHERE " +
           "(:status IS NULL OR p.status = :status) AND " +
           "(:category IS NULL OR p.category = :category) AND " +
           "(:keyword IS NULL OR LOWER(p.title) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<Policy> search(
            @Param("status") Policy.Status status,
            @Param("category") Policy.Category category,
            @Param("keyword") String keyword,
            Pageable pageable);

    /**
     * 조문 검색 필터(지침 &gt; 장 드롭다운)용 경량 조회 — 본문(LONGTEXT)은 싣지 않는다.
     * 반환 배열: [0]=id, [1]=지침명, [2]=장 번호, [3]=장 표기, [4]=장 제목
     */
    @Query("SELECT p.id, p.guidelineName, p.chapterNo, p.chapterLabel, p.chapterTitle FROM Policy p " +
           "ORDER BY p.guidelineName ASC, " +
           "CASE WHEN p.chapterNo IS NULL THEN 1 ELSE 0 END ASC, " +   // 부칙 등 번호 없는 장은 맨 뒤
           "p.chapterNo ASC, p.id ASC")
    List<Object[]> findChapterFacets();
}
