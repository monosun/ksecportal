package com.monosun.secportal.privacy.repository;

import com.monosun.secportal.privacy.entity.PrivacyDpia;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PrivacyDpiaRepository extends JpaRepository<PrivacyDpia, Long> {

    List<PrivacyDpia> findAllByOrderByAssessmentDateDesc();

    long countByStatus(PrivacyDpia.Status status);

    long countByRiskLevel(PrivacyDpia.RiskLevel riskLevel);
}
