package com.monosun.secportal.perf.web;

import com.monosun.secportal.perf.service.PerformanceService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/** 화면(요청) 처리 시간을 재서 임계시간을 넘으면 성능관리에 기록한다. */
@Component
@RequiredArgsConstructor
public class SlowRequestInterceptor implements HandlerInterceptor {

    private static final String START_ATTR = "perf.startNanos";

    private final PerformanceService performanceService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        request.setAttribute(START_ATTR, System.nanoTime());
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        Object start = request.getAttribute(START_ATTR);
        if (!(start instanceof Long startNanos)) return;
        long durationMs = (System.nanoTime() - startNanos) / 1_000_000L;

        String uri = request.getRequestURI();
        // 성능관리 화면 자체의 조회는 기록하지 않는다 (자기 자신을 계속 늘리지 않도록)
        if (uri != null && uri.contains("/admin/performance")) return;

        String query = request.getQueryString();
        String target = request.getMethod() + " " + uri;
        String detail = query == null || query.isBlank() ? null : "?" + query;

        performanceService.recordScreen(target, detail, durationMs,
                currentUsername(), clientIp(request), request.getMethod(), response.getStatus());
    }

    private String currentUsername() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            return auth != null ? auth.getName() : null;
        } catch (Exception e) {
            return null;
        }
    }

    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) return forwarded.split(",")[0].trim();
        return request.getRemoteAddr();
    }
}
