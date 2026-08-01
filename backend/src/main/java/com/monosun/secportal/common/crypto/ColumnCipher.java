package com.monosun.secportal.common.crypto;

import lombok.extern.slf4j.Slf4j;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 개인정보 컬럼 암호화기 (AES-256-GCM).
 *
 * <p>암호문은 {@code PIENC1:} 접두어 + Base64(IV(12바이트) ‖ 암호문 ‖ 인증태그) 형식이다.
 * 접두어가 없는 값은 아직 암호화되지 않은 평문으로 보고 그대로 통과시키므로,
 * 백필 중이거나 예전 백업을 복원한 직후에도 화면이 정상 동작한다.</p>
 *
 * <p>키는 {@code PI_ENCRYPTION_KEY} 를 우선 사용하고, 없으면 Jasypt 마스터 키
 * ({@code JASYPT_ENCRYPTOR_PASSWORD})를 쓴다. 두 값 모두 없으면 암호화가 비활성화된다.
 * <b>키를 잃으면 암호문은 복구할 수 없다</b> — 백업본과 분리해 보관해야 한다.</p>
 *
 * <p>JPA 컨버터는 스프링 빈이 아니므로(하이버네이트가 직접 생성) 스프링 주입 없이
 * 환경변수·시스템 프로퍼티에서 직접 키를 읽는다. 설정 파일로 키를 주려면
 * {@link #configure(String, boolean)} 로 덮어쓴다.</p>
 */
@Slf4j
public final class ColumnCipher {

    public static final String PREFIX = "PIENC1:";
    /** 복호화에 실패했을 때 화면에 표시할 값 — 원문은 DB에 그대로 남아 있다 */
    public static final String UNDECRYPTABLE = "(복호화 실패)";

    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int IV_LENGTH = 12;
    private static final int TAG_BITS = 128;

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final AtomicBoolean DECRYPT_FAILURE_LOGGED = new AtomicBoolean(false);

    /** 설정 파일로 주입된 값 — 없으면 환경변수를 본다 */
    private static volatile String configuredKey;
    private static volatile Boolean configuredEnabled;

    private static volatile SecretKeySpec cachedKey;
    private static volatile boolean resolved = false;

    private ColumnCipher() {}

    /** application.yml 등 스프링 설정값으로 키·활성 여부를 덮어쓴다 */
    public static synchronized void configure(String key, boolean enabled) {
        configuredKey = key;
        configuredEnabled = enabled;
        resolved = false;
        cachedKey = null;
    }

    public static boolean isEnabled() {
        return key() != null;
    }

    public static boolean isEncrypted(String value) {
        return value != null && value.startsWith(PREFIX);
    }

    /** 평문을 암호문으로. 암호화가 꺼져 있거나 이미 암호문이면 그대로 돌려준다. */
    public static String encrypt(String plain) {
        if (plain == null || plain.isEmpty() || isEncrypted(plain)) return plain;
        SecretKeySpec key = key();
        if (key == null) return plain;
        try {
            byte[] iv = new byte[IV_LENGTH];
            RANDOM.nextBytes(iv);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, iv));
            byte[] encrypted = cipher.doFinal(plain.getBytes(StandardCharsets.UTF_8));
            byte[] out = new byte[iv.length + encrypted.length];
            System.arraycopy(iv, 0, out, 0, iv.length);
            System.arraycopy(encrypted, 0, out, iv.length, encrypted.length);
            return PREFIX + Base64.getEncoder().encodeToString(out);
        } catch (Exception e) {
            // 암호화 실패를 평문 저장으로 넘기면 보호가 무력화되므로 저장을 중단한다
            throw new IllegalStateException("개인정보 컬럼 암호화에 실패했습니다: " + e.getMessage(), e);
        }
    }

    /** 암호문을 평문으로. 접두어가 없으면 평문으로 보고 그대로 돌려준다. */
    public static String decrypt(String stored) {
        if (!isEncrypted(stored)) return stored;
        SecretKeySpec key = key();
        if (key == null) {
            warnOnce("개인정보 암호화 키가 없어 암호문을 복호화할 수 없습니다. PI_ENCRYPTION_KEY 를 확인하세요.");
            return UNDECRYPTABLE;
        }
        try {
            byte[] raw = Base64.getDecoder().decode(stored.substring(PREFIX.length()));
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, raw, 0, IV_LENGTH));
            byte[] plain = cipher.doFinal(raw, IV_LENGTH, raw.length - IV_LENGTH);
            return new String(plain, StandardCharsets.UTF_8);
        } catch (Exception e) {
            // 키가 바뀌었거나 값이 손상된 경우 — 화면 전체가 실패하지 않도록 표시값만 대체한다
            warnOnce("개인정보 암호문 복호화에 실패했습니다(키 불일치 또는 값 손상): " + e.getMessage());
            return UNDECRYPTABLE;
        }
    }

    private static void warnOnce(String message) {
        if (DECRYPT_FAILURE_LOGGED.compareAndSet(false, true)) {
            log.error("[개인정보 암호화] {}", message);
        }
    }

    /** 암호화 키 — 없으면 null(암호화 비활성) */
    private static SecretKeySpec key() {
        if (!resolved) {
            synchronized (ColumnCipher.class) {
                if (!resolved) {
                    cachedKey = resolveKey();
                    resolved = true;
                }
            }
        }
        return cachedKey;
    }

    private static SecretKeySpec resolveKey() {
        if (Boolean.FALSE.equals(configuredEnabled)) {
            log.warn("[개인정보 암호화] 비활성 상태입니다. 개인정보 컬럼이 평문으로 저장됩니다.");
            return null;
        }
        String passphrase = firstNonBlank(
                configuredKey,
                System.getenv("PI_ENCRYPTION_KEY"),
                System.getProperty("pi.encryption.key"),
                System.getenv("JASYPT_ENCRYPTOR_PASSWORD"));
        if (isBlank(passphrase)) {
            log.warn("[개인정보 암호화] 키가 설정되지 않아 비활성 상태입니다. "
                    + "PI_ENCRYPTION_KEY 또는 JASYPT_ENCRYPTOR_PASSWORD 를 설정하세요.");
            return null;
        }
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(passphrase.getBytes(StandardCharsets.UTF_8));
            return new SecretKeySpec(digest, "AES");
        } catch (Exception e) {
            throw new IllegalStateException("개인정보 암호화 키 생성에 실패했습니다", e);
        }
    }

    private static String firstNonBlank(String... values) {
        for (String v : values) {
            if (!isBlank(v)) return v;
        }
        return null;
    }

    private static boolean isBlank(String v) {
        return v == null || v.isBlank();
    }
}
