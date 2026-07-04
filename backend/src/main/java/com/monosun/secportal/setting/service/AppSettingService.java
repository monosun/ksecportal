package com.monosun.secportal.setting.service;

import com.monosun.secportal.setting.entity.AppSetting;
import com.monosun.secportal.setting.repository.AppSettingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AppSettingService {

    private final AppSettingRepository repo;

    @Transactional(readOnly = true)
    public Map<String, String> getAllAsMap() {
        List<AppSetting> all = repo.findAll();
        Map<String, String> map = new LinkedHashMap<>();
        for (AppSetting s : all) {
            map.put(s.getKey(), s.getValue());
        }
        return map;
    }

    @Transactional(readOnly = true)
    public String getValue(String key) {
        return repo.findByKey(key).map(AppSetting::getValue).orElse(null);
    }

    @Transactional
    public void upsert(String key, String value) {
        AppSetting setting = repo.findByKey(key).orElse(new AppSetting(key, null, null));
        setting.setValue(value);
        repo.save(setting);
    }

    /**
     * 이메일·외부 링크 기준 URL 결정 — 설정관리의 app.base_url(도메인 주소) 우선,
     * 미등록 시 fallback(환경변수 APP_BASE_URL) 사용.
     * 도메인만 입력된 경우(경로 없음) 백엔드 API 경로인 /api를 자동으로 붙인다.
     */
    @Transactional(readOnly = true)
    public String resolveBaseUrl(String fallback) {
        String v = getValue("app.base_url");
        if (v == null || v.isBlank()) return fallback;
        v = v.trim().replaceAll("/+$", "");
        try {
            java.net.URI u = java.net.URI.create(v);
            String path = u.getPath();
            if (path == null || path.isEmpty() || "/".equals(path)) v = v + "/api";
        } catch (Exception ignored) { }
        return v;
    }
}
