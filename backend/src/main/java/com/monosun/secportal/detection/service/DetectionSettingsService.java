package com.monosun.secportal.detection.service;

import com.monosun.secportal.common.exception.BusinessException;
import com.monosun.secportal.detection.entity.DetectionEvent;
import com.monosun.secportal.notification.entity.NotificationConfig;
import com.monosun.secportal.notification.repository.NotificationConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.*;

/**
 * 공격 탐지 설정. notification_configs 의 {@code detection.*} 키에 저장하고,
 * 모든 요청에서 읽히므로 메모리 스냅샷으로 캐시한다(저장 시 즉시, 그 외 1분마다 갱신).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DetectionSettingsService {

    private static final String P = "detection.";
    private static final String K_HONEY_TOKEN = P + "honey_token";

    /** 기본값 — 키 이름은 API 필드명과 같다 */
    private static final Map<String, String> DEFAULTS = new LinkedHashMap<>();
    static {
        DEFAULTS.put("enabled", "true");
        DEFAULTS.put("windowMinutes", "5");
        DEFAULTS.put("scoreThreshold", "60");
        DEFAULTS.put("autoBlockEnabled", "true");
        DEFAULTS.put("autoBlockMinSeverity", "CRITICAL");
        DEFAULTS.put("blockMinutes", "60");
        DEFAULTS.put("whitelistIps", "");
        DEFAULTS.put("honeyAccounts", "svc-backup@ksecportal.local,root@ksecportal.local");
        DEFAULTS.put("loginWindowMinutes", "10");
        DEFAULTS.put("loginFailThreshold", "10");
        DEFAULTS.put("loginDistinctAccounts", "4");
        DEFAULTS.put("loginLockMinutes", "30");
        DEFAULTS.put("notifyMinSeverity", "HIGH");
        DEFAULTS.put("retentionDays", "365");
    }

    private final NotificationConfigRepository repo;
    private volatile Settings cached;

    /** 요청 처리 경로에서 쓰는 불변 스냅샷 */
    public record Settings(
            boolean enabled,
            int windowMinutes,
            int scoreThreshold,
            boolean autoBlockEnabled,
            DetectionEvent.Severity autoBlockMinSeverity,
            int blockMinutes,
            List<String> whitelistIps,
            List<String> honeyAccounts,
            String honeyToken,
            int loginWindowMinutes,
            int loginFailThreshold,
            int loginDistinctAccounts,
            int loginLockMinutes,
            DetectionEvent.Severity notifyMinSeverity,
            int retentionDays
    ) {}

    public Settings get() {
        Settings s = cached;
        if (s == null) s = reload();
        return s;
    }

    /** 기동 직후 요청 스레드와 스케줄러가 동시에 읽어도 허니토큰이 두 번 만들어지지 않도록 직렬화한다 */
    @Scheduled(fixedDelay = 60_000, initialDelay = 60_000)
    public synchronized Settings reload() {
        try {
            Map<String, String> v = new HashMap<>(DEFAULTS);
            repo.findAll().forEach(c -> {
                if (c.getConfigKey().startsWith(P) && c.getConfigValue() != null) {
                    v.put(c.getConfigKey().substring(P.length()), c.getConfigValue());
                }
            });
            String token = v.get("honey_token");
            if (token == null || token.isBlank()) {
                token = newHoneyToken();
                try {
                    repo.save(new NotificationConfig(K_HONEY_TOKEN, token));
                } catch (Exception dup) {
                    // 다른 인스턴스가 먼저 만들었으면 그 값을 쓴다
                    token = repo.findById(K_HONEY_TOKEN).map(NotificationConfig::getConfigValue).orElse(token);
                }
            }
            cached = new Settings(
                    bool(v, "enabled"),
                    clamp(intVal(v, "windowMinutes"), 1, 60),
                    clamp(intVal(v, "scoreThreshold"), 20, 300),
                    bool(v, "autoBlockEnabled"),
                    severity(v, "autoBlockMinSeverity"),
                    clamp(intVal(v, "blockMinutes"), 0, 43_200),
                    list(v.get("whitelistIps")),
                    list(v.get("honeyAccounts")).stream().map(e -> e.toLowerCase(Locale.ROOT)).toList(),
                    token,
                    clamp(intVal(v, "loginWindowMinutes"), 1, 120),
                    clamp(intVal(v, "loginFailThreshold"), 3, 1000),
                    clamp(intVal(v, "loginDistinctAccounts"), 2, 100),
                    clamp(intVal(v, "loginLockMinutes"), 1, 1440),
                    severity(v, "notifyMinSeverity"),
                    clamp(intVal(v, "retentionDays"), 30, 3650)
            );
        } catch (Exception e) {
            log.warn("[탐지] 설정을 읽지 못해 기본값을 사용합니다: {}", e.getMessage());
            if (cached == null) {
                Map<String, String> v = new HashMap<>(DEFAULTS);
                cached = new Settings(true, 5, 60, true, DetectionEvent.Severity.CRITICAL, 60, List.of(),
                        list(v.get("honeyAccounts")), null, 10, 10, 4, 30, DetectionEvent.Severity.HIGH, 365);
            }
        }
        return cached;
    }

    /** 화면용 — 저장 형식 그대로(목록은 줄바꿈 구분) */
    public Map<String, Object> toMap() {
        Settings s = get();
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("enabled", s.enabled());
        m.put("windowMinutes", s.windowMinutes());
        m.put("scoreThreshold", s.scoreThreshold());
        m.put("autoBlockEnabled", s.autoBlockEnabled());
        m.put("autoBlockMinSeverity", s.autoBlockMinSeverity().name());
        m.put("blockMinutes", s.blockMinutes());
        m.put("whitelistIps", String.join("\n", s.whitelistIps()));
        m.put("honeyAccounts", String.join("\n", s.honeyAccounts()));
        m.put("honeyToken", s.honeyToken());
        m.put("loginWindowMinutes", s.loginWindowMinutes());
        m.put("loginFailThreshold", s.loginFailThreshold());
        m.put("loginDistinctAccounts", s.loginDistinctAccounts());
        m.put("loginLockMinutes", s.loginLockMinutes());
        m.put("notifyMinSeverity", s.notifyMinSeverity().name());
        m.put("retentionDays", s.retentionDays());
        return m;
    }

    public Settings save(Map<String, Object> body) {
        for (String key : DEFAULTS.keySet()) {
            if (!body.containsKey(key) || body.get(key) == null) continue;
            String value = String.valueOf(body.get(key)).trim();
            if (key.equals("whitelistIps") || key.equals("honeyAccounts")) {
                value = String.join(",", list(value));
            }
            if (key.endsWith("Severity")) {
                try {
                    DetectionEvent.Severity.valueOf(value);
                } catch (IllegalArgumentException e) {
                    throw new BusinessException("심각도 값이 올바르지 않습니다: " + value);
                }
            }
            if (value.length() > 500) throw new BusinessException(key + " 값이 너무 깁니다(최대 500자).");
            repo.save(new NotificationConfig(P + key, value));
        }
        return reload();
    }

    public synchronized String regenerateHoneyToken() {
        String token = newHoneyToken();
        repo.save(new NotificationConfig(K_HONEY_TOKEN, token));
        reload();
        return token;
    }

    private static String newHoneyToken() {
        byte[] b = new byte[16];
        new SecureRandom().nextBytes(b);
        StringBuilder sb = new StringBuilder("ksp_live_");
        for (byte x : b) sb.append(String.format("%02x", x));
        return sb.toString();
    }

    private static List<String> list(String raw) {
        if (raw == null || raw.isBlank()) return List.of();
        return Arrays.stream(raw.split("[,\\s]+"))
                .map(String::trim).filter(x -> !x.isEmpty()).distinct().toList();
    }

    private static boolean bool(Map<String, String> v, String k) {
        return "true".equalsIgnoreCase(v.get(k));
    }

    private static int intVal(Map<String, String> v, String k) {
        try {
            return Integer.parseInt(v.get(k).trim());
        } catch (Exception e) {
            return Integer.parseInt(DEFAULTS.get(k));
        }
    }

    private static DetectionEvent.Severity severity(Map<String, String> v, String k) {
        try {
            return DetectionEvent.Severity.valueOf(v.get(k).trim());
        } catch (Exception e) {
            return DetectionEvent.Severity.valueOf(DEFAULTS.get(k));
        }
    }

    private static int clamp(int x, int lo, int hi) {
        return Math.max(lo, Math.min(hi, x));
    }
}
