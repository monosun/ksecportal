package com.monosun.secportal.nvd.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.monosun.secportal.nvd.dto.NvdCveDto;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.time.Duration;

@Service
public class NvdService {

    private static final String NVD_API_URL =
            "https://services.nvd.nist.gov/rest/json/cves/2.0?cveId={cveId}";

    private final RestTemplate restTemplate;

    public NvdService(RestTemplateBuilder builder) {
        this.restTemplate = builder
                .setConnectTimeout(Duration.ofSeconds(10))
                .setReadTimeout(Duration.ofSeconds(15))
                .build();
    }

    public NvdCveDto lookup(String cveId) {
        ResponseEntity<JsonNode> response;
        try {
            response = restTemplate.getForEntity(NVD_API_URL, JsonNode.class, cveId);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "NVD API 호출 실패: " + e.getMessage());
        }

        JsonNode body = response.getBody();
        if (body == null || body.path("totalResults").asInt(0) == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "CVE를 찾을 수 없습니다: " + cveId);
        }

        JsonNode cve = body.path("vulnerabilities").get(0).path("cve");
        String description = extractDescription(cve);
        Double cvssScore = null;
        String severity = null;

        JsonNode metrics = cve.path("metrics");
        for (String key : new String[]{"cvssMetricV31", "cvssMetricV30"}) {
            JsonNode metric = metrics.path(key);
            if (!metric.isMissingNode() && metric.isArray() && !metric.isEmpty()) {
                JsonNode cvssData = metric.get(0).path("cvssData");
                cvssScore = roundScore(cvssData.path("baseScore").asDouble());
                severity = mapSeverity(cvssData.path("baseSeverity").asText(null));
                break;
            }
        }

        if (cvssScore == null) {
            JsonNode v2 = metrics.path("cvssMetricV2");
            if (!v2.isMissingNode() && v2.isArray() && !v2.isEmpty()) {
                double score = v2.get(0).path("cvssData").path("baseScore").asDouble();
                cvssScore = roundScore(score);
                severity = scoreToSeverity(score);
            }
        }

        return NvdCveDto.builder()
                .cveId(cve.path("id").asText())
                .description(description)
                .cvssScore(cvssScore)
                .severity(severity)
                .build();
    }

    private String extractDescription(JsonNode cve) {
        JsonNode descriptions = cve.path("descriptions");
        for (JsonNode desc : descriptions) {
            if ("en".equals(desc.path("lang").asText())) {
                return desc.path("value").asText();
            }
        }
        return descriptions.isEmpty() ? "" : descriptions.get(0).path("value").asText();
    }

    private String mapSeverity(String nvdSeverity) {
        if (nvdSeverity == null) return null;
        return switch (nvdSeverity.toUpperCase()) {
            case "CRITICAL" -> "CRITICAL";
            case "HIGH" -> "HIGH";
            case "MEDIUM" -> "MEDIUM";
            case "LOW" -> "LOW";
            default -> "INFO";
        };
    }

    private String scoreToSeverity(double score) {
        if (score >= 9.0) return "CRITICAL";
        if (score >= 7.0) return "HIGH";
        if (score >= 4.0) return "MEDIUM";
        if (score > 0) return "LOW";
        return "INFO";
    }

    private Double roundScore(double score) {
        return Math.round(score * 10.0) / 10.0;
    }
}
