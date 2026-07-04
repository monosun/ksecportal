package com.monosun.secportal.policy.repository;

import com.monosun.secportal.policy.entity.Policy;
import com.monosun.secportal.policy.entity.PolicyAcknowledgment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PolicyAcknowledgmentRepository extends JpaRepository<PolicyAcknowledgment, Long> {
    boolean existsByPolicyIdAndUserId(Long policyId, Long userId);
    Optional<PolicyAcknowledgment> findByPolicyIdAndUserId(Long policyId, Long userId);
    long countByPolicyId(Long policyId);

    @Query("SELECT COUNT(DISTINCT a.user.id) FROM PolicyAcknowledgment a WHERE a.policy.status = :status")
    long countDistinctUsersByPolicyStatus(@Param("status") Policy.Status status);
}
