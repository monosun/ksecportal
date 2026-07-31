package com.monosun.secportal.perf.service;

import com.monosun.secportal.perf.dto.PerformanceDto;
import com.monosun.secportal.perf.entity.SlowLog;
import com.monosun.secportal.perf.repository.SlowLogRepository;
import com.monosun.secportal.setting.service.AppSettingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 성능관리 — 임계시간(기본 3초)을 넘긴 화면 요청·SQL을 기록/조회한다.
 *
 * 기록은 요청 처리 흐름을 막지 않도록 메모리 큐에 쌓아두고 스케줄러가 일괄 저장한다.
 * (SQL 인터셉트 지점에서 곧바로 DB에 쓰면 진행 중인 트랜잭션·커넥션과 얽히기 때문)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PerformanceService {

    private final SlowLogRepository repository;
    private final AppSettingService appSettingService;

    public static final String KEY_THRESHOLD = "perf.threshold_ms";
    public static final String KEY_ENABLED = "perf.enabled";
    public static final String KEY_SQL_ENABLED = "perf.sql_enabled";
    public static final String KEY_RETENTION_DAYS = "perf.retention_days";

    public static final int DEFAULT_THRESHOLD_MS = 3000;
    public static final int DEFAULT_RETENTION_DAYS = 30;

    /** 큐가 무한정 커지지 않도록 상한 — 넘치면 새 기록을 버린다(운영에 영향 주지 않는 것이 우선) */
    private static final int QUEUE_LIMIT = 2000;
    /** 저장하는 SQL 구문 길이 상한 */
    private static final int SQL_MAX_LENGTH = 4000;

    private final ConcurrentLinkedQueue<SlowLog> queue = new ConcurrentLinkedQueue<>();
    private final AtomicInteger queued = new AtomicInteger();

    /** 매 요청·매 쿼리마다 설정 테이블을 읽지 않도록 캐시 (설정 변경 시 즉시 갱신) */
    private volatile int cachedThresholdMs = DEFAULT_THRESHOLD_MS;
    private volatile boolean cachedEnabled = true;
    private volatile boolean cachedSqlEnabled = true;
    private volatile long cacheLoadedAt = 0L;
    static final long CACHE_TTL_MS = 30_000L;

    /** 기록 저장(플러시) 중임을 표시 — 그때 발생하는 SQL은 다시 기록하지 않는다 */
    private static final ThreadLocal<Boolean> FLUSHING = ThreadLocal.withInitial(() -> false);

    public static boolean isFlushing() {
        return FLUSHING.get();
    }

    // ── 설정 ────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public PerformanceDto.Config getConfig() {
        refreshCache(true);
        return PerformanceDto.Config.builder()
                .thresholdMs(cachedThresholdMs)
                .enabled(cachedEnabled)
                .sqlEnabled(cachedSqlEnabled)
                .retentionDays(intSetting(KEY_RETENTION_DAYS, DEFAULT_RETENTION_DAYS))
                .build();
    }

    @Transactional
    public PerformanceDto.Config updateConfig(PerformanceDto.ConfigRequest req) {
        if (req.getThresholdMs() != null) {
            int v = Math.max(100, Math.min(600_000, req.getThresholdMs()));
            appSettingService.upsert(KEY_THRESHOLD, String.valueOf(v));
        }
        if (req.getEnabled() != null) appSettingService.upsert(KEY_ENABLED, String.valueOf(req.getEnabled()));
        if (req.getSqlEnabled() != null) appSettingService.upsert(KEY_SQL_ENABLED, String.valueOf(req.getSqlEnabled()));
        if (req.getRetentionDays() != null) {
            int v = Math.max(1, Math.min(3650, req.getRetentionDays()));
            appSettingService.upsert(KEY_RETENTION_DAYS, String.valueOf(v));
        }
        refreshCache(true);
        return getConfig();
    }

    /**
     * 임계값(ms) — 인터셉터·SQL 프록시가 호출하는 경로다.
     * 이 경로에서 설정을 DB로 읽으면 그 조회 SQL이 다시 기록 경로를 타므로,
     * 여기서는 절대 DB에 접근하지 않고 스케줄러가 갱신해 둔 캐시 값만 돌려준다.
     */
    public int thresholdMs() {
        return cachedThresholdMs;
    }

    public boolean isEnabled() {
        return cachedEnabled;
    }

    public boolean isSqlEnabled() {
        return cachedEnabled && cachedSqlEnabled;
    }

    /** 설정 캐시 주기 갱신 — 기록 경로가 아닌 스케줄러 스레드에서만 DB를 읽는다 */
    @Scheduled(fixedDelay = CACHE_TTL_MS, initialDelay = 5_000L)
    public void refreshConfigCache() {
        refreshCache(true);
    }

    private void refreshCache(boolean force) {
        long now = System.currentTimeMillis();
        if (!force && now - cacheLoadedAt < CACHE_TTL_MS) return;
        try {
            cachedThresholdMs = intSetting(KEY_THRESHOLD, DEFAULT_THRESHOLD_MS);
            cachedEnabled = boolSetting(KEY_ENABLED, true);
            cachedSqlEnabled = boolSetting(KEY_SQL_ENABLED, true);
        } catch (Exception e) {
            // 설정 조회 실패는 기본값으로 계속 동작 (성능 수집이 서비스를 막으면 안 됨)
            log.debug("성능관리 설정 조회 실패 — 기본값 사용: {}", e.getMessage());
        }
        cacheLoadedAt = now;
    }

    private int intSetting(String key, int defaultValue) {
        String v = appSettingService.getValue(key);
        if (v == null || v.isBlank()) return defaultValue;
        try { return Integer.parseInt(v.trim()); } catch (NumberFormatException e) { return defaultValue; }
    }

    private boolean boolSetting(String key, boolean defaultValue) {
        String v = appSettingService.getValue(key);
        if (v == null || v.isBlank()) return defaultValue;
        return Boolean.parseBoolean(v.trim());
    }

    // ── 기록 ────────────────────────────────────────────────────────────────

    /** 화면(요청) 지연 기록 */
    public void recordScreen(String target, String detail, long durationMs,
                             String username, String ip, String method, Integer status) {
        if (!isEnabled()) return;
        int threshold = thresholdMs();
        if (durationMs < threshold) return;
        offer(SlowLog.builder()
                .logType(SlowLog.LogType.SCREEN)
                .target(cut(target, 500))
                .detail(detail)
                .durationMs(durationMs)
                .thresholdMs(threshold)
                .username(cut(username, 100))
                .ipAddress(cut(ip, 60))
                .httpMethod(cut(method, 10))
                .statusCode(status)
                .occurredAt(LocalDateTime.now())
                .build());
    }

    /** SQL 지연 기록 */
    public void recordSql(String sql, long durationMs, String username) {
        if (!isSqlEnabled()) return;
        int threshold = thresholdMs();
        if (durationMs < threshold) return;
        String normalized = sql == null ? "" : sql.replaceAll("\\s+", " ").trim();
        offer(SlowLog.builder()
                .logType(SlowLog.LogType.SQL)
                .target(cut(normalized, 500))
                .detail(cut(normalized, SQL_MAX_LENGTH))
                .durationMs(durationMs)
                .thresholdMs(threshold)
                .username(cut(username, 100))
                .occurredAt(LocalDateTime.now())
                .build());
    }

    private void offer(SlowLog entry) {
        if (queued.get() >= QUEUE_LIMIT) return;
        queue.offer(entry);
        queued.incrementAndGet();
    }

    private String cut(String s, int max) {
        if (s == null) return null;
        return s.length() <= max ? s : s.substring(0, max);
    }

    /** 큐에 쌓인 기록을 주기적으로 저장 */
    @Scheduled(fixedDelay = 5_000L)
    @Transactional
    public void flush() {
        if (queue.isEmpty()) return;
        List<SlowLog> batch = new ArrayList<>();
        SlowLog item;
        while ((item = queue.poll()) != null) {
            queued.decrementAndGet();
            batch.add(item);
            if (batch.size() >= 500) break;
        }
        if (batch.isEmpty()) return;
        FLUSHING.set(true);
        try {
            repository.saveAll(batch);
        } catch (Exception e) {
            log.warn("성능 기록 저장 실패 ({}건): {}", batch.size(), e.getMessage());
        } finally {
            FLUSHING.set(false);
        }
    }

    /** 보관기간이 지난 기록 정리 — 매일 03:20 */
    @Scheduled(cron = "0 20 3 * * *")
    @Transactional
    public void purgeExpired() {
        int days = intSetting(KEY_RETENTION_DAYS, DEFAULT_RETENTION_DAYS);
        FLUSHING.set(true);
        try {
            int deleted = repository.deleteOlderThan(LocalDateTime.now().minusDays(days));
            if (deleted > 0) log.info("성능 기록 {}건 정리 (보관 {}일 초과)", deleted, days);
        } finally {
            FLUSHING.set(false);
        }
    }

    // ── 조회 ────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Page<PerformanceDto.LogResponse> list(String logType, String keyword, Long minMs,
                                                 LocalDateTime dateFrom, LocalDateTime dateTo, Pageable pageable) {
        SlowLog.LogType type = null;
        if (logType != null && !logType.isBlank()) {
            try { type = SlowLog.LogType.valueOf(logType.trim().toUpperCase()); } catch (IllegalArgumentException ignored) {}
        }
        String kw = (keyword == null || keyword.isBlank()) ? null : keyword.trim();
        return repository.search(type, kw, minMs, dateFrom, dateTo, pageable)
                .map(PerformanceDto.LogResponse::from);
    }

    @Transactional(readOnly = true)
    public PerformanceDto.Stats stats() {
        long screen = repository.countByLogType(SlowLog.LogType.SCREEN);
        long sql = repository.countByLogType(SlowLog.LogType.SQL);
        return PerformanceDto.Stats.builder()
                .total(screen + sql)
                .screenCount(screen)
                .sqlCount(sql)
                .maxDurationMs(repository.findMaxDuration())
                .build();
    }

    @Transactional
    public int purge(Integer days) {
        FLUSHING.set(true);
        try {
            if (days == null || days <= 0) {
                long all = repository.count();
                repository.deleteAllInBatch();
                return (int) all;
            }
            return repository.deleteOlderThan(LocalDateTime.now().minusDays(days));
        } finally {
            FLUSHING.set(false);
        }
    }
}
