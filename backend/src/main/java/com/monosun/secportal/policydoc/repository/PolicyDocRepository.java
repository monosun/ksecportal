package com.monosun.secportal.policydoc.repository;

import com.monosun.secportal.policydoc.entity.PolicyDoc;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PolicyDocRepository extends JpaRepository<PolicyDoc, Long> {

    @Query("SELECT d FROM PolicyDoc d WHERE " +
           "(:category IS NULL OR d.category = :category) " +
           "AND (:status IS NULL OR d.status = :status) " +
           "AND (:keyword IS NULL OR d.title LIKE %:keyword% OR d.docNo LIKE %:keyword% " +
           "  OR d.department LIKE %:keyword% OR d.owner LIKE %:keyword% OR d.description LIKE %:keyword%)")
    Page<PolicyDoc> search(@Param("category") PolicyDoc.Category category,
                           @Param("status") PolicyDoc.Status status,
                           @Param("keyword") String keyword,
                           Pageable pageable);

    @Query("SELECT d.category, COUNT(d) FROM PolicyDoc d WHERE (:status IS NULL OR d.status = :status) GROUP BY d.category")
    List<Object[]> countByCategory(@Param("status") PolicyDoc.Status status);
}
