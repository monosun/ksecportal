package com.monosun.secportal.auth.controller;

import com.monosun.secportal.auth.dto.AuthDto;
import com.monosun.secportal.auth.service.OktaAuthService;
import com.monosun.secportal.common.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
public class OktaAuthController {

    private final OktaAuthService oktaAuthService;

    @GetMapping("/auth/okta/config")
    public ApiResponse<AuthDto.OktaConfigResponse> config() {
        return ApiResponse.ok(oktaAuthService.getConfig());
    }

    @PostMapping("/auth/okta/token")
    public ApiResponse<AuthDto.TokenResponse> token(@Valid @RequestBody AuthDto.OktaCallbackRequest request) {
        return ApiResponse.ok(oktaAuthService.handleCallback(request));
    }

    @GetMapping("/admin/okta/test")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Map<String, Object>> test() {
        return ApiResponse.ok(oktaAuthService.testConnection());
    }
}
