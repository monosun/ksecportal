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

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

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

    private List<JsonNode> fetchPaged(String baseUrl, String token, String pathWithQuery) {
        List<JsonNode> result = new ArrayList<>();
        for (int page = 1; page <= MAX_PAGES; page++) {
            JsonNode body = exchange(baseUrl, token, pathWithQuery + "&page=" + page).getBody();
            if (body == null || !body.isArray() || body.isEmpty()) break;
            body.forEach(result::add);
            if (body.size() < PER_PAGE) break;
        }
        return result;
    }

    private ResponseEntity<JsonNode> exchange(String baseUrl, String token, String path) {
        String base = (baseUrl == null || baseUrl.isBlank()) ? DEFAULT_API_BASE
                : baseUrl.trim().replaceAll("/+$", "");
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.set(HttpHeaders.ACCEPT, "application/vnd.github+json");
        headers.set("X-GitHub-Api-Version", "2022-11-28");
        try {
            return restTemplate.exchange(base + path, HttpMethod.GET, new HttpEntity<>(headers), JsonNode.class);
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
