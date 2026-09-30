package com.monosun.secportal.secfinding.repository;

import com.monosun.secportal.secfinding.entity.SecurityFinding;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SecurityFindingRepository extends JpaRepository<SecurityFinding, Long> {

    /**
     * 목록 조회.
     *
     * @param sourceType null 이면 전체. {@code MANUAL} 은 출처 컬럼이 비어 있는 구 데이터도 함께 본다
     *                   (ddl-auto 로 컬럼이 추가되기 전에 쌓인 행은 값이 NULL 이다).
     */
    @Query("SELECT f FROM SecurityFinding f WHERE " +
           "(:year IS NULL OR f.year = :year) AND " +
           "(:status IS NULL OR f.status = :status) AND " +
           "(:riskLevel IS NULL OR f.riskLevel = :riskLevel) AND " +
           "(:auditType IS NULL OR f.auditType = :auditType) AND " +
           "(:sourceType IS NULL OR f.sourceType = :sourceType " +
           " OR (:includeNullSource = TRUE AND f.sourceType IS NULL)) AND " +
           "(:keyword IS NULL OR LOWER(f.findingSummary) LIKE LOWER(CONCAT('%',:keyword,'%')) " +
           " OR LOWER(f.requirementCode) LIKE LOWER(CONCAT('%',:keyword,'%')))")
    Page<SecurityFinding> findWithFilters(
            @Param("year") Integer year,
            @Param("status") SecurityFinding.Status status,
            @Param("riskLevel") SecurityFinding.RiskLevel riskLevel,
            @Param("auditType") SecurityFinding.AuditType auditType,
            @Param("sourceType") SecurityFinding.SourceType sourceType,
            @Param("includeNullSource") boolean includeNullSource,
            @Param("keyword") String keyword,
            Pageable pageable);

    @Query("SELECT DISTINCT f.year FROM SecurityFinding f ORDER BY f.year DESC")
    List<Integer> findDistinctYears();

    /** ISMS 결함 1건에 대응하는 결함사항(가져온 건) */
    Optional<SecurityFinding> findBySourceDefectId(Long sourceDefectId);

    /** 이미 가져와 있는 ISMS 결함 id 목록 — 기동 시 누락분 보충에 쓴다 */
    @Query("SELECT f.sourceDefectId FROM SecurityFinding f WHERE f.sourceDefectId IS NOT NULL")
    List<Long> findAllSourceDefectIds();
}
