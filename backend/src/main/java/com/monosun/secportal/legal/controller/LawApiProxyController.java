package com.monosun.secportal.legal.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.monosun.secportal.common.response.ApiResponse;
import com.monosun.secportal.setting.service.AppSettingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.time.Duration;

@Slf4j
@RestController
@RequestMapping("/law-proxy")  // context-path(/api)가 붙어 실제 경로는 /api/law-proxy
public class LawApiProxyController {

    private static final String LAW_BASE = "https://www.law.go.kr/DRF";
    private static final String API_KEY_SETTING = "lawApiKey";
    // 연결 테스트용 — law.go.kr에 항상 존재하는 법령으로 검색해 OC 코드 유효성만 확인
    private static final String TEST_QUERY = "대한민국헌법";

    private final AppSettingService appSettingService;
    private final ObjectMapper objectMapper;
    /**
     * 공용 RestTemplate 은 타임아웃이 없어 law.go.kr 이 응답하지 않으면 요청이 그대로 매달린다.
     * 화면에 "연결 실패" 를 제때 보여주려면 이 프록시만큼은 자체 제한시간을 둔다.
     */
    private final RestTemplate restTemplate;

    public LawApiProxyController(AppSettingService appSettingService,
                                 ObjectMapper objectMapper,
                                 RestTemplateBuilder builder) {
        this.appSettingService = appSettingService;
        this.objectMapper = objectMapper;
        this.restTemplate = builder
                .setConnectTimeout(Duration.ofSeconds(5))
                .setReadTimeout(Duration.ofSeconds(20))
                .build();
    }

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

        return relay(uri);
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

        return relay(builder.build().encode(java.nio.charset.StandardCharsets.UTF_8).toUri());
    }

    /** 연결 테스트 — 등록된 OC 코드로 실제 검색을 수행해 유효성 확인 (설정관리 > API 연동) */
    @PostMapping("/test")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<TestResult> test() {
        String apiKey = resolveApiKey();
        if (apiKey == null) {
            return ApiResponse.ok(new TestResult(false, "등록된 API 키가 없습니다."));
        }

        java.net.URI uri = UriComponentsBuilder.fromHttpUrl(LAW_BASE + "/lawSearch.do")
                .queryParam("OC", apiKey)
                .queryParam("target", "law")
                .queryParam("type", "JSON")
                .queryParam("query", TEST_QUERY)
                .queryParam("display", 1)
                .queryParam("page", 1)
                .build().encode(java.nio.charset.StandardCharsets.UTF_8).toUri();

        try {
            String body = restTemplate.getForObject(uri, String.class);
            if (isNotJson(body)) {
                return ApiResponse.ok(new TestResult(false,
                        "law.go.kr이 JSON 대신 오류 페이지를 보냈습니다. OC 코드와 등록된 도메인을 확인하세요."));
            }
            JsonNode search = objectMapper.readTree(body).path("LawSearch");
            JsonNode law = search.path("law");
            boolean hasResult = law.isArray() ? !law.isEmpty() : law.isObject();
            if (hasResult) {
                return ApiResponse.ok(new TestResult(true, "연결 성공 — law.go.kr Open API 정상 응답"));
            }
            return ApiResponse.ok(new TestResult(false, "law.go.kr에 연결되었으나 결과가 없습니다. OC 코드를 확인하세요."));
        } catch (RestClientException e) {
            return ApiResponse.ok(new TestResult(false, describeFailure(e)));
        } catch (Exception e) {
            return ApiResponse.ok(new TestResult(false, "응답 처리에 실패했습니다: " + e.getMessage()));
        }
    }

    /**
     * law.go.kr 응답을 그대로 중계하되, 실패하면 사유를 담은 JSON 으로 502 를 돌려준다.
     * 예전에는 예외가 그대로 올라가 화면에 "Internal server error" 만 보였고,
     * OC 코드가 잘못돼 JSON 대신 HTML 안내가 오면 프론트가 조용히 정적 조문으로 넘어가 원인을 알 수 없었다.
     */
    private ResponseEntity<String> relay(java.net.URI uri) {
        try {
            String body = restTemplate.getForObject(uri, String.class);
            if (body == null || body.isBlank()) {
                return upstreamError("법제처(law.go.kr)가 빈 응답을 보냈습니다. 잠시 후 다시 시도해주세요.");
            }
            if (isNotJson(body)) {
                return upstreamError("법제처(law.go.kr)가 JSON 대신 오류 페이지를 보냈습니다. "
                        + "설정관리 > API 연동의 법제처 API 키(OC 코드)를 확인하세요.");
            }
            return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(body);
        } catch (RestClientException e) {
            log.warn("법제처 API 호출 실패: {}", e.toString());
            return upstreamError(describeFailure(e));
        }
    }

    /** JSON 을 요청했는데 HTML(오류 안내 페이지 등)이 온 경우 */
    private boolean isNotJson(String body) {
        if (body == null) return true;
        String head = body.stripLeading();
        return !(head.startsWith("{") || head.startsWith("["));
    }

    /** 예외를 사용자가 읽고 조치할 수 있는 한 문장으로 바꾼다 */
    private String describeFailure(RestClientException e) {
        if (e instanceof HttpStatusCodeException he) {
            return "법제처(law.go.kr) 응답 오류 " + he.getStatusCode().value() + " — 잠시 후 다시 시도해주세요.";
        }
        Throwable cause = (e instanceof ResourceAccessException && e.getCause() != null) ? e.getCause() : e;
        if (cause instanceof UnknownHostException) {
            return "law.go.kr 주소를 찾을 수 없습니다 (DNS 조회 실패). 서버의 네트워크 설정을 확인하세요.";
        }
        if (cause instanceof SocketTimeoutException) {
            return "법제처(law.go.kr) 응답 시간이 초과되었습니다. 잠시 후 다시 시도해주세요.";
        }
        if (cause instanceof ConnectException) {
            return "법제처(law.go.kr)에 접속하지 못했습니다 (연결 거부·방화벽 차단).";
        }
        String msg = cause.getMessage();
        return "법제처(law.go.kr) 연결에 실패했습니다" + (msg == null || msg.isBlank() ? "." : ": " + msg);
    }

    private ResponseEntity<String> upstreamError(String message) {
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .contentType(MediaType.APPLICATION_JSON)
                .body("{\"error\":" + jsonString(message) + ",\"message\":" + jsonString(message) + "}");
    }

    /** 메시지에 따옴표·개행이 섞여도 깨지지 않도록 JSON 문자열로 인코딩 */
    private String jsonString(String s) {
        try {
            return objectMapper.writeValueAsString(s);
        } catch (Exception e) {
            return "\"오류가 발생했습니다.\"";
        }
    }

    private String resolveApiKey() {
        String key = appSettingService.getValue(API_KEY_SETTING);
        return (key != null && !key.isBlank()) ? key.trim() : null;
    }

    private ResponseEntity<String> keyMissing() {
        String message = "법제처 API 키가 설정되지 않았습니다. 설정관리 > API 연동 탭에서 법제처 API 키(OC 코드)를 등록하세요.";
        return ResponseEntity.badRequest()
                .contentType(MediaType.APPLICATION_JSON)
                .body("{\"error\":" + jsonString(message) + ",\"message\":" + jsonString(message) + "}");
    }

    public record TestResult(boolean success, String message) {}
}
