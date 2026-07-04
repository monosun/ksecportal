package com.monosun.secportal.notification.repository;

import com.monosun.secportal.notification.entity.NotificationConfig;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationConfigRepository extends JpaRepository<NotificationConfig, String> {
}
