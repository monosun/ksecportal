package com.monosun.secportal.ismsdefect.repository;

import com.monosun.secportal.ismsdefect.entity.IsmsDefectReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface IsmsDefectReportRepository extends JpaRepository<IsmsDefectReport, Long> {

    Optional<IsmsDefectReport> findByYear(int year);

    @Query("SELECT DISTINCT r.year FROM IsmsDefectReport r ORDER BY r.year DESC")
    List<Integer> findDistinctYears();
}
