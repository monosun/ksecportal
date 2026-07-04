package com.monosun.secportal.auth.service;

import com.monosun.secportal.notification.entity.NotificationConfig;
import com.monosun.secportal.notification.repository.NotificationConfigRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class SecurityConfigService {

    static final String KEY_MAX_ATTEMPTS = "security.login.max_attempts";
    static final String KEY_LOCKOUT_MINUTES = "security.login.lockout_minutes";
    static final int DEFAULT_MAX_ATTEMPTS = 5;
    static final int DEFAULT_LOCKOUT_MINUTES = 10;

    private final NotificationConfigRepository repo;

    public int getMaxAttempts() {
        return repo.findById(KEY_MAX_ATTEMPTS)
                .map(c -> parseInt(c.getConfigValue(), DEFAULT_MAX_ATTEMPTS))
                .orElse(DEFAULT_MAX_ATTEMPTS);
    }

    public int getLockoutMinutes() {
        return repo.findById(KEY_LOCKOUT_MINUTES)
                .map(c -> parseInt(c.getConfigValue(), DEFAULT_LOCKOUT_MINUTES))
                .orElse(DEFAULT_LOCKOUT_MINUTES);
    }

    @Transactional
    public void save(int maxAttempts, int lockoutMinutes) {
        repo.save(new NotificationConfig(KEY_MAX_ATTEMPTS, String.valueOf(maxAttempts)));
        repo.save(new NotificationConfig(KEY_LOCKOUT_MINUTES, String.valueOf(lockoutMinutes)));
    }

    public Map<String, Object> getAll() {
        return Map.of(
                "maxAttempts", getMaxAttempts(),
                "lockoutMinutes", getLockoutMinutes()
        );
    }

    private int parseInt(String value, int defaultValue) {
        try {
            return Integer.parseInt(value);
        } catch (Exception e) {
            return defaultValue;
        }
    }
}
