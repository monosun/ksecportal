package com.monosun.secportal.admin.repository;

import com.monosun.secportal.admin.entity.PendingAdminAction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PendingAdminActionRepository extends JpaRepository<PendingAdminAction, Long> {
    Optional<PendingAdminAction> findByToken(String token);
}
