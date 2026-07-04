package com.monosun.secportal.setting.controller;

import com.monosun.secportal.common.response.ApiResponse;
import com.monosun.secportal.setting.service.AppSettingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
public class AppSettingController {

    private final AppSettingService service;

    // 인증 없이 접근 가능 — 로그인 화면에서 로고를 로드하기 위해 public
    @GetMapping("/public/app-settings")
    public ResponseEntity<ApiResponse<Map<String, String>>> getAll() {
        return ResponseEntity.ok(ApiResponse.ok(service.getAllAsMap()));
    }

    @PutMapping("/admin/app-settings/{key}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> update(
            @PathVariable String key,
            @RequestBody Map<String, String> body) {
        service.upsert(key, body.get("value"));
        return ResponseEntity.ok(ApiResponse.noContent());
    }
}
