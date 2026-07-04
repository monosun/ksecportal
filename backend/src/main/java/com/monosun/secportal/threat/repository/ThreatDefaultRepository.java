package com.monosun.secportal.threat.repository;

import com.monosun.secportal.threat.entity.ThreatDefault;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ThreatDefaultRepository extends JpaRepository<ThreatDefault, Long> {

    List<ThreatDefault> findAllByOrderByIdAsc();

    boolean existsByNameAndTypeAndCategory(String name, String type, String category);

    Optional<ThreatDefault> findByNameAndTypeAndCategory(String name, String type, String category);
}
