package com.monosun.secportal.security.repository;

import com.monosun.secportal.security.entity.SecurityIntegration;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SecurityIntegrationRepository extends JpaRepository<SecurityIntegration, Long> {
    long countByStatus(SecurityIntegration.ConnectionStatus status);
}
