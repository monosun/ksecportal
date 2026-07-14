package com.monosun.secportal.notification.dto;

import com.monosun.secportal.notification.entity.MailConfig;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

public class MailConfigDto {

    @Getter @Builder
    public static class ConfigResponse {
        private String host;
        private Integer port;
        private String username;
        private boolean passwordStored;
        private String passwordMasked;
        private String fromAddress;
        private String fromName;
        private boolean useAuth;
        private boolean useStartTls;
        private boolean enabled;
        private LocalDateTime updatedAt;

        public static ConfigResponse from(MailConfig c, String masked) {
            boolean stored = c != null && c.getPassword() != null && !c.getPassword().isBlank();
            return ConfigResponse.builder()
                    .host(c != null ? c.getHost() : null)
                    .port(c != null ? c.getPort() : null)
                    .username(c != null ? c.getUsername() : null)
                    .passwordStored(stored)
                    .passwordMasked(stored ? masked : null)
                    .fromAddress(c != null ? c.getFromAddress() : null)
                    .fromName(c != null ? c.getFromName() : null)
                    .useAuth(c == null || c.isUseAuth())
                    .useStartTls(c == null || c.isUseStartTls())
                    .enabled(c != null && c.isEnabled())
                    .updatedAt(c != null ? c.getUpdatedAt() : null)
                    .build();
        }
    }

    @Getter @Setter @NoArgsConstructor
    public static class ConfigRequest {
        private String host;
        private Integer port;
        private String username;
        /** 빈 값이면 기존 비밀번호 유지, "-" 이면 삭제, 그 외 값은 교체 */
        private String password;
        private String fromAddress;
        private String fromName;
        private Boolean useAuth;
        private Boolean useStartTls;
        private Boolean enabled;
    }

    @Getter @Setter @NoArgsConstructor
    public static class TestRequest {
        /** 테스트 메일 수신 주소 */
        private String to;
    }

    @Getter @Builder
    public static class TestResult {
        private boolean success;
        private String message;
    }
}
