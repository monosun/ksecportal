package com.monosun.secportal.monthlycheck.repository;

import com.monosun.secportal.monthlycheck.entity.MonthlyCheckEvidence;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MonthlyCheckEvidenceRepository extends JpaRepository<MonthlyCheckEvidence, Long> {
    List<MonthlyCheckEvidence> findByCheckItemIdOrderByCreatedAtDesc(Long checkItemId);
}
