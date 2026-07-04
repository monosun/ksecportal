package com.monosun.secportal.backup.service;

import com.monosun.secportal.setting.service.AppSettingService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Component;

import java.util.concurrent.ScheduledFuture;

@Slf4j
@Component
@RequiredArgsConstructor
public class BackupScheduler {

    private final AppSettingService settingService;
    private final BackupService backupService;
    private final TaskScheduler taskScheduler;

    private ScheduledFuture<?> currentFuture;

    @PostConstruct
    public synchronized void init() {
        String enabled = settingService.getValue("backup.enabled");
        String cron = settingService.getValue("backup.cron");
        if ("true".equals(enabled) && cron != null && !cron.isBlank()) {
            schedule(cron);
            log.info("Scheduled backup initialized with cron: {}", cron);
        }
    }

    public synchronized void reschedule(String cron, boolean enabled) {
        if (currentFuture != null) {
            currentFuture.cancel(false);
            currentFuture = null;
        }
        if (enabled && cron != null && !cron.isBlank()) {
            schedule(cron);
            log.info("Backup rescheduled with cron: {}", cron);
        } else {
            log.info("Scheduled backup disabled");
        }
    }

    private void schedule(String cron) {
        currentFuture = taskScheduler.schedule(this::runBackup, new CronTrigger(cron));
    }

    private void runBackup() {
        String password = settingService.getValue("backup.default.password");
        if (password == null || password.isBlank()) {
            log.warn("Scheduled backup skipped: no default password configured");
            return;
        }
        try {
            log.info("Starting scheduled backup...");
            String filename = backupService.createAndSaveBackup(password, "SCHEDULED");
            log.info("Scheduled backup completed: {}", filename);
        } catch (Exception e) {
            log.error("Scheduled backup failed: {}", e.getMessage(), e);
        }
    }
}
