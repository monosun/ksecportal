package com.monosun.secportal.notification.repository;

import com.monosun.secportal.notification.entity.MailConfig;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MailConfigRepository extends JpaRepository<MailConfig, Long> {
    Optional<MailConfig> findFirstByOrderByIdAsc();
}
