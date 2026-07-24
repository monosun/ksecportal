package com.monosun.secportal.secreview.repository;

import com.monosun.secportal.secreview.entity.SecurityReviewItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SecurityReviewItemRepository extends JpaRepository<SecurityReviewItem, Long> {

    List<SecurityReviewItem> findByReviewIdOrderBySortOrderAscIdAsc(Long reviewId);
}
