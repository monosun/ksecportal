package com.monosun.secportal.auth.config;

import com.monosun.secportal.auth.entity.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.charset.StandardCharsets;

/**
 * 초기 비밀번호 변경 강제. 프론트 라우터 가드만으로는 API 직접 호출을 막을 수 없으므로 서버에서도 거절한다.
 * 기본 관리자 비밀번호는 저장소에 공개되어 있어, 변경 전 계정은 조회를 포함한 모든 업무 API 를 쓸 수 없다.
 */
@Configuration
public class PasswordChangeEnforcementInterceptor implements HandlerInterceptor, WebMvcConfigurer {

    private static final String BODY =
            "{\"success\":false,\"message\":\"초기 비밀번호를 변경한 뒤 사용할 수 있습니다.\",\"data\":null}";

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(this).addPathPatterns("/**").excludePathPatterns(
                "/auth/me", "/auth/password", "/auth/refresh",
                // 인증 없이도 공개된 경로 — 변경 화면의 로고·코드 조회가 토큰을 달고 와도 막지 않는다
                "/public/**", "/codes/**", "/error");
    }

    @Override
    public boolean preHandle(HttpServletRequest req, HttpServletResponse res, Object handler) throws Exception {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof User user) || !user.isMustChangePassword()) {
            return true;
        }
        res.setStatus(HttpServletResponse.SC_FORBIDDEN);
        res.setContentType("application/json;charset=UTF-8");
        res.getOutputStream().write(BODY.getBytes(StandardCharsets.UTF_8));
        return false;
    }
}
