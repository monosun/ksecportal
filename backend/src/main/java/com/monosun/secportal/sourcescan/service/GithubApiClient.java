package com.monosun.secportal.sourcescan.service;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * GitHub REST API 클라이언트 (https://docs.github.com/rest).
 * Fine-grained/classic PAT 모두 지원, GitHub Enterprise는 apiBaseUrl로 대응.
 */
@Component
public class GithubApiClient {

    public static final String DEFAULT_API_BASE = "https://api.github.com";
    private static final int PER_PAGE = 100;
    private static final int MAX_PAGES = 3; // 카테고리당 최대 300건

    private final RestTemplate restTemplate;

    public GithubApiClient(RestTemplateBuilder builder) {
        this.restTemplate = builder
                .setConnectTimeout(Duration.ofSeconds(10))
                .setReadTimeout(Duration.ofSeconds(30))
                .build();
    }

    public JsonNode getUser(String baseUrl, String token) {
        return exchange(baseUrl, token, "/user").getBody();
    }

    public List<JsonNode> listRepos(String baseUrl, String token) {
        return fetchPaged(baseUrl, token, "/user/repos?sort=updated&per_page=" + PER_PAGE);
    }

    public List<JsonNode> dependabotAlerts(String baseUrl, String token, String repository) {
        return fetchPaged(baseUrl, token,
                "/repos/" + repository + "/dependabot/alerts?state=open&per_page=" + PER_PAGE);
    }

    public List<JsonNode> codeScanningAlerts(String baseUrl, String token, String repository) {
        return fetchPaged(baseUrl, token,
                "/repos/" + repository + "/code-scanning/alerts?state=open&per_page=" + PER_PAGE);
    }

    public List<JsonNode> secretScanningAlerts(String baseUrl, String token, String repository) {
        return fetchPaged(baseUrl, token,
                "/repos/" + repository + "/secret-scanning/alerts?state=open&per_page=" + PER_PAGE);
    }

    /** 저장소 메타데이터 (default_branch, html_url 등) */
    public JsonNode getRepo(String baseUrl, String token, String repository) {
        return exchange(baseUrl, token, "/repos/" + repository).getBody();
    }

    /** 저장소 소스 전체를 tar.gz로 내려받는다 (내장 SAST용 — 단일 요청). */
    public byte[] downloadTarball(String baseUrl, String token, String repository, String ref) {
        String base = (baseUrl == null || baseUrl.isBlank()) ? DEFAULT_API_BASE
                : baseUrl.trim().replaceAll("/+$", "");
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.set(HttpHeaders.ACCEPT, "application/vnd.github+json");
        headers.set("X-GitHub-Api-Version", "2022-11-28");
        String path = "/repos/" + repository + "/tarball/" + (ref == null || ref.isBlank() ? "" : ref);
        try {
            ResponseEntity<byte[]> resp = restTemplate.exchange(
                    base + path, HttpMethod.GET, new HttpEntity<>(headers), byte[].class);
            return resp.getBody();
        } catch (HttpStatusCodeException e) {
            throw new GithubApiException(e.getStatusCode().value(), extractMessage(e));
        } catch (Exception e) {
            throw new GithubApiException(0, e.getMessage());
        }
    }

    /**
     * 응답의 {@code Link: <...>; rel="next"} 헤더를 따라가며 페이지를 모은다.
     *
     * <p>{@code page=N} 을 직접 붙이지 않는다 — Dependabot alerts 등 일부 API 는 커서 방식
     * ({@code after=}/{@code before=})만 지원해 {@code page} 파라미터를 보내면 400
     * ("Pagination using the `page` parameter is not supported.") 을 돌려준다.
     * Link 헤더는 두 방식 모두에서 다음 페이지 URL 을 주므로 공통으로 쓸 수 있다.
     */
    private List<JsonNode> fetchPaged(String baseUrl, String token, String pathWithQuery) {
        String base = normalizeBase(baseUrl);
        List<JsonNode> result = new ArrayList<>();
        URI next = URI.create(base + pathWithQuery);
        for (int page = 1; page <= MAX_PAGES && next != null; page++) {
            ResponseEntity<JsonNode> resp = exchange(token, next);
            JsonNode body = resp.getBody();
            if (body == null || !body.isArray() || body.isEmpty()) break;
            body.forEach(result::add);
            next = nextLink(resp.getHeaders(), base);
        }
        return result;
    }

    private static final Pattern NEXT_LINK = Pattern.compile("<([^>]+)>\\s*;\\s*rel=\"next\"");

    /**
     * Link 헤더의 rel="next" URL. 토큰이 다른 호스트로 새지 않도록 설정된 API 주소로 시작하는 URL 만 따른다.
     * URL 은 이미 인코딩된 상태라 다시 인코딩되지 않게 URI 로 그대로 넘긴다(커서의 %3D 등 보존).
     */
    private static URI nextLink(HttpHeaders headers, String base) {
        String link = headers.getFirst(HttpHeaders.LINK);
        if (link == null) return null;
        Matcher m = NEXT_LINK.matcher(link);
        if (!m.find()) return null;
        String url = m.group(1);
        if (!url.startsWith(base + "/")) return null;
        try {
            return URI.create(url);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static String normalizeBase(String baseUrl) {
        return (baseUrl == null || baseUrl.isBlank()) ? DEFAULT_API_BASE
                : baseUrl.trim().replaceAll("/+$", "");
    }

    private ResponseEntity<JsonNode> exchange(String baseUrl, String token, String path) {
        return exchange(token, URI.create(normalizeBase(baseUrl) + path));
    }

    private ResponseEntity<JsonNode> exchange(String token, URI uri) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.set(HttpHeaders.ACCEPT, "application/vnd.github+json");
        headers.set("X-GitHub-Api-Version", "2022-11-28");
        try {
            return restTemplate.exchange(uri, HttpMethod.GET, new HttpEntity<>(headers), JsonNode.class);
        } catch (HttpStatusCodeException e) {
            throw new GithubApiException(e.getStatusCode().value(), extractMessage(e));
        } catch (Exception e) {
            throw new GithubApiException(0, e.getMessage());
        }
    }

    private String extractMessage(HttpStatusCodeException e) {
        try {
            com.fasterxml.jackson.databind.ObjectMapper om = new com.fasterxml.jackson.databind.ObjectMapper();
            String msg = om.readTree(e.getResponseBodyAsString()).path("message").asText(null);
            return msg != null ? msg : e.getStatusText();
        } catch (Exception ignored) {
            return e.getStatusText();
        }
    }

    public static class GithubApiException extends RuntimeException {
        private final int status;

        public GithubApiException(int status, String message) {
            super(message);
            this.status = status;
        }

        public int getStatus() { return status; }
    }
}
