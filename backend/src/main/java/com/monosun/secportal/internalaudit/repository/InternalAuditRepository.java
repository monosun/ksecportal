package com.monosun.secportal.internalaudit.repository;

import com.monosun.secportal.internalaudit.entity.InternalAudit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface InternalAuditRepository extends JpaRepository<InternalAudit, Long> {

    List<InternalAudit> findByYearOrderByCreatedAtDesc(int year);

    @Query("SELECT DISTINCT a.year FROM InternalAudit a ORDER BY a.year DESC")
    List<Integer> findDistinctYears();
}
