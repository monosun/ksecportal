package com.monosun.secportal.secdoc.repository;

import com.monosun.secportal.secdoc.entity.SecDoc;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SecDocRepository extends JpaRepository<SecDoc, Long> {

    @Query("SELECT d FROM SecDoc d WHERE d.latest = true " +
           "AND (:category IS NULL OR d.category = :category) " +
           "AND (:keyword IS NULL OR " +
           "  (:field = 'TITLE'       AND d.title        LIKE %:keyword%) OR " +
           "  (:field = 'DESCRIPTION' AND d.description  LIKE %:keyword%) OR " +
           "  (:field = 'FILENAME'    AND d.fileName     LIKE %:keyword%) OR " +
           "  (:field = 'VERSION'     AND d.version      LIKE %:keyword%) OR " +
           "  (:field = 'ORG'         AND d.producingOrg LIKE %:keyword%) OR " +
           "  (:field = 'ALL' AND (d.title LIKE %:keyword% OR d.description LIKE %:keyword% " +
           "    OR d.fileName LIKE %:keyword% OR d.version LIKE %:keyword% OR d.producingOrg LIKE %:keyword%))) " +
           "ORDER BY d.createdAt DESC")
    Page<SecDoc> findLatest(@Param("category") SecDoc.Category category,
                             @Param("keyword") String keyword,
                             @Param("field") String field,
                             Pageable pageable);

    List<SecDoc> findByDocumentKeyOrderByCreatedAtDesc(String documentKey);

    void deleteByDocumentKey(String documentKey);
}
