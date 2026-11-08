package com.monosun.secportal.detection.service;

import com.monosun.secportal.auth.repository.UserRepository;
import com.monosun.secportal.common.exception.BusinessException;
import com.monosun.secportal.detection.entity.DetectionEvent.Category;
import com.monosun.secportal.detection.entity.DetectionEvent.Severity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Locale;

/**
 * 로그인 경로 보완 — 계정 단위 잠금(AuthService)이 못 막는 공격을 IP 단위로 막는다.
 * <ul>
 *   <li>미끼 계정: 존재하지 않는 미끼 이메일로 로그인하면 유출 계정 목록·미끼 페이지를 쓰는 공격자로 본다.</li>
 *   <li>크리덴셜 스터핑: 한 IP 가 여러 계정을 돌아가며 실패하면 계정 잠금에 걸리지 않으므로
 *       IP 기준 실패 건수·서로 다른 계정 수로 판단해 그 IP 의 로그인을 일시 차단한다.</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class LoginGuardService {

    private final DetectionSettingsService settingsService;
    private final RequestTracker tracker;
    private final DetectionSignals signals;
    private final IpBlockService ipBlockService;
    private final UserRepository userRepository;

    /** 로그인 처리 전에 호출. IP 로그인 차단 중이면 예외. 미끼 계정이면 탐지 후 일반 실패 메시지로 거절. */
    public void beforeLogin(String ip, String email, String userAgent) {
        DetectionSettingsService.Settings s = settingsService.get();
        if (!s.enabled()) return;
        long remain = tracker.loginBlockedRemainingMs(ip);
        if (remain > 0) {
            long mins = Math.max(1, (remain + 59_999) / 60_000);
            throw new BusinessException("이 위치(IP)에서 로그인 실패가 많아 일시적으로 차단되었습니다. " + mins + "분 후 다시 시도하세요.");
        }
        String normalized = email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
        if (!normalized.isEmpty() && s.honeyAccounts().contains(normalized)
                && userRepository.findByEmail(email.trim()).isEmpty()) {
            signals.fire(DetectionEventService.Signal.builder()
                    .category(Category.HONEY_ACCOUNT).severity(Severity.CRITICAL).ip(ip)
                    .title("미끼 계정 로그인 시도: " + normalized)
                    .detail("실제로 존재하지 않고 미끼 페이지에만 노출한 계정(" + normalized + ")으로 로그인을 시도했습니다.\n"
                            + "미끼 페이지를 읽은 자동화 도구·AI 에이전트이거나 유출 계정 목록을 쓰는 공격자일 가능성이 높습니다.")
                    .path("/auth/login").userAgent(userAgent).build());
            throw new BusinessException("이메일 또는 비밀번호가 올바르지 않습니다");
        }
    }

    /** 로그인 실패 시 호출 — 존재하지 않는 계정 실패도 포함한다. */
    public void onLoginFailure(String ip, String email, String userAgent) {
        DetectionSettingsService.Settings s = settingsService.get();
        if (!s.enabled() || ip == null) return;
        int[] stat = tracker.recordLoginFailure(ip, email, s.loginWindowMinutes() * 60_000L);
        int fails = stat[0];
        int distinct = stat[1];
        boolean stuffing = distinct >= s.loginDistinctAccounts();
        boolean brute = fails >= s.loginFailThreshold();
        if (!stuffing && !brute) return;

        boolean whitelisted = ipBlockService.isWhitelisted(ip);
        if (!whitelisted) {
            tracker.blockLogin(ip, System.currentTimeMillis() + s.loginLockMinutes() * 60_000L);
        }
        String accounts = String.join(", ", tracker.recentLoginAccounts(ip));
        signals.fire(DetectionEventService.Signal.builder()
                .category(Category.LOGIN_ATTACK)
                .severity(stuffing ? Severity.HIGH : Severity.MEDIUM)
                .ip(ip)
                .title(stuffing ? "크리덴셜 스터핑 의심 — 한 IP 에서 " + distinct + "개 계정 로그인 실패"
                        : "무차별 대입 의심 — 한 IP 에서 로그인 " + fails + "회 실패")
                .detail("최근 " + s.loginWindowMinutes() + "분간 로그인 실패 " + fails + "회, 서로 다른 계정 " + distinct + "개\n"
                        + "시도 계정: " + accounts + "\n"
                        + (whitelisted ? "허용 목록 IP 라 로그인 차단은 하지 않았습니다."
                        : "조치: 이 IP 의 로그인을 " + s.loginLockMinutes() + "분간 차단했습니다."))
                .path("/auth/login").userAgent(userAgent).build());
        // 정상 로그인으로는 IP 실패 이력을 지우지 않는다 — 공격자가 자기 계정 로그인으로 카운터를 초기화하는 것을 막는다
    }
}
