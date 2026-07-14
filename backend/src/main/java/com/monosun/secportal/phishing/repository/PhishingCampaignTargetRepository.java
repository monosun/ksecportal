package com.monosun.secportal.phishing.repository;

import com.monosun.secportal.phishing.entity.PhishingCampaignTarget;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface PhishingCampaignTargetRepository extends JpaRepository<PhishingCampaignTarget, Long> {
    Optional<PhishingCampaignTarget> findByTrackingToken(String token);

    /** 발송이 시도된(결과가 기록된) 대상들을 캠페인·수신자와 함께 최신순으로 조회 — 발송 처리 결과 로그용 */
    @Query("SELECT ct FROM PhishingCampaignTarget ct " +
           "JOIN FETCH ct.campaign c JOIN FETCH ct.target t " +
           "WHERE ct.sendStatus IS NOT NULL ORDER BY ct.id DESC")
    List<PhishingCampaignTarget> findSendLogs();
}
