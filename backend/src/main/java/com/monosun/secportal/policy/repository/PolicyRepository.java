package com.monosun.secportal.policy.repository;

import com.monosun.secportal.policy.entity.Policy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PolicyRepository extends JpaRepository<Policy, Long> {

    @Query("SELECT p FROM Policy p WHERE " +
           "(:status IS NULL OR p.status = :status) AND " +
           "(:category IS NULL OR p.category = :category) AND " +
           "(:keyword IS NULL OR LOWER(p.title) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<Policy> search(
            @Param("status") Policy.Status status,
            @Param("category") Policy.Category category,
            @Param("keyword") String keyword,
            Pageable pageable);
}
