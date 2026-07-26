package com.monosun.secportal.relatedsite.repository;

import com.monosun.secportal.relatedsite.entity.RelatedSite;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RelatedSiteRepository extends JpaRepository<RelatedSite, Long> {

    List<RelatedSite> findAllByOrderBySortOrderAscIdAsc();

    List<RelatedSite> findByActiveTrueOrderBySortOrderAscIdAsc();

    boolean existsByUrlIgnoreCase(String url);

    Optional<RelatedSite> findFirstByUrlIgnoreCase(String url);
}
