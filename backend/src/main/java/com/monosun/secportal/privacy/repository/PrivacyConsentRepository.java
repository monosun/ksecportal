package com.monosun.secportal.privacy.repository;

import com.monosun.secportal.privacy.entity.PrivacyConsent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PrivacyConsentRepository extends JpaRepository<PrivacyConsent, Long> {

    List<PrivacyConsent> findAllByOrderByTitleAscVersionDesc();

    long countByStatus(PrivacyConsent.Status status);
}
