package com.monosun.secportal.privacy.repository;

import com.monosun.secportal.privacy.entity.PrivacyConsentVersion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PrivacyConsentVersionRepository extends JpaRepository<PrivacyConsentVersion, Long> {

    List<PrivacyConsentVersion> findByConsentIdOrderByIdDesc(Long consentId);

    void deleteByConsentId(Long consentId);
}
