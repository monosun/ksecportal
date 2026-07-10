package com.monosun.secportal.risk.repository;

import com.monosun.secportal.risk.entity.RiskAssessment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface RiskAssessmentRepository extends JpaRepository<RiskAssessment, Long> {

    List<RiskAssessment> findByRoundIdOrderByCreatedAtAsc(Long roundId);

    @Query("SELECT a FROM RiskAssessment a WHERE a.roundId = :roundId " +
           "AND (:assetName IS NULL OR LOWER(a.assetName) LIKE LOWER(CONCAT('%', :assetName, '%'))) " +
           "AND (:assetType IS NULL OR a.assetType = :assetType) " +
           "AND (:threatName IS NULL OR LOWER(a.threatName) LIKE LOWER(CONCAT('%', :threatName, '%'))) " +
           "AND (:threatType IS NULL OR a.threatType = :threatType) " +
           "AND (:riskGrade IS NULL OR a.riskGrade = :riskGrade) " +
           "ORDER BY a.createdAt ASC")
    List<RiskAssessment> search(
            @Param("roundId") Long roundId,
            @Param("assetName") String assetName,
            @Param("assetType") String assetType,
            @Param("threatName") String threatName,
            @Param("threatType") String threatType,
            @Param("riskGrade") RiskAssessment.Grade riskGrade);

    long countByRoundId(Long roundId);

    void deleteByRoundId(Long roundId);

    /** 처리방법이 특정 값인 항목 (위험 처리 계획 — '감소' 항목 조회용) */
    List<RiskAssessment> findByRoundIdAndTreatment(Long roundId, RiskAssessment.Treatment treatment);
}
