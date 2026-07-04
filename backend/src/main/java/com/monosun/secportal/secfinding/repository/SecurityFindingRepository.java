package com.monosun.secportal.secfinding.repository;

import com.monosun.secportal.secfinding.entity.SecurityFinding;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SecurityFindingRepository extends JpaRepository<SecurityFinding, Long> {

    @Query("SELECT f FROM SecurityFinding f WHERE " +
           "(:year IS NULL OR f.year = :year) AND " +
           "(:status IS NULL OR f.status = :status) AND " +
           "(:riskLevel IS NULL OR f.riskLevel = :riskLevel) AND " +
           "(:auditType IS NULL OR f.auditType = :auditType) AND " +
           "(:keyword IS NULL OR LOWER(f.findingSummary) LIKE LOWER(CONCAT('%',:keyword,'%')) " +
           " OR LOWER(f.requirementCode) LIKE LOWER(CONCAT('%',:keyword,'%')))")
    Page<SecurityFinding> findWithFilters(
            @Param("year") Integer year,
            @Param("status") SecurityFinding.Status status,
            @Param("riskLevel") SecurityFinding.RiskLevel riskLevel,
            @Param("auditType") SecurityFinding.AuditType auditType,
            @Param("keyword") String keyword,
            Pageable pageable);

    @Query("SELECT DISTINCT f.year FROM SecurityFinding f ORDER BY f.year DESC")
    List<Integer> findDistinctYears();
}
