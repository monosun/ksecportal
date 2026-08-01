package com.monosun.secportal.common.crypto;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * 설정 파일(application.yml)의 개인정보 암호화 설정을 {@link ColumnCipher} 에 주입한다.
 * 설정이 비어 있으면 컨버터가 환경변수({@code PI_ENCRYPTION_KEY} → {@code JASYPT_ENCRYPTOR_PASSWORD})를 직접 읽는다.
 */
@Slf4j
@Configuration
public class ColumnCipherConfig {

    @Value("${app.privacy.column-encryption.enabled:true}")
    private boolean enabled;

    @Value("${app.privacy.column-encryption.key:}")
    private String key;

    @PostConstruct
    public void init() {
        ColumnCipher.configure(key, enabled);
        if (!enabled) {
            log.warn("[개인정보 암호화] app.privacy.column-encryption.enabled=false — 개인정보 컬럼이 평문으로 저장됩니다.");
        } else if (ColumnCipher.isEnabled()) {
            log.info("[개인정보 암호화] 개인정보 컬럼 암호화가 활성화되었습니다(AES-256-GCM).");
        }
    }
}
