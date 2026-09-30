package com.monosun.secportal.ismsdefect.repository;

import com.monosun.secportal.ismsdefect.entity.IsmsDefect;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface IsmsDefectRepository extends JpaRepository<IsmsDefect, Long> {

    @Query("SELECT d FROM IsmsDefect d WHERE d.year = :year " +
           "AND (:auditType IS NULL OR d.auditType = :auditType) " +
           "AND (:defectType IS NULL OR d.defectType = :defectType) " +
           "AND (:severity IS NULL OR d.severity = :severity) " +
           "AND (:status IS NULL OR d.status = :status) " +
           "AND (:keyword IS NULL " +
           "     OR LOWER(d.title) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "     OR LOWER(d.content) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "     OR LOWER(d.itemCode) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "     OR LOWER(d.defectNo) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
           "ORDER BY d.sortOrder ASC, d.defectNo ASC, d.id ASC")
    List<IsmsDefect> search(@Param("year") int year,
                            @Param("auditType") IsmsDefect.AuditType auditType,
                            @Param("defectType") IsmsDefect.DefectType defectType,
                            @Param("severity") IsmsDefect.Severity severity,
                            @Param("status") IsmsDefect.Status status,
                            @Param("keyword") String keyword);

    List<IsmsDefect> findByYearOrderBySortOrderAscIdAsc(int year);

    @Query("SELECT DISTINCT d.year FROM IsmsDefect d ORDER BY d.year DESC")
    List<Integer> findDistinctYears();

    long countByYear(int year);

    @Query("SELECT COALESCE(MAX(d.sortOrder), 0) FROM IsmsDefect d WHERE d.year = :year")
    int maxSortOrder(@Param("year") int year);
}
