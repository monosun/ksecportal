package com.monosun.secportal.risk.repository;

import com.monosun.secportal.risk.entity.RiskAssessmentRound;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface RiskAssessmentRoundRepository extends JpaRepository<RiskAssessmentRound, Long> {

    List<RiskAssessmentRound> findByYearOrderByRoundNoAsc(int year);

    @Query("SELECT DISTINCT r.year FROM RiskAssessmentRound r ORDER BY r.year DESC")
    List<Integer> findDistinctYears();

    boolean existsByYearAndRoundNo(int year, int roundNo);
}
