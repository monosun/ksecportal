package com.monosun.secportal.auth.dto;

import com.monosun.secportal.auth.entity.User;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

public class AuthDto {

    @Getter
    public static class LoginRequest {
        @NotBlank @Email
        private String email;
        @NotBlank
        private String password;
    }

    @Getter
    public static class RegisterRequest {
        @NotBlank @Email
        private String email;
        @NotBlank @Size(min = 8, max = 100)
        private String password;
        @NotBlank @Size(max = 100)
        private String name;
        private String department;
    }

    @Getter
    @Builder
    public static class TokenResponse {
        private boolean mfaRequired;
        private String tempToken;
        private String accessToken;
        private String tokenType;
        private long expiresIn;   // milliseconds until token expiry
        private UserInfo user;

        public static TokenResponse of(String token, User user, long expiresIn) {
            return TokenResponse.builder()
                    .mfaRequired(false)
                    .accessToken(token)
                    .tokenType("Bearer")
                    .expiresIn(expiresIn)
                    .user(UserInfo.from(user))
                    .build();
        }

        public static TokenResponse mfaPending(String tempToken) {
            return TokenResponse.builder()
                    .mfaRequired(true)
                    .tempToken(tempToken)
                    .build();
        }
    }

    @Getter
    public static class MfaVerifyRequest {
        @NotBlank
        private String tempToken;
        @NotBlank
        private String code;
    }

    @Getter
    public static class MfaCodeRequest {
        @NotBlank
        private String code;
    }

    @Getter
    @Builder
    public static class MfaSetupResponse {
        private String secret;
        private String qrCodeUri;
    }

    @Getter
    @Builder
    public static class MfaStatusResponse {
        private boolean mfaEnabled;
    }

    @Getter
    public static class ChangePasswordRequest {
        @NotBlank
        private String currentPassword;
        @NotBlank @Size(min = 8, max = 100)
        private String newPassword;
    }

    @Getter
    public static class OktaCallbackRequest {
        @NotBlank
        private String code;
        @NotBlank
        private String codeVerifier;
    }

    @Getter
    @Builder
    public static class OktaConfigResponse {
        private boolean enabled;
        private String clientId;
        private String issuer;
        private String redirectUri;
    }

    @Getter
    @Builder
    public static class UserInfo {
        private Long id;
        private String email;
        private String name;
        private String role;
        private String department;
        private boolean mustChangePassword;

        public static UserInfo from(User user) {
            return UserInfo.builder()
                    .id(user.getId())
                    .email(user.getEmail())
                    .name(user.getName())
                    .role(user.getRole().name())
                    .department(user.getDepartment())
                    .mustChangePassword(user.isMustChangePassword())
                    .build();
        }
    }
}
