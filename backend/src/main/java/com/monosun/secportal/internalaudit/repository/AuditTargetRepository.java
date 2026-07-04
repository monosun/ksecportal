package com.monosun.secportal.internalaudit.repository;

import com.monosun.secportal.internalaudit.entity.AuditTarget;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AuditTargetRepository extends JpaRepository<AuditTarget, Long> {

    List<AuditTarget> findByAuditIdOrderBySortOrderAscCreatedAtAsc(Long auditId);
}
