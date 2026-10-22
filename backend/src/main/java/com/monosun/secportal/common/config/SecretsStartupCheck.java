package com.monosun.secportal.common.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 기동 시 비밀 값(DB 비밀번호·JWT 시크릿·Jasypt 마스터 키·개인정보 암호화 키)의 보호 상태를 점검한다.
 *
 * <p>비밀 값은 {@code ENC(...)} 로 암호화해 환경변수에 넣고, 마스터 키
 * ({@code JASYPT_ENCRYPTOR_PASSWORD})는 파일(docker secret)로 주입하는 것을 권장한다.</p>
 *
 * <p>점검 결과는 두 가지로 나뉜다.</p>
 * <ul>
 *   <li><b>차단</b> — 공개된 기본값·너무 짧은 키·샘플 비밀번호·개인정보 암호화 꺼짐.
 *       {@code SECURITY_FAIL_ON_INSECURE_SECRETS=true} 이면 기동을 중단한다.</li>
 *   <li><b>권고</b> — 평문 주입 등 더 안전하게 바꿀 수 있는 항목. 경고만 남긴다.</li>
 * </ul>
 *
 * <p>다른 시더·백필보다 먼저({@link Ordered#HIGHEST_PRECEDENCE}) 실행한다. 늦게 돌면
 * {@code PiColumnEncryptionBackfill} 이 이미 공개된 기본 키로 데이터를 암호화한 뒤에야 중단되어
 * 보호가 무의미해진다.</p>
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class SecretsStartupCheck implements ApplicationRunner {

    /** 배포본에 그대로 남아 있으면 안 되는 기본값들 */
    private static final List<String> DEFAULT_JASYPT_KEYS = List.of("dev-local-key", "your-master-key", "changeme");
    private static final String DEFAULT_DB_PASSWORD = "secportal123";   // sast:ignore 인증에 쓰는 비밀이 아니라, 샘플 비밀번호가 남아 있는지 비교하는 탐지용 상수다
    private static final int MIN_KEY_LENGTH = 16;

    private final Environment env;

    @Value("${security.fail-on-insecure-secrets:false}")
    private boolean failOnInsecure;

    public SecretsStartupCheck(Environment env) {
        this.env = env;
    }

    @Override
    public void run(ApplicationArguments args) {
        List<String> blocking = new ArrayList<>();
        List<String> advisory = new ArrayList<>();

        // 1) Jasypt 마스터 키 — 이 키로 모든 ENC(...) 값이 복호화되고, 개인정보 전용 키가 없으면 개인정보도 이 키로 암호화된다
        String jasyptKey = rawEnv("JASYPT_ENCRYPTOR_PASSWORD");
        if (isBlank(jasyptKey) || DEFAULT_JASYPT_KEYS.contains(jasyptKey)) {
            blocking.add("JASYPT_ENCRYPTOR_PASSWORD 가 기본값입니다. 공개된 값이라 ENC(...) 값과 개인정보 암호문을 누구나 풀 수 있습니다.");
        } else if (jasyptKey.length() < MIN_KEY_LENGTH) {
            blocking.add("JASYPT_ENCRYPTOR_PASSWORD 가 너무 짧습니다(" + MIN_KEY_LENGTH + "자 이상).");
        }
        if (isBlank(rawEnv("JASYPT_ENCRYPTOR_PASSWORD_FILE"))) {
            log.info("[보안점검] Jasypt 마스터 키가 환경변수로 주입되었습니다. " +
                    "JASYPT_ENCRYPTOR_PASSWORD_FILE 로 파일 주입하면 docker inspect 에 노출되지 않습니다.");
        }

        // 2) DB 비밀번호 — 샘플 비밀번호가 아닌지, ENC(...) 로 주입되었는지
        String dbPassword = rawEnv("SPRING_DATASOURCE_PASSWORD");
        if (isBlank(dbPassword)) {
            advisory.add("SPRING_DATASOURCE_PASSWORD 가 설정되지 않아 기본값이 사용됩니다.");
        } else {
            if (DEFAULT_DB_PASSWORD.equals(dbPassword)) {
                blocking.add("DB 비밀번호가 샘플 값(" + DEFAULT_DB_PASSWORD + ")입니다. 반드시 변경하세요.");
            }
            if (!isEncrypted(dbPassword)) {
                advisory.add("DB 비밀번호가 평문으로 주입되었습니다. .env 의 DB_PASSWORD_ENC 에 ENC(...) 값을 넣어 암호화하세요.");
            }
        }

        // 3) JWT 시크릿 — 기본값이면 누구나 관리자 토큰을 만들 수 있다
        String jwtSecret = env.getProperty("jwt.secret", "");
        if (jwtSecret.contains("change-in-production")) {
            blocking.add("JWT_SECRET 이 기본값입니다. 32자 이상의 임의 값으로 변경하세요.");
        } else if (jwtSecret.length() < 32) {
            blocking.add("JWT_SECRET 이 32자 미만입니다.");
        }

        // 4) 개인정보 컬럼 암호화 키 — 잃으면 데이터를 복구할 수 없으므로 별도 키 사용을 권한다
        boolean piEncryptionEnabled = env.getProperty("app.privacy.column-encryption.enabled", Boolean.class, true);
        String piKey = firstNonBlank(env.getProperty("app.privacy.column-encryption.key"), rawEnv("PI_ENCRYPTION_KEY"));
        if (!piEncryptionEnabled) {
            blocking.add("개인정보 컬럼 암호화가 꺼져 있습니다(PI_COLUMN_ENCRYPTION_ENABLED). 개인정보가 평문으로 저장됩니다.");
        } else if (isBlank(piKey)) {
            log.info("[보안점검] 개인정보 컬럼 암호화가 Jasypt 마스터 키를 사용합니다. "
                    + "PI_ENCRYPTION_KEY 로 전용 키를 두면 마스터 키 교체와 분리할 수 있습니다.");
        } else if (DEFAULT_JASYPT_KEYS.contains(piKey) || piKey.length() < MIN_KEY_LENGTH) {
            blocking.add("PI_ENCRYPTION_KEY 가 기본값이거나 너무 짧습니다(" + MIN_KEY_LENGTH + "자 이상).");
        }

        if (!advisory.isEmpty()) {
            log.warn("[보안점검] 권고 사항:\n  - {}", String.join("\n  - ", advisory));
        }
        if (blocking.isEmpty()) {
            log.info("[보안점검] 비밀 값 보호 설정 정상");
            return;
        }

        String joined = String.join("\n  - ", blocking);
        if (failOnInsecure) {
            log.error("[보안점검] 안전하지 않은 비밀 값 설정으로 기동을 중단합니다:\n  - {}", joined);
            throw new IllegalStateException("안전하지 않은 비밀 값 설정: " + String.join(" / ", blocking));
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

    private String firstNonBlank(String... values) {
        for (String v : values) {
            if (!isBlank(v)) return v;
        }
        return "";
    }

    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
