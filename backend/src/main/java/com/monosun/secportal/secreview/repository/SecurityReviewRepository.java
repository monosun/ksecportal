package com.monosun.secportal.secreview.repository;

import com.monosun.secportal.secreview.entity.SecurityReview;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SecurityReviewRepository extends JpaRepository<SecurityReview, Long> {

    @Query("""
        SELECT r FROM SecurityReview r
        WHERE (:status IS NULL OR r.status = :status)
          AND (:reviewType IS NULL OR r.reviewType = :reviewType)
          AND (:keyword IS NULL
               OR LOWER(r.title) LIKE LOWER(CONCAT('%', :keyword, '%'))
               OR LOWER(r.systemName) LIKE LOWER(CONCAT('%', :keyword, '%'))
               OR LOWER(r.department) LIKE LOWER(CONCAT('%', :keyword, '%')))
        ORDER BY r.createdAt DESC
        """)
    Page<SecurityReview> search(@Param("status") SecurityReview.Status status,
                                @Param("reviewType") SecurityReview.ReviewType reviewType,
                                @Param("keyword") String keyword,
                                Pageable pageable);

    List<SecurityReview> findByStatus(SecurityReview.Status status);

    long countByStatus(SecurityReview.Status status);

    long countByDecision(SecurityReview.Decision decision);
}
