package com.monosun.secportal.privacy.repository;

import com.monosun.secportal.privacy.entity.PrivacyRightsRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface PrivacyRightsRequestRepository extends JpaRepository<PrivacyRightsRequest, Long> {

    List<PrivacyRightsRequest> findAllByOrderByRequestDateDesc();

    long countByStatus(PrivacyRightsRequest.Status status);

    /** 요청 유형별 건수 — [RequestType, count] */
    @Query("SELECT r.requestType, COUNT(r) FROM PrivacyRightsRequest r GROUP BY r.requestType")
    List<Object[]> countGroupByType();
}
