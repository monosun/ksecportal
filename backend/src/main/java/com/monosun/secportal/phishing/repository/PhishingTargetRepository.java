package com.monosun.secportal.phishing.repository;

import com.monosun.secportal.phishing.entity.PhishingTarget;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PhishingTargetRepository extends JpaRepository<PhishingTarget, Long> {
    List<PhishingTarget> findAllByOrderByCreatedAtDesc();
    List<PhishingTarget> findAllByActiveTrue();
    boolean existsByEmail(String email);
}
