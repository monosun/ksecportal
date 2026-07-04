package com.monosun.secportal.phishing.repository;

import com.monosun.secportal.phishing.entity.PhishingTemplate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PhishingTemplateRepository extends JpaRepository<PhishingTemplate, Long> {
    List<PhishingTemplate> findAllByOrderByCreatedAtDesc();
}
