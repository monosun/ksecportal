package com.monosun.secportal.internalaudit.repository;

import com.monosun.secportal.internalaudit.entity.AuditItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AuditItemRepository extends JpaRepository<AuditItem, Long> {

    List<AuditItem> findByAuditIdOrderBySortOrderAscCreatedAtAsc(Long auditId);
}
