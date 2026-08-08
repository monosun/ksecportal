package com.monosun.secportal.threat.repository;

import com.monosun.secportal.threat.entity.ThreatDefault;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ThreatDefaultRepository extends JpaRepository<ThreatDefault, Long> {

    List<ThreatDefault> findAllByOrderByIdAsc();

    /** 유니크 제약 uq_threat_defaults_name_type_cat_risk 와 동일한 키 조합 */
    boolean existsByNameAndTypeAndCategoryAndLikelihoodAndImpact(
            String name, String type, String category, int likelihood, int impact);

    Optional<ThreatDefault> findByNameAndTypeAndCategoryAndLikelihoodAndImpact(
            String name, String type, String category, int likelihood, int impact);
}
