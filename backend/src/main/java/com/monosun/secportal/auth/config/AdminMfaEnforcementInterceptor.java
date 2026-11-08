package com.monosun.secportal.auth.config;

import com.monosun.secportal.auth.entity.User;
import com.monosun.secportal.auth.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.charset.StandardCharsets;
import java.util.Set;

/**
 * 관리자 MFA 필수 정책(설정관리 &gt; 보안 설정). 켜져 있으면 MFA 를 등록하지 않은 관리자 계정은
 * 조회는 할 수 있지만 변경 작업(POST·PUT·PATCH·DELETE)은 MFA 등록(/auth/**) 외에 모두 거절된다.
 * 탈취된 관리자 비밀번호만으로 시스템을 변경하는 것을 막기 위한 보완 통제.
 */
@Configuration
public class AdminMfaEnforcementInterceptor implements HandlerInterceptor, WebMvcConfigurer {

    private static final Set<String> SAFE = Set.of("GET", "HEAD", "OPTIONS");
    private static final String BODY =
            "{\"success\":false,\"message\":\"관리자 계정은 MFA(다중 인증)를 먼저 등록해야 변경 작업을 할 수 있습니다. 설정 화면에서 MFA 를 등록하세요.\",\"data\":null}";

    private final AuthService authService;

    /** WebMvc 설정은 일찍 초기화되므로 인증 서비스는 지연 주입한다 */
    public AdminMfaEnforcementInterceptor(@Lazy AuthService authService) {
        this.authService = authService;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(this).addPathPatterns("/**").excludePathPatterns("/auth/**");
    }

    @Override
    public boolean preHandle(HttpServletRequest req, HttpServletResponse res, Object handler) throws Exception {
        if (SAFE.contains(req.getMethod())) return true;
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof User user)) return true;
        if (!authService.mfaSetupRequired(user)) return true;
        res.setStatus(HttpServletResponse.SC_FORBIDDEN);
        res.setContentType("application/json;charset=UTF-8");
        res.getOutputStream().write(BODY.getBytes(StandardCharsets.UTF_8));
        return false;
    }
}
