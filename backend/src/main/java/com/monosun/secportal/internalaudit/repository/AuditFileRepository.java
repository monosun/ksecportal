package com.monosun.secportal.internalaudit.repository;

import com.monosun.secportal.internalaudit.entity.AuditFile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AuditFileRepository extends JpaRepository<AuditFile, Long> {

    List<AuditFile> findByAuditIdOrderByCreatedAtAsc(Long auditId);
}
