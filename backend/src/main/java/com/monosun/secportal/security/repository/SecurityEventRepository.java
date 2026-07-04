package com.monosun.secportal.security.repository;

import com.monosun.secportal.security.entity.SecurityEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface SecurityEventRepository extends JpaRepository<SecurityEvent, Long> {
    Page<SecurityEvent> findByIntegrationIdOrderByDetectedAtDesc(Long integrationId, Pageable pageable);
    long countByIntegrationId(Long integrationId);
    long countByCreatedAtAfter(LocalDateTime since);
    long countBySeverityAndCreatedAtAfter(SecurityEvent.EventSeverity severity, LocalDateTime since);
    List<SecurityEvent> findTop5ByOrderByDetectedAtDesc();
}
