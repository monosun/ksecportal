package com.monosun.secportal.auth.controller;

import com.monosun.secportal.auth.dto.AuthDto;
import com.monosun.secportal.auth.entity.User;
import com.monosun.secportal.auth.service.AuthService;
import com.monosun.secportal.auth.service.MfaService;
import com.monosun.secportal.common.exception.UnauthorizedException;
import com.monosun.secportal.common.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final MfaService mfaService;

    @PostMapping("/login")
    public ApiResponse<AuthDto.TokenResponse> login(@Valid @RequestBody AuthDto.LoginRequest request) {
        return ApiResponse.ok(authService.login(request));
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<AuthDto.UserInfo> register(@Valid @RequestBody AuthDto.RegisterRequest request) {
        return ApiResponse.created(authService.register(request));
    }

    @GetMapping("/me")
    public ApiResponse<AuthDto.UserInfo> me(@AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            throw new UnauthorizedException("Authentication required");
        }
        return ApiResponse.ok(authService.getMe(userDetails.getUsername()));
    }

    @PatchMapping("/password")
    public ApiResponse<Void> changePassword(
            @Valid @RequestBody AuthDto.ChangePasswordRequest request,
            @AuthenticationPrincipal User user) {
        authService.changePassword(user, request);
        return ApiResponse.ok(null);
    }

    @PostMapping("/refresh")
    public ApiResponse<AuthDto.TokenResponse> refresh(@AuthenticationPrincipal User user) {
        if (user == null) throw new UnauthorizedException("Authentication required");
        return ApiResponse.ok(authService.refresh(user));
    }

    @PostMapping("/mfa/verify")
    public ApiResponse<AuthDto.TokenResponse> verifyMfa(@Valid @RequestBody AuthDto.MfaVerifyRequest request) {
        return ApiResponse.ok(authService.verifyMfa(request));
    }

    @GetMapping("/mfa/status")
    public ApiResponse<AuthDto.MfaStatusResponse> getMfaStatus(@AuthenticationPrincipal User user) {
        return ApiResponse.ok(authService.getMfaStatus(user));
    }

    @PostMapping("/mfa/setup")
    public ApiResponse<AuthDto.MfaSetupResponse> setupMfa(@AuthenticationPrincipal User user) {
        return ApiResponse.ok(mfaService.setupMfa(user));
    }

    @PostMapping("/mfa/enable")
    public ApiResponse<Void> enableMfa(
            @Valid @RequestBody AuthDto.MfaCodeRequest request,
            @AuthenticationPrincipal User user) {
        mfaService.enableMfa(user, request.getCode());
        return ApiResponse.ok(null);
    }

    @PostMapping("/mfa/disable")
    public ApiResponse<Void> disableMfa(
            @Valid @RequestBody AuthDto.MfaCodeRequest request,
            @AuthenticationPrincipal User user) {
        mfaService.disableMfa(user, request.getCode());
        return ApiResponse.ok(null);
    }
}
