package com.monosun.secportal.auth.service;

import com.monosun.secportal.audit.service.AuditLogService;
import com.monosun.secportal.auth.dto.AuthDto;
import com.monosun.secportal.auth.entity.User;
import com.monosun.secportal.auth.repository.UserRepository;
import com.monosun.secportal.common.exception.BusinessException;
import com.monosun.secportal.common.security.JwtTokenProvider;
import com.monosun.secportal.setting.service.AppSettingService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditLogService auditLogService;
    private final MfaService mfaService;
    private final SecurityConfigService securityConfigService;
    private final AppSettingService appSettingService;

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

    @Transactional(noRollbackFor = BusinessException.class)
    public AuthDto.TokenResponse login(AuthDto.LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail()).orElse(null);

        if (user == null || !user.isActive()) {
            auditLogService.log("LOGIN_FAILED", "USER", null, "email=" + request.getEmail());
            throw new BusinessException("이메일 또는 비밀번호가 올바르지 않습니다");
        }

        if (user.getLockedUntil() != null && user.getLockedUntil().isAfter(LocalDateTime.now())) {
            long remaining = Math.max(1, ChronoUnit.MINUTES.between(LocalDateTime.now(), user.getLockedUntil()));
            throw new BusinessException("계정이 잠겼습니다. " + remaining + "분 후 다시 시도하세요.");
        }

        boolean authenticated;
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));
            authenticated = true;
        } catch (Exception e) {
            authenticated = false;
        }

        if (!authenticated) {
            user.setFailedLoginAttempts(user.getFailedLoginAttempts() + 1);
            int attempts = user.getFailedLoginAttempts();
            int maxAttempts = securityConfigService.getMaxAttempts();
            if (attempts >= maxAttempts) {
                int base = securityConfigService.getLockoutMinutes();
                int extra = attempts - maxAttempts;
                long lockMins = Math.min((long) base * (1L << Math.min(extra, 10)), 1440L);
                user.setLockedUntil(LocalDateTime.now().plusMinutes(lockMins));
            }
            auditLogService.log("LOGIN_FAILED", "USER", user.getId(), "email=" + request.getEmail());
            throw new BusinessException("이메일 또는 비밀번호가 올바르지 않습니다");
        }

        user.setFailedLoginAttempts(0);
        user.setLockedUntil(null);

        if (user.isMfaEnabled()) {
            return AuthDto.TokenResponse.mfaPending(tokenProvider.generateTempToken(user.getEmail()));
        }

        long expiresIn = sessionExpirationMs();
        Authentication auth = new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
        auditLogService.log("LOGIN_SUCCESS", "USER", user.getId(), "email=" + user.getEmail());
        return AuthDto.TokenResponse.of(tokenProvider.generateToken(auth, expiresIn), user, expiresIn);
    }

    @Transactional
    public AuthDto.TokenResponse verifyMfa(AuthDto.MfaVerifyRequest request) {
        if (!tokenProvider.validateToken(request.getTempToken()) || !tokenProvider.isTempToken(request.getTempToken())) {
            throw new BusinessException("유효하지 않은 토큰입니다.");
        }
        String email = tokenProvider.getUsernameFromToken(request.getTempToken());
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException("사용자를 찾을 수 없습니다."));

        if (!user.isMfaEnabled() || user.getMfaSecret() == null) {
            throw new BusinessException("MFA가 설정되지 않은 계정입니다.");
        }
        if (!mfaService.verifyCode(user.getMfaSecret(), request.getCode())) {
            throw new BusinessException("인증 코드가 올바르지 않습니다.");
        }

        long expiresIn = sessionExpirationMs();
        Authentication auth = new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
        auditLogService.log("LOGIN_SUCCESS", "USER", user.getId(), "email=" + user.getEmail());
        return AuthDto.TokenResponse.of(tokenProvider.generateToken(auth, expiresIn), user, expiresIn);
    }

    @Transactional
    public AuthDto.TokenResponse refresh(User principal) {
        User user = userRepository.findById(principal.getId())
                .orElseThrow(() -> new BusinessException("사용자를 찾을 수 없습니다"));
        long expiresIn = sessionExpirationMs();
        Authentication auth = new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
        auditLogService.log("SESSION_EXTENDED", "USER", user.getId(), "email=" + user.getEmail());
        return AuthDto.TokenResponse.of(tokenProvider.generateToken(auth, expiresIn), user, expiresIn);
    }

    @Transactional(readOnly = true)
    public AuthDto.MfaStatusResponse getMfaStatus(User principal) {
        User user = userRepository.findById(principal.getId()).orElseThrow();
        return AuthDto.MfaStatusResponse.builder().mfaEnabled(user.isMfaEnabled()).build();
    }

    @Transactional
    public AuthDto.UserInfo register(AuthDto.RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException("Email already registered");
        }
        validatePasswordStrength(request.getPassword());
        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .name(request.getName())
                .department(request.getDepartment())
                .role(User.Role.USER)
                .build();
        return AuthDto.UserInfo.from(userRepository.save(user));
    }

    private void validatePasswordStrength(String password) {
        if (password == null || password.length() < 8) {
            throw new BusinessException("비밀번호는 8자 이상이어야 합니다.");
        }
        if (!password.matches(".*[A-Z].*")) {
            throw new BusinessException("비밀번호에 대문자(A-Z)를 포함해야 합니다.");
        }
        if (!password.matches(".*[a-z].*")) {
            throw new BusinessException("비밀번호에 소문자(a-z)를 포함해야 합니다.");
        }
        if (!password.matches(".*[0-9].*")) {
            throw new BusinessException("비밀번호에 숫자(0-9)를 포함해야 합니다.");
        }
        if (!password.matches(".*[^A-Za-z0-9].*")) {
            throw new BusinessException("비밀번호에 특수문자(!@#$ 등)를 포함해야 합니다.");
        }
    }

    @Transactional(readOnly = true)
    public AuthDto.UserInfo getMe(String email) {
        return userRepository.findByEmail(email)
                .map(AuthDto.UserInfo::from)
                .orElseThrow(() -> new BusinessException("User not found"));
    }

    @Transactional
    public void changePassword(User principal, AuthDto.ChangePasswordRequest request) {
        // @AuthenticationPrincipal 객체는 현재 트랜잭션 밖에서 로드된 detached 엔티티이므로
        // 동일 트랜잭션 내에서 재조회해야 dirty checking이 정상 동작한다.
        User user = userRepository.findById(principal.getId())
                .orElseThrow(() -> new BusinessException("User not found"));
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new BusinessException("현재 비밀번호가 올바르지 않습니다.");
        }
        validatePasswordStrength(request.getNewPassword());
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setMustChangePassword(false);
        auditLogService.log("PASSWORD_CHANGED", "USER", user.getId(), "self");
    }
}
