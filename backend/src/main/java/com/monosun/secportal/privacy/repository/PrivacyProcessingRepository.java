package com.monosun.secportal.privacy.repository;

import com.monosun.secportal.privacy.entity.PrivacyProcessing;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PrivacyProcessingRepository extends JpaRepository<PrivacyProcessing, Long> {

    List<PrivacyProcessing> findAllByOrderByNameAsc();

    long countByStatus(PrivacyProcessing.Status status);
}
