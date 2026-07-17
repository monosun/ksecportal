package com.monosun.secportal.privacy.repository;

import com.monosun.secportal.privacy.entity.PrivacyRetention;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface PrivacyRetentionRepository extends JpaRepository<PrivacyRetention, Long> {

    List<PrivacyRetention> findAllByOrderByExpiryDateAsc();

    long countByStatus(PrivacyRetention.Status status);

    /** 지정일 이전에 만료예정이며 아직 파기되지 않은 항목 — 만료예정 알림용 */
    List<PrivacyRetention> findByExpiryDateLessThanEqualAndStatusNotOrderByExpiryDateAsc(
            LocalDate until, PrivacyRetention.Status excludedStatus);
}
