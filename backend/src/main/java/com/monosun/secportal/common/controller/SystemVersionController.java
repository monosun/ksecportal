package com.monosun.secportal.common.controller;

import com.monosun.secportal.common.response.ApiResponse;
import org.springframework.boot.SpringBootVersion;
import org.springframework.core.SpringVersion;
import org.springframework.security.core.SpringSecurityCoreVersion;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 백엔드 기술스택 버전 조회 (환경설정 → 정보 탭).
 *
 * <p>버전을 하드코딩하지 않고 실제 로드된 라이브러리에서 읽으므로 의존성을 올리면
 * 화면 표기도 함께 따라간다. actuator는 {@code management.server.port: -1} 로
 * 비활성화되어 있어 여기서 별도로 제공한다.
 *
 * <p>핑거프린팅에 쓰일 수 있는 정보이므로 인증된 사용자만 접근할 수 있다
 * (SecurityConfig의 {@code anyRequest().authenticated()} 적용).
 */
@RestController
@RequestMapping("/system")
public class SystemVersionController {

    @GetMapping("/version")
    public ApiResponse<Map<String, String>> version() {
        Map<String, String> stack = new LinkedHashMap<>();
        stack.put("springBoot", orUnknown(SpringBootVersion.getVersion()));
        stack.put("springFramework", orUnknown(SpringVersion.getVersion()));
        stack.put("springSecurity", orUnknown(SpringSecurityCoreVersion.getVersion()));
        stack.put("hibernate", hibernateVersion());
        stack.put("java", orUnknown(System.getProperty("java.version")));
        stack.put("jvm", orUnknown(System.getProperty("java.vm.name")));
        return ApiResponse.ok(stack);
    }

    /** Hibernate는 클래스패스에 없을 수도 있으므로 리플렉션으로 안전하게 읽는다. */
    private String hibernateVersion() {
        try {
            Class<?> versionClass = Class.forName("org.hibernate.Version");
            Object v = versionClass.getMethod("getVersionString").invoke(null);
            return orUnknown(v != null ? v.toString() : null);
        } catch (Exception e) {
            return "unknown";
        }
    }

    private String orUnknown(String v) {
        return (v == null || v.isBlank()) ? "unknown" : v;
    }
}
