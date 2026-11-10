package com.monosun.secportal.detection.service;

import com.monosun.secportal.audit.service.AuditLogService;
import com.monosun.secportal.auth.entity.User;
import com.monosun.secportal.auth.repository.UserRepository;
import com.monosun.secportal.common.exception.BusinessException;
import com.monosun.secportal.common.exception.ResourceNotFoundException;
import com.monosun.secportal.detection.dto.DetectionDto;
import com.monosun.secportal.detection.entity.DetectionEvent;
import com.monosun.secportal.detection.entity.DetectionEvent.Category;
import com.monosun.secportal.detection.entity.DetectionEvent.Severity;
import com.monosun.secportal.detection.entity.DetectionEvent.Status;
import com.monosun.secportal.detection.repository.DetectionEventRepository;
import com.monosun.secportal.detection.web.TarpitResponder;
import com.monosun.secportal.inbox.entity.InboxMessage;
import com.monosun.secportal.inbox.service.InboxService;
import com.monosun.secportal.incident.dto.IncidentDto;
import com.monosun.secportal.incident.entity.Incident;
import com.monosun.secportal.incident.service.IncidentService;
import com.monosun.secportal.notification.service.ApprovalNotificationService;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.HtmlUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class DetectionEventService {

    /** 같은 IP·같은 유형을 하나의 이벤트로 묶는 시간 */
    private static final int DEDUPE_MINUTES = 30;
    private static final List<Status> OPEN = List.of(Status.NEW, Status.ACKNOWLEDGED);
    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final DetectionEventRepository repo;
    private final DetectionSettingsService settingsService;
    private final IpBlockService ipBlockService;
    private final ApprovalNotificationService notifier;
    private final InboxService inboxService;
    private final UserRepository userRepository;
    private final IncidentService incidentService;
    private final AuditLogService auditLogService;
    private final TarpitResponder tarpitResponder;

    @Builder
    public record Signal(Category category, Severity severity, String ip, String title, String detail,
                         String path, String userAgent, String user, Integer score) {}

    /**
     * 탐지 신호를 이벤트로 기록한다. 요청 처리 중에 호출되므로 어떤 예외도 밖으로 던지지 않는다.
     * 새 이벤트이거나 심각도가 올라간 경우에만 알림·자동 차단을 수행한다.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public DetectionEvent raise(Signal sig) {
        try {
            if (!settingsService.get().enabled() && sig.category() != Category.TEST) return null;
            LocalDateTime now = LocalDateTime.now();
            Optional<DetectionEvent> existing = sig.ip() == null ? Optional.empty()
                    : repo.findFirstBySourceIpAndCategoryAndStatusInAndLastSeenAtAfterOrderByIdDesc(
                            sig.ip(), sig.category(), OPEN, now.minusMinutes(DEDUPE_MINUTES));

            DetectionEvent e;
            boolean notify;
            if (existing.isPresent()) {
                e = existing.get();
                e.setOccurrences((e.getOccurrences() == null ? 1 : e.getOccurrences()) + 1);
                e.setLastSeenAt(now);
                if (sig.detail() != null) e.setDetail(cut(sig.detail(), 20_000));
                if (sig.path() != null) e.setRequestPath(cut(sig.path(), 500));
                if (sig.score() != null) e.setScore(Math.max(sig.score(), e.getScore() == null ? 0 : e.getScore()));
                if (sig.user() != null) e.setUserEmail(cut(sig.user(), 200));
                notify = sig.severity().ordinal() > e.getSeverity().ordinal();
                if (notify) {
                    e.setSeverity(sig.severity());
                    e.setTitle(cut(sig.title(), 200));
                }
            } else {
                e = DetectionEvent.builder()
                        .category(sig.category())
                        .severity(sig.severity())
                        .title(cut(sig.title(), 200))
                        .detail(cut(sig.detail(), 20_000))
                        .sourceIp(sig.ip())
                        .userEmail(cut(sig.user(), 200))
                        .userAgent(cut(sig.userAgent(), 500))
                        .requestPath(cut(sig.path(), 500))
                        .score(sig.score())
                        .detectedAt(now)
                        .lastSeenAt(now)
                        .build();
                notify = true;
            }
            e = repo.save(e);
            if (notify) {
                maybeAutoBlock(e);
                maybeNotify(e);
            }
            return e;
        } catch (Exception ex) {
            log.warn("[탐지] 이벤트 기록 실패({}): {}", sig.category(), ex.getMessage());
            return null;
        }
    }

    private void maybeAutoBlock(DetectionEvent e) {
        DetectionSettingsService.Settings s = settingsService.get();
        if (!s.autoBlockEnabled() || e.getSourceIp() == null || e.getCategory() == Category.TEST) return;
        if (!e.getSeverity().atLeast(s.autoBlockMinSeverity())) return;
        boolean blocked = ipBlockService.autoBlock(e.getSourceIp(), s.blockMinutes(),
                "[자동] " + e.getTitle(), e.getId());
        if (blocked) {
            e.setAutoBlocked(true);
            repo.save(e);
        }
    }

    private void maybeNotify(DetectionEvent e) {
        if (!e.getSeverity().atLeast(settingsService.get().notifyMinSeverity())) return;
        try {
            String title = "[공격탐지][" + e.getSeverity() + "] " + e.getTitle();
            String body = "유형: " + categoryLabel(e.getCategory()) + "\n"
                    + "출발지 IP: " + nvl(e.getSourceIp()) + "\n"
                    + (e.getUserEmail() != null ? "계정: " + e.getUserEmail() + "\n" : "")
                    + (e.getRequestPath() != null ? "경로: " + e.getRequestPath() + "\n" : "")
                    + "탐지 시각: " + e.getDetectedAt().format(TS) + "\n"
                    + (Boolean.TRUE.equals(e.getAutoBlocked()) ? "조치: IP 자동 차단\n" : "")
                    + "\n" + nvl(e.getDetail());
            String html = "<h3>" + HtmlUtils.htmlEscape(title) + "</h3><pre style=\"font-family:inherit;white-space:pre-wrap\">"
                    + HtmlUtils.htmlEscape(body) + "</pre><p>정보보호 포탈 &gt; 보안 운영 &gt; 공격 탐지에서 확인하세요.</p>";
            notifier.send(title, html, ":rotating_light: *" + title + "*\n" + body);
            for (User admin : userRepository.findAllByRoleAndActiveTrue(User.Role.ADMIN)) {
                inboxService.send(admin, InboxMessage.MessageType.SYSTEM, cut(title, 200), cut(body, 2000), null);
            }
        } catch (Exception ex) {
            log.warn("[탐지] 알림 발송 실패: {}", ex.getMessage());
        }
    }

    // ── 조회·처리 ─────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Page<DetectionDto.EventResponse> search(Category category, Severity severity, Status status,
                                                   String ip, Integer days, Pageable pageable) {
        LocalDateTime from = days == null || days <= 0 ? null : LocalDateTime.now().minusDays(days);
        String ipFilter = ip == null || ip.isBlank() ? null : ip.trim();
        return repo.search(category, severity, status, ipFilter, from, pageable).map(DetectionDto.EventResponse::from);
    }

    @Transactional(readOnly = true)
    public DetectionDto.EventResponse get(Long id) {
        return DetectionDto.EventResponse.from(find(id));
    }

    @Transactional
    public DetectionDto.EventResponse updateStatus(Long id, Status status, String note, User by) {
        if (status == null || status == Status.ESCALATED) throw new BusinessException("변경할 상태가 올바르지 않습니다.");
        DetectionEvent e = find(id);
        e.setStatus(status);
        e.setHandlerNote(cut(note, 1000));
        e.setHandledBy(by.getName());
        e.setHandledAt(LocalDateTime.now());
        auditLogService.log("DETECTION_STATUS", "DETECTION_EVENT", id, status + (note == null ? "" : " / " + note));
        return DetectionDto.EventResponse.from(repo.save(e));
    }

    /** 침해사고로 전환 — 사고를 만들고 이벤트를 ESCALATED 로 표시 */
    @Transactional
    public DetectionDto.EventResponse escalate(Long id, User by) {
        DetectionEvent e = find(id);
        if (e.getIncidentId() != null) throw new BusinessException("이미 침해사고로 전환된 이벤트입니다.");
        IncidentDto.CreateRequest req = new IncidentDto.CreateRequest();
        req.setTitle(cut("[공격탐지] " + e.getTitle(), 200));
        req.setDescription("공격 탐지 이벤트 #" + e.getId() + " 에서 전환\n\n"
                + "유형: " + categoryLabel(e.getCategory()) + "\n"
                + "출발지 IP: " + nvl(e.getSourceIp()) + "\n"
                + "User-Agent: " + nvl(e.getUserAgent()) + "\n"
                + "요청 경로: " + nvl(e.getRequestPath()) + "\n"
                + "최초/최근 탐지: " + e.getDetectedAt().format(TS) + " / " + e.getLastSeenAt().format(TS)
                + " (" + e.getOccurrences() + "회)\n\n" + nvl(e.getDetail()));
        req.setSeverity(switch (e.getSeverity()) {
            case CRITICAL -> Incident.Severity.CRITICAL;
            case HIGH -> Incident.Severity.HIGH;
            case MEDIUM -> Incident.Severity.MEDIUM;
            default -> Incident.Severity.LOW;
        });
        req.setType(e.getCategory() == Category.LOGIN_ATTACK || e.getCategory() == Category.HONEYTOKEN
                || e.getCategory() == Category.HONEY_ACCOUNT || e.getCategory() == Category.HONEY_DATA
                ? Incident.IncidentType.UNAUTHORIZED_ACCESS : Incident.IncidentType.OTHER);
        req.setAffectedSystems("정보보호 포탈(KSecPortal)");
        req.setDetectedAt(e.getDetectedAt());
        IncidentDto.Response incident = incidentService.create(req, by.getId());
        e.setIncidentId(incident.getId());
        e.setStatus(Status.ESCALATED);
        e.setHandledBy(by.getName());
        e.setHandledAt(LocalDateTime.now());
        auditLogService.log("DETECTION_ESCALATED", "DETECTION_EVENT", id, "incident=" + incident.getId());
        return DetectionDto.EventResponse.from(repo.save(e));
    }

    @Transactional
    public void delete(Long id) {
        DetectionEvent e = find(id);
        repo.delete(e);
        auditLogService.log("DETECTION_DELETED", "DETECTION_EVENT", id, e.getTitle());
    }

    @Transactional(readOnly = true)
    public DetectionDto.Summary summary() {
        LocalDateTime now = LocalDateTime.now();
        List<DetectionEvent> week = repo.findByLastSeenAtAfter(now.minusDays(7));
        LocalDateTime dayAgo = now.minusDays(1);
        Map<String, Long> bySeverity24h = new LinkedHashMap<>();
        for (Severity s : Severity.values()) bySeverity24h.put(s.name(), 0L);
        Map<String, Long> byCategory7d = new LinkedHashMap<>();
        for (Category c : Category.values()) byCategory7d.put(c.name(), 0L);
        Map<String, Long> ipCount = new HashMap<>();
        Map<String, Long> daily = new LinkedHashMap<>();
        for (int i = 6; i >= 0; i--) daily.put(now.minusDays(i).toLocalDate().toString(), 0L);
        long last24h = 0;
        for (DetectionEvent e : week) {
            if (e.getCategory() == Category.TEST) continue;
            byCategory7d.merge(e.getCategory().name(), 1L, Long::sum);
            daily.computeIfPresent(e.getLastSeenAt().toLocalDate().toString(), (k, v) -> v + 1);
            if (e.getSourceIp() != null) ipCount.merge(e.getSourceIp(), (long) e.getOccurrences(), Long::sum);
            if (e.getLastSeenAt().isAfter(dayAgo)) {
                last24h++;
                bySeverity24h.merge(e.getSeverity().name(), 1L, Long::sum);
            }
        }
        List<Map<String, Object>> topIps = ipCount.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(10)
                .map(en -> Map.<String, Object>of("ip", en.getKey(), "count", en.getValue(),
                        "blocked", ipBlockService.isBlocked(en.getKey())))
                .toList();
        return DetectionDto.Summary.builder()
                .last24h(last24h)
                .openCount(repo.countByStatus(Status.NEW))
                .activeBlocks(ipBlockService.activeCount())
                .bySeverity24h(bySeverity24h)
                .byCategory7d(byCategory7d)
                .daily7d(daily)
                .topIps(topIps)
                .enabled(settingsService.get().enabled())
                .responseMode(settingsService.get().responseMode())
                .tarpitServed(tarpitResponder.servedCount())
                .build();
    }

    /** 알림·차단 경로 점검용 테스트 이벤트(자동 차단은 하지 않는다) */
    @Transactional
    public DetectionDto.EventResponse createTest(User by, String ip) {
        DetectionEvent e = raise(Signal.builder()
                .category(Category.TEST).severity(Severity.HIGH).ip(ip)
                .title("알림 테스트 이벤트")
                .detail(by.getName() + " 님이 공격 탐지 알림 경로를 점검하기 위해 생성한 테스트 이벤트입니다.")
                .user(by.getEmail()).build());
        if (e == null) throw new BusinessException("테스트 이벤트를 만들지 못했습니다.");
        return DetectionDto.EventResponse.from(e);
    }

    /** 매일 03:40 보존기간이 지난 이벤트(침해사고 전환분 제외)와 해제된 차단 이력 정리 */
    @Scheduled(cron = "0 40 3 * * *")
    @Transactional
    public void purgeOld() {
        int days = settingsService.get().retentionDays();
        int n = repo.deleteOlderThan(LocalDateTime.now().minusDays(days), Status.ESCALATED);
        if (n > 0) log.info("[탐지] 보존기간({}일) 경과 이벤트 {}건 삭제", days, n);
    }

    public static String categoryLabel(Category c) {
        return switch (c) {
            case HONEYPOT_SCAN -> "공격 도구 경로 탐색(허니팟)";
            case HONEYPOT_BAIT -> "미끼 경로 접근(숨김 링크·robots.txt)";
            case LLM_AGENT -> "LLM 에이전트 확인(프롬프트 인젝션 미끼)";
            case HONEYTOKEN -> "가짜 API 키(허니토큰) 사용";
            case HONEY_ACCOUNT -> "미끼 계정 로그인 시도";
            case BEHAVIOR -> "자동화 행위 탐지(행위 점수)";
            case LOGIN_ATTACK -> "로그인 공격(무차별 대입·크리덴셜 스터핑)";
            case HONEY_DATA -> "미끼 데이터 재사용(가짜 레코드 조회)";
            case TEST -> "테스트";
        };
    }

    private DetectionEvent find(Long id) {
        return repo.findById(id).orElseThrow(() -> new ResourceNotFoundException("DetectionEvent", id));
    }

    private static String nvl(String s) {
        return s == null ? "-" : s;
    }

    static String cut(String s, int max) {
        if (s == null) return null;
        return s.length() > max ? s.substring(0, max) : s;
    }
}
