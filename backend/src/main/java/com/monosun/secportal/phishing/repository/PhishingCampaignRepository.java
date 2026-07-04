package com.monosun.secportal.phishing.repository;

import com.monosun.secportal.phishing.entity.PhishingCampaign;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface PhishingCampaignRepository extends JpaRepository<PhishingCampaign, Long> {

    @Query("SELECT c FROM PhishingCampaign c LEFT JOIN FETCH c.template ORDER BY c.createdAt DESC")
    List<PhishingCampaign> findAllWithTemplate();

    @Query("SELECT c FROM PhishingCampaign c LEFT JOIN FETCH c.template LEFT JOIN FETCH c.campaignTargets WHERE c.id = :id")
    Optional<PhishingCampaign> findByIdWithTargets(Long id);
}
