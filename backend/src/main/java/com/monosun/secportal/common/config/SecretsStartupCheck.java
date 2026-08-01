package com.monosun.secportal.common.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 기동 시 비밀 값(DB 비밀번호·JWT 시크릿·Jasypt 마스터 키)의 보호 상태를 점검한다.
 *
 * <p>비밀 값은 {@code ENC(...)} 로 암호화해 환경변수에 넣고, 마스터 키
 * ({@code JASYPT_ENCRYPTOR_PASSWORD})는 파일(docker secret)로 주입하는 것을 권장한다.
 * 점검에 걸린 항목은 경고로 남기며, {@code SECURITY_FAIL_ON_INSECURE_SECRETS=true} 인
 * 경우 기동을 중단한다.</p>
 */
@Slf4j
@Component
public class SecretsStartupCheck {

    /** 배포본에 그대로 남아 있으면 안 되는 기본값들 */
    private static final List<String> DEFAULT_JASYPT_KEYS = List.of("dev-local-key", "your-master-key", "changeme");
    private static final String DEFAULT_DB_PASSWORD = "secportal123";
    private static final int MIN_JASYPT_KEY_LENGTH = 16;

    private final Environment env;

    @Value("${security.fail-on-insecure-secrets:false}")
    private boolean failOnInsecure;

    public SecretsStartupCheck(Environment env) {
        this.env = env;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void check() {
        List<String> issues = new ArrayList<>();

        // 1) Jasypt 마스터 키 — 이 키로 모든 ENC(...) 값이 복호화되므로 가장 중요하다
        String jasyptKey = rawEnv("JASYPT_ENCRYPTOR_PASSWORD");
        if (isBlank(jasyptKey) || DEFAULT_JASYPT_KEYS.contains(jasyptKey)) {
            issues.add("JASYPT_ENCRYPTOR_PASSWORD 가 기본값입니다. 운영 환경에서는 반드시 변경하세요.");
        } else if (jasyptKey.length() < MIN_JASYPT_KEY_LENGTH) {
            issues.add("JASYPT_ENCRYPTOR_PASSWORD 가 너무 짧습니다(" + MIN_JASYPT_KEY_LENGTH + "자 이상 권장).");
        }
        if (isBlank(rawEnv("JASYPT_ENCRYPTOR_PASSWORD_FILE"))) {
            log.info("[보안점검] Jasypt 마스터 키가 환경변수로 주입되었습니다. " +
                    "JASYPT_ENCRYPTOR_PASSWORD_FILE 로 파일 주입하면 docker inspect 에 노출되지 않습니다.");
        }

        // 2) DB 비밀번호 — ENC(...) 로 주입되었는지, 샘플 비밀번호가 아닌지
        String dbPassword = rawEnv("SPRING_DATASOURCE_PASSWORD");
        if (isBlank(dbPassword)) {
            issues.add("SPRING_DATASOURCE_PASSWORD 가 설정되지 않아 기본값이 사용됩니다.");
        } else {
            if (!isEncrypted(dbPassword)) {
                issues.add("DB 비밀번호가 평문으로 주입되었습니다. .env 의 DB_PASSWORD_ENC 에 ENC(...) 값을 넣어 암호화하세요.");
            }
            if (DEFAULT_DB_PASSWORD.equals(dbPassword)) {
                issues.add("DB 비밀번호가 샘플 값(" + DEFAULT_DB_PASSWORD + ")입니다. 반드시 변경하세요.");
            }
        }

        // 3) JWT 시크릿
        String jwtSecret = env.getProperty("jwt.secret", "");
        if (jwtSecret.contains("change-in-production")) {
            issues.add("JWT_SECRET 이 기본값입니다. 32자 이상의 임의 값으로 변경하세요.");
        } else if (jwtSecret.length() < 32) {
            issues.add("JWT_SECRET 이 32자 미만입니다.");
        }

        if (issues.isEmpty()) {
            log.info("[보안점검] 비밀 값 보호 설정 정상 (DB 비밀번호 암호화 주입 확인)");
            return;
        }

        String joined = String.join("\n  - ", issues);
        if (failOnInsecure) {
            log.error("[보안점검] 안전하지 않은 비밀 값 설정으로 기동을 중단합니다:\n  - {}", joined);
            throw new IllegalStateException("안전하지 않은 비밀 값 설정: " + String.join(" / ", issues));
        }
        log.warn("[보안점검] 비밀 값 보호 조치가 필요합니다:\n  - {}\n" +
                "  (SECURITY_FAIL_ON_INSECURE_SECRETS=true 로 두면 위 상태에서 기동이 중단됩니다)", joined);
    }

    /** 원본 환경변수 값 — Jasypt 로 복호화되기 전 값이어야 ENC(...) 여부를 판별할 수 있다. */
    private String rawEnv(String name) {
        String v = System.getenv(name);
        return v == null ? "" : v.trim();
    }

    private boolean isEncrypted(String value) {
        return value.startsWith("ENC(") && value.endsWith(")");
    }

    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
