package com.monosun.secportal.privacy.repository;

import com.monosun.secportal.privacy.entity.PrivacyProvision;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface PrivacyProvisionRepository extends JpaRepository<PrivacyProvision, Long> {

    List<PrivacyProvision> findAllByOrderByRecipientAsc();

    /** 특정 처리업무에 연계된 제공 목록 — 처리현황 화면에서 제공 정보를 관리할 때 사용 */
    List<PrivacyProvision> findByProcessingIdOrderByRecipientAsc(Long processingId);

    long countByStatus(PrivacyProvision.Status status);

    /** 제공 유형별 건수 — [ProvisionType, count] */
    @Query("SELECT p.provisionType, COUNT(p) FROM PrivacyProvision p GROUP BY p.provisionType")
    List<Object[]> countGroupByType();
}
