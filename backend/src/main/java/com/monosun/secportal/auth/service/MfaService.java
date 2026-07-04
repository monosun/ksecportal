package com.monosun.secportal.auth.service;

import com.monosun.secportal.auth.dto.AuthDto;
import com.monosun.secportal.auth.entity.User;
import com.monosun.secportal.auth.repository.UserRepository;
import com.monosun.secportal.common.exception.BusinessException;
import dev.samstevens.totp.code.DefaultCodeGenerator;
import dev.samstevens.totp.code.DefaultCodeVerifier;
import dev.samstevens.totp.code.HashingAlgorithm;
import dev.samstevens.totp.qr.QrData;
import dev.samstevens.totp.secret.DefaultSecretGenerator;
import dev.samstevens.totp.time.SystemTimeProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MfaService {

    private final UserRepository userRepository;

    private final DefaultSecretGenerator secretGenerator = new DefaultSecretGenerator();
    private final DefaultCodeVerifier codeVerifier = new DefaultCodeVerifier(
            new DefaultCodeGenerator(), new SystemTimeProvider());

    @Transactional
    public AuthDto.MfaSetupResponse setupMfa(User principal) {
        User user = userRepository.findById(principal.getId()).orElseThrow();
        if (user.isMfaEnabled()) {
            throw new BusinessException("MFA가 이미 활성화되어 있습니다. 비활성화 후 재설정하세요.");
        }
        String secret = secretGenerator.generate();
        user.setMfaSecret(secret);

        String qrUri = new QrData.Builder()
                .label(user.getEmail())
                .secret(secret)
                .issuer("SecPortal")
                .algorithm(HashingAlgorithm.SHA1)
                .digits(6)
                .period(30)
                .build()
                .getUri();

        return AuthDto.MfaSetupResponse.builder()
                .secret(secret)
                .qrCodeUri(qrUri)
                .build();
    }

    @Transactional
    public void enableMfa(User principal, String code) {
        User user = userRepository.findById(principal.getId()).orElseThrow();
        if (user.getMfaSecret() == null) {
            throw new BusinessException("MFA 설정을 먼저 시작하세요.");
        }
        if (!verifyCode(user.getMfaSecret(), code)) {
            throw new BusinessException("인증 코드가 올바르지 않습니다.");
        }
        user.setMfaEnabled(true);
    }

    @Transactional
    public void disableMfa(User principal, String code) {
        User user = userRepository.findById(principal.getId()).orElseThrow();
        if (!user.isMfaEnabled()) {
            throw new BusinessException("MFA가 활성화되어 있지 않습니다.");
        }
        if (!verifyCode(user.getMfaSecret(), code)) {
            throw new BusinessException("인증 코드가 올바르지 않습니다.");
        }
        user.setMfaEnabled(false);
        user.setMfaSecret(null);
    }

    public boolean verifyCode(String secret, String code) {
        try {
            return codeVerifier.isValidCode(secret, code);
        } catch (Exception e) {
            return false;
        }
    }
}
