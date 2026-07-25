package com.monosun.secportal.glossary.repository;

import com.monosun.secportal.glossary.entity.GlossaryTerm;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface GlossaryTermRepository extends JpaRepository<GlossaryTerm, Long> {

    List<GlossaryTerm> findAllByOrderBySortOrderAscIdAsc();

    List<GlossaryTerm> findByActiveTrueOrderBySortOrderAscIdAsc();

    boolean existsByNameIgnoreCase(String name);

    java.util.Optional<GlossaryTerm> findFirstByNameIgnoreCase(String name);

    @Query("SELECT DISTINCT t.category FROM GlossaryTerm t WHERE t.category IS NOT NULL AND t.active = true ORDER BY t.category")
    List<String> findCategories();
}
