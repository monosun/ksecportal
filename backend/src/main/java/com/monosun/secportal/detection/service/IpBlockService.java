package com.monosun.secportal.detection.service;

import com.monosun.secportal.common.exception.BusinessException;
import com.monosun.secportal.common.exception.ResourceNotFoundException;
import com.monosun.secportal.common.net.ClientIp;
import com.monosun.secportal.detection.entity.BlockedIp;
import com.monosun.secportal.detection.repository.BlockedIpRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * IP 차단. 모든 요청에서 조회하므로 유효한 차단 목록을 메모리에 캐시하고(변경 시 즉시, 그 외 30초마다 갱신)
 * 허용 목록(화이트리스트) IP 는 어떤 경우에도 차단하지 않는다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IpBlockService {

    private final BlockedIpRepository repo;
    private final DetectionSettingsService settingsService;

    /** ip → 차단 만료 시각(epoch ms, Long.MAX_VALUE = 무기한) */
    private final Map<String, Long> cache = new ConcurrentHashMap<>();

    public boolean isBlocked(String ip) {
        if (ip == null) return false;
        Long until = cache.get(ip);
        if (until == null) return false;
        if (until < System.currentTimeMillis()) {
            cache.remove(ip);
            return false;
        }
        return !isWhitelisted(ip);
    }

    public boolean isWhitelisted(String ip) {
        return ClientIp.matchesAny(ip, settingsService.get().whitelistIps());
    }

    @Scheduled(fixedDelay = 30_000, initialDelay = 5_000)
    @Transactional
    public void refresh() {
        LocalDateTime now = LocalDateTime.now();
        Map<String, Long> fresh = new ConcurrentHashMap<>();
        for (BlockedIp b : repo.findByActiveTrue()) {
            if (!b.isEffective(now)) {
                b.setActive(false);          // 만료된 차단은 자동 해제 처리
                b.setReleasedAt(now);
                b.setReleasedBy("SYSTEM(만료)");
                continue;
            }
            long until = b.getBlockedUntil() == null ? Long.MAX_VALUE
                    : b.getBlockedUntil().atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();
            fresh.merge(b.getIp(), until, Math::max);
        }
        cache.clear();
        cache.putAll(fresh);
    }

    /** 자동 차단 — 허용 목록 IP 이거나 이미 차단 중이면 아무것도 하지 않고 false */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean autoBlock(String ip, int minutes, String reason, Long eventId) {
        if (ip == null || isWhitelisted(ip) || isBlocked(ip)) return false;
        save(ip, minutes, reason, "SYSTEM", eventId);
        log.warn("[탐지] IP 자동 차단: {} ({}분) — {}", ip, minutes == 0 ? "무기한" : minutes, reason);
        return true;
    }

    @Transactional
    public BlockedIp block(String ip, int minutes, String reason, String by) {
        if (ip == null || ip.isBlank()) throw new BusinessException("차단할 IP 를 입력하세요.");
        String v = ip.trim();
        if (!v.matches("[0-9a-fA-F:.]{2,64}")) throw new BusinessException("IP 주소 형식이 올바르지 않습니다.");
        if (minutes < 0) throw new BusinessException("차단 시간은 0(무기한) 이상이어야 합니다.");
        return save(v, minutes, reason, by, null);
    }

    @Transactional
    public void release(Long id, String by) {
        BlockedIp b = repo.findById(id).orElseThrow(() -> new ResourceNotFoundException("BlockedIp", id));
        b.setActive(false);
        b.setReleasedAt(LocalDateTime.now());
        b.setReleasedBy(by);
        cache.remove(b.getIp());
        refresh();
    }

    public List<BlockedIp> list() {
        return repo.findAllByOrderByCreatedAtDesc();
    }

    public long activeCount() {
        return cache.size();
    }

    private BlockedIp save(String ip, int minutes, String reason, String by, Long eventId) {
        LocalDateTime now = LocalDateTime.now();
        BlockedIp b = repo.save(BlockedIp.builder()
                .ip(ip)
                .reason(reason != null && reason.length() > 500 ? reason.substring(0, 500) : reason)
                .blockedUntil(minutes == 0 ? null : now.plusMinutes(minutes))
                .createdBy(by)
                .eventId(eventId)
                .active(true)
                .createdAt(now)
                .build());
        cache.put(ip, minutes == 0 ? Long.MAX_VALUE : System.currentTimeMillis() + minutes * 60_000L);
        return b;
    }
}
