package com.monosun.secportal.detection.web;

import com.monosun.secportal.common.net.ClientIp;
import com.monosun.secportal.common.security.JwtTokenProvider;
import com.monosun.secportal.detection.entity.DetectionEvent.Category;
import com.monosun.secportal.detection.entity.DetectionEvent.Severity;
import com.monosun.secportal.detection.service.DetectionEventService;
import com.monosun.secportal.detection.service.DetectionSettingsService;
import com.monosun.secportal.detection.service.DetectionSignals;
import com.monosun.secportal.detection.service.IpBlockService;
import com.monosun.secportal.detection.service.RequestTracker;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * 모든 API 요청의 입구·출구에서 동작하는 공격 탐지 필터.
 * <ol>
 *   <li>차단된 IP 면 Spring Security 보다 먼저 403 으로 끊는다.</li>
 *   <li>요청 헤더·쿼리에 허니토큰(미끼에만 노출한 가짜 API 키)이 있으면 즉시 CRITICAL 이벤트.</li>
 *   <li>응답이 끝난 뒤 최종 상태코드(401·403·404 포함)를 IP 별 윈도에 기록 → {@code BehaviorAnalyzer} 가 1분마다 분석.</li>
 * </ol>
 * Spring Security 필터 체인(-100)보다 바깥에서 돌아야 인증 실패(401)·권한 거부(403) 응답 코드까지 볼 수 있다.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
@RequiredArgsConstructor
public class DetectionRequestFilter extends OncePerRequestFilter {

    private static final String BLOCKED_BODY =
            "{\"success\":false,\"message\":\"보안 정책에 따라 이 위치(IP)의 접근이 차단되었습니다. 정보보호 담당자에게 문의하세요.\",\"data\":null}";

    private final DetectionSettingsService settingsService;
    private final IpBlockService ipBlockService;
    private final RequestTracker tracker;
    private final DetectionSignals signals;
    private final JwtTokenProvider tokenProvider;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = path(request);
        return path.startsWith("/actuator") || path.startsWith("/detect-trap");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        DetectionSettingsService.Settings s = settingsService.get();
        if (!s.enabled()) {
            chain.doFilter(req, res);
            return;
        }
        String ip = ClientIp.resolve(req);
        String path = path(req);
        String ua = req.getHeader("User-Agent");

        if (ipBlockService.isBlocked(ip)) {
            res.setStatus(HttpServletResponse.SC_FORBIDDEN);
            res.setContentType("application/json;charset=UTF-8");
            res.getOutputStream().write(BLOCKED_BODY.getBytes(StandardCharsets.UTF_8));
            tracker.record(ip, req.getMethod(), path, 403, null, ua);
            return;
        }

        String honeyToken = s.honeyToken();
        if (honeyToken != null && carriesToken(req, honeyToken)) {
            signals.fire(DetectionEventService.Signal.builder()
                    .category(Category.HONEYTOKEN).severity(Severity.CRITICAL).ip(ip)
                    .title("허니토큰(가짜 API 키) 사용 탐지")
                    .detail("미끼 페이지에만 노출한 가짜 API 키가 " + req.getMethod() + " " + path
                            + " 요청에 실려 왔습니다.\n미끼 페이지를 읽고 그 안의 자격증명을 사용한 자동화 도구·AI 에이전트 또는 공격자입니다.")
                    .path(req.getMethod() + " " + path).userAgent(ua).build());
        }

        try {
            chain.doFilter(req, res);
        } finally {
            tracker.record(ip, req.getMethod(), path, res.getStatus(), bearerUser(req), ua);
        }
    }

    private boolean carriesToken(HttpServletRequest req, String token) {
        return contains(req.getHeader("Authorization"), token)
                || contains(req.getHeader("X-API-Key"), token)
                || contains(req.getHeader("X-Api-Token"), token)
                || contains(req.getQueryString(), token)
                || contains(req.getHeader("Cookie"), token);
    }

    private static boolean contains(String v, String token) {
        return v != null && v.contains(token);
    }

    private String bearerUser(HttpServletRequest req) {
        String h = req.getHeader("Authorization");
        if (h == null || !h.startsWith("Bearer ")) return null;
        try {
            return tokenProvider.getUsernameFromToken(h.substring(7));
        } catch (Exception e) {
            return null;   // 만료·위조 토큰 — 401 로 기록되므로 사용자 표시는 생략
        }
    }

    private static String path(HttpServletRequest req) {
        String uri = req.getRequestURI();
        String ctx = req.getContextPath();
        return ctx != null && !ctx.isEmpty() && uri.startsWith(ctx) ? uri.substring(ctx.length()) : uri;
    }
}
