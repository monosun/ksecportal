package com.monosun.secportal.auth.service;

import com.monosun.secportal.audit.service.AuditLogService;
import com.monosun.secportal.auth.dto.AuthDto;
import com.monosun.secportal.auth.entity.User;
import com.monosun.secportal.auth.repository.UserRepository;
import com.monosun.secportal.common.exception.BusinessException;
import com.monosun.secportal.common.security.JwtTokenProvider;
import com.monosun.secportal.setting.service.AppSettingService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtDecoders;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OktaAuthService {

    @Value("${okta.enabled:false}")
    private boolean envEnabled;

    @Value("${okta.client-id:}")
    private String envClientId;

    @Value("${okta.issuer:}")
    private String envIssuer;

    @Value("${okta.redirect-uri:}")
    private String envRedirectUri;

    private final UserRepository userRepository;
    private final JwtTokenProvider tokenProvider;
    private final AuditLogService auditLogService;
    private final AppSettingService appSettingService;
    private final PasswordEncoder passwordEncoder;
    private final RestTemplate restTemplate;

    private volatile JwtDecoder jwtDecoder;
    private volatile String cachedDecoderIssuer;

    private boolean resolveEnabled() {
        String db = appSettingService.getValue("okta.enabled");
        return db != null ? Boolean.parseBoolean(db) : envEnabled;
    }

    private String resolveClientId() {
        String db = appSettingService.getValue("okta.client_id");
        return (db != null && !db.isBlank()) ? db : envClientId;
    }

    private String resolveIssuer() {
        String db = appSettingService.getValue("okta.issuer");
        return (db != null && !db.isBlank()) ? db : envIssuer;
    }

    private String resolveRedirectUri() {
        String db = appSettingService.getValue("okta.redirect_uri");
        return (db != null && !db.isBlank()) ? db : envRedirectUri;
    }

    private long sessionExpirationMs() {
        try {
            String val = appSettingService.getValue("session_timeout_minutes");
            if (val != null && !val.isBlank()) {
                long minutes = Long.parseLong(val.trim());
                if (minutes >= 1) return minutes * 60_000L;
            }
        } catch (Exception ignored) {}
        return tokenProvider.getExpiration();
    }

    public boolean isEnabled() {
        return resolveEnabled() && !resolveClientId().isBlank() && !resolveIssuer().isBlank();
    }

    public AuthDto.OktaConfigResponse getConfig() {
        if (!isEnabled()) {
            return AuthDto.OktaConfigResponse.builder().enabled(false).build();
        }
        return AuthDto.OktaConfigResponse.builder()
                .enabled(true)
                .clientId(resolveClientId())
                .issuer(resolveIssuer())
                .redirectUri(resolveRedirectUri())
                .build();
    }

    public Map<String, Object> testConnection() {
        Map<String, Object> result = new LinkedHashMap<>();
        String effectiveIssuer = resolveIssuer();
        result.put("enabled", isEnabled());
        result.put("clientId", resolveClientId().isBlank() ? null : resolveClientId());
        result.put("issuer", effectiveIssuer.isBlank() ? null : effectiveIssuer);
        result.put("redirectUri", resolveRedirectUri().isBlank() ? null : resolveRedirectUri());

        if (!isEnabled()) {
            result.put("ok", false);
            result.put("message", "Okta 연동이 비활성화되어 있습니다.");
            return result;
        }

        try {
            String discoveryUrl = effectiveIssuer + "/.well-known/openid-configuration";
            restTemplate.getForObject(discoveryUrl, Map.class);
            result.put("ok", true);
            result.put("message", "Okta 서버 연결 성공");
        } catch (Exception e) {
            result.put("ok", false);
            result.put("message", "Okta 서버 연결 실패: " + e.getMessage());
        }
        return result;
    }

    @Transactional
    public AuthDto.TokenResponse handleCallback(AuthDto.OktaCallbackRequest request) {
        if (!isEnabled()) {
            throw new BusinessException("Okta 로그인이 비활성화되어 있습니다.");
        }

        Map<String, Object> tokenResponse = exchangeCodeForToken(
                request.getCode(), request.getCodeVerifier(), resolveClientId(), resolveIssuer(), resolveRedirectUri());
        String idToken = (String) tokenResponse.get("id_token");
        if (idToken == null) {
            throw new BusinessException("Okta 토큰 교환에 실패했습니다.");
        }

        Jwt jwt = validateIdToken(idToken, resolveIssuer());
        String sub = jwt.getSubject();
        String email = jwt.getClaimAsString("email");
        String name = jwt.getClaimAsString("name");

        if (email == null) {
            throw new BusinessException("Okta 계정에 이메일 정보가 없습니다. 관리자에게 문의하세요.");
        }

        User user = findOrCreateUser(sub, email, name);

        long expiresIn = sessionExpirationMs();
        Authentication auth = new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
        auditLogService.log("LOGIN_SUCCESS", "USER", user.getId(), "email=" + user.getEmail() + ",method=okta");
        return AuthDto.TokenResponse.of(tokenProvider.generateToken(auth, expiresIn), user, expiresIn);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> exchangeCodeForToken(
            String code, String codeVerifier, String clientId, String issuer, String redirectUri) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("grant_type", "authorization_code");
        params.add("client_id", clientId);
        params.add("code", code);
        params.add("code_verifier", codeVerifier);
        params.add("redirect_uri", redirectUri);

        try {
            Map<String, Object> response = restTemplate.postForObject(
                    issuer + "/v1/token",
                    new HttpEntity<>(params, headers),
                    Map.class);
            if (response == null) throw new BusinessException("Okta 서버로부터 응답이 없습니다.");
            return response;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException("Okta 인증에 실패했습니다: " + e.getMessage());
        }
    }

    private Jwt validateIdToken(String idToken, String effectiveIssuer) {
        try {
            if (jwtDecoder == null || !effectiveIssuer.equals(cachedDecoderIssuer)) {
                synchronized (this) {
                    if (jwtDecoder == null || !effectiveIssuer.equals(cachedDecoderIssuer)) {
                        jwtDecoder = JwtDecoders.fromIssuerLocation(effectiveIssuer);
                        cachedDecoderIssuer = effectiveIssuer;
                    }
                }
            }
            return jwtDecoder.decode(idToken);
        } catch (Exception e) {
            throw new BusinessException("Okta 토큰 검증에 실패했습니다.");
        }
    }

    private User findOrCreateUser(String sub, String email, String name) {
        return userRepository.findByOktaId(sub).orElseGet(() ->
            userRepository.findByEmail(email).map(existing -> {
                if (!existing.isActive()) {
                    throw new BusinessException("비활성화된 계정입니다.");
                }
                existing.setOktaId(sub);
                return userRepository.save(existing);
            }).orElseGet(() -> {
                User newUser = User.builder()
                        .email(email)
                        .name(name != null ? name : email)
                        .password(passwordEncoder.encode(UUID.randomUUID().toString()))
                        .oktaId(sub)
                        .role(User.Role.USER)
                        .build();
                return userRepository.save(newUser);
            })
        );
    }
}
