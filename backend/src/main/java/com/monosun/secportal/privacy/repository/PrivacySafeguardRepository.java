package com.monosun.secportal.privacy.repository;

import com.monosun.secportal.privacy.entity.PrivacySafeguard;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface PrivacySafeguardRepository extends JpaRepository<PrivacySafeguard, Long> {

    List<PrivacySafeguard> findAllByOrderByCheckDateDesc();

    long countByStatus(PrivacySafeguard.Status status);

    /** 보호조치 유형별 건수 — [SafeguardType, count] */
    @Query("SELECT s.safeguardType, COUNT(s) FROM PrivacySafeguard s GROUP BY s.safeguardType")
    List<Object[]> countGroupByType();
}
