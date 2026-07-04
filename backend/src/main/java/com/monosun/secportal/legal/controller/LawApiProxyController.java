package com.monosun.secportal.legal.controller;

import com.monosun.secportal.setting.service.AppSettingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@RestController
@RequestMapping("/law-proxy")  // context-path(/api)가 붙어 실제 경로는 /api/law-proxy
@RequiredArgsConstructor
public class LawApiProxyController {

    private static final String LAW_BASE = "https://www.law.go.kr/DRF";
    private static final String API_KEY_SETTING = "lawApiKey";

    private final AppSettingService appSettingService;
    private final RestTemplate restTemplate;

    /** 법령 검색 — 법령명으로 MST 번호 조회 (target: law | admrul) */
    @GetMapping("/search")
    public ResponseEntity<String> search(
            @RequestParam String query,
            @RequestParam(defaultValue = "law") String target) {
        String apiKey = resolveApiKey();
        if (apiKey == null) return keyMissing();

        // 한글 검색어는 raw로 들어오므로 build 후 encode (build(true)는 이미 인코딩된 값 전제 → 예외)
        java.net.URI uri = UriComponentsBuilder.fromHttpUrl(LAW_BASE + "/lawSearch.do")
                .queryParam("OC", apiKey)
                .queryParam("target", target)
                .queryParam("type", "JSON")
                .queryParam("query", query)
                .queryParam("display", 100)
                .queryParam("page", 1)
                .build().encode(java.nio.charset.StandardCharsets.UTF_8).toUri();

        String body = restTemplate.getForObject(uri, String.class);
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(body);
    }

    /** 법령 전문 조회 — MST 번호로 전체 조문 가져오기 (target: law | admrul) */
    @GetMapping("/content")
    public ResponseEntity<String> content(
            @RequestParam String mst,
            @RequestParam(defaultValue = "law") String target) {
        String apiKey = resolveApiKey();
        if (apiKey == null) return keyMissing();

        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(LAW_BASE + "/lawService.do")
                .queryParam("OC", apiKey)
                .queryParam("target", target)
                .queryParam("type", "JSON");
        // 행정규칙(고시·규정·세칙)은 MST 대신 ID(행정규칙일련번호) 파라미터를 사용
        if ("admrul".equals(target)) builder.queryParam("ID", mst);
        else builder.queryParam("MST", mst);

        String body = restTemplate.getForObject(
                builder.build().encode(java.nio.charset.StandardCharsets.UTF_8).toUri(), String.class);
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(body);
    }

    private String resolveApiKey() {
        String key = appSettingService.getValue(API_KEY_SETTING);
        return (key != null && !key.isBlank()) ? key.trim() : null;
    }

    private ResponseEntity<String> keyMissing() {
        return ResponseEntity.badRequest()
                .contentType(MediaType.APPLICATION_JSON)
                .body("{\"error\":\"API 키가 설정되지 않았습니다. 설정관리 > API 연동 탭에서 법제처 API 키를 등록하세요.\"}");
    }
}
