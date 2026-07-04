package com.monosun.secportal.phishing.repository;

import com.monosun.secportal.phishing.entity.PhishingCampaignTarget;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PhishingCampaignTargetRepository extends JpaRepository<PhishingCampaignTarget, Long> {
    Optional<PhishingCampaignTarget> findByTrackingToken(String token);
}
