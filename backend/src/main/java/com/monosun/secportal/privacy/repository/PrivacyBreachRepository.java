package com.monosun.secportal.privacy.repository;

import com.monosun.secportal.privacy.entity.PrivacyBreach;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PrivacyBreachRepository extends JpaRepository<PrivacyBreach, Long> {

    List<PrivacyBreach> findAllByOrderByDetectedAtDesc();

    long countByStatus(PrivacyBreach.Status status);
}
