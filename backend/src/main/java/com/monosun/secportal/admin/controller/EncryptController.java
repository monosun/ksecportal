package com.monosun.secportal.admin.controller;

import com.monosun.secportal.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.jasypt.encryption.StringEncryptor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/admin/tools")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class EncryptController {

    @Qualifier("jasyptStringEncryptor")
    private final StringEncryptor encryptor;

    @PostMapping("/encrypt")
    public ApiResponse<Map<String, String>> encrypt(@RequestBody Map<String, String> body) {
        String value = body.get("value");
        if (value == null || value.isBlank()) {
            return ApiResponse.ok("value is required", Map.of());
        }
        String encrypted = "ENC(" + encryptor.encrypt(value) + ")";
        return ApiResponse.ok(Map.of("encrypted", encrypted, "hint", ".env에 위 값을 붙여넣으세요"));
    }
}
