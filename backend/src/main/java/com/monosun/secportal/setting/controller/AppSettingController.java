package com.monosun.secportal.setting.controller;

import com.monosun.secportal.common.response.ApiResponse;
import com.monosun.secportal.setting.service.AppSettingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

@RestController
@RequiredArgsConstructor
public class AppSettingController {

    // 로그인 화면(인증 전)에서 필요한 항목만 — lawApiKey 등 민감값은 절대 포함하지 않는다
    private static final Set<String> PUBLIC_KEYS = Set.of(
            "login_logo", "login_logo_text", "session_timeout_minutes", "menu_order");

    private final AppSettingService service;

    // 인증 없이 접근 가능 — 로그인 화면에서 로고를 로드하기 위해 public. 화이트리스트 항목만 반환.
    @GetMapping("/public/app-settings")
    public ResponseEntity<ApiResponse<Map<String, String>>> getAll() {
        Map<String, String> all = service.getAllAsMap();
        Map<String, String> filtered = new LinkedHashMap<>();
        for (String key : PUBLIC_KEYS) {
            String value = all.get(key);
            if (value != null) filtered.put(key, value);
        }
        return ResponseEntity.ok(ApiResponse.ok(filtered));
    }

    // 로그인 후 화면(설정관리·대시보드 등)에서 사용 — 인증만 요구, 전체 설정값 반환
    @GetMapping("/app-settings")
    public ResponseEntity<ApiResponse<Map<String, String>>> getAllAuthenticated() {
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
