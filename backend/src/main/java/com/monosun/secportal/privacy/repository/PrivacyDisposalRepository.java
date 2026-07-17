package com.monosun.secportal.privacy.repository;

import com.monosun.secportal.privacy.entity.PrivacyDisposal;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PrivacyDisposalRepository extends JpaRepository<PrivacyDisposal, Long> {

    List<PrivacyDisposal> findAllByOrderByPlannedDateDesc();

    long countByStatus(PrivacyDisposal.Status status);

    long countByApprovalStatus(PrivacyDisposal.ApprovalStatus approvalStatus);
}
