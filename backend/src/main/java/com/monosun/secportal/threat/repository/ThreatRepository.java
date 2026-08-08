package com.monosun.secportal.threat.repository;

import com.monosun.secportal.threat.entity.Threat;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ThreatRepository extends JpaRepository<Threat, Long> {

    List<Threat> findAllByOrderByCreatedAtDesc();

    boolean existsByName(String name);

    /** 기본 항목 복사 시 동일 항목 판별 — 위협명이 중복될 수 있으므로 위험도까지 함께 본다. */
    boolean existsByNameAndTypeAndCategoryAndLikelihoodAndImpact(
            String name, String type, String category, int likelihood, int impact);
}
