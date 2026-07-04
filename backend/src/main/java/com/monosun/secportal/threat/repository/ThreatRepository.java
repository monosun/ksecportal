package com.monosun.secportal.threat.repository;

import com.monosun.secportal.threat.entity.Threat;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ThreatRepository extends JpaRepository<Threat, Long> {

    List<Threat> findAllByOrderByCreatedAtDesc();

    boolean existsByName(String name);
}
