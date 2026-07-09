package com.monosun.secportal.sourcescan.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.monosun.secportal.audit.service.AuditLogService;
import com.monosun.secportal.common.exception.BusinessException;
import com.monosun.secportal.common.exception.ResourceNotFoundException;
import com.monosun.secportal.sourcescan.dto.SourceScanDto;
import com.monosun.secportal.sourcescan.entity.GithubConfig;
import com.monosun.secportal.sourcescan.entity.SourceScan;
import com.monosun.secportal.sourcescan.entity.SourceScanFinding;
import com.monosun.secportal.sourcescan.repository.GithubConfigRepository;
import com.monosun.secportal.sourcescan.repository.SourceScanRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SourceScanService {

    private final GithubConfigRepository configRepository;
    private final SourceScanRepository scanRepository;
    private final GithubApiClient githubApiClient;
    private final SastEngine sastEngine;
    private final AuditLogService auditLogService;

    // ── GitHub 연동 설정 ─────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public SourceScanDto.ConfigResponse getConfig() {
        GithubConfig config = configRepository.findFirstByOrderByIdAsc().orElse(null);
        boolean stored = config != null && config.getToken() != null && !config.getToken().isBlank();
        return SourceScanDto.ConfigResponse.builder()
                .tokenStored(stored)
                .tokenMasked(stored ? mask(config.getToken()) : null)
                .apiBaseUrl(config != null ? config.getApiBaseUrl() : null)
                .owner(config != null ? config.getOwner() : null)
                .updatedAt(config != null ? config.getUpdatedAt() : null)
                .build();
    }

    @Transactional
    public SourceScanDto.ConfigResponse saveConfig(SourceScanDto.ConfigRequest req) {
        GithubConfig config = configRepository.findFirstByOrderByIdAsc()
                .orElseGet(() -> GithubConfig.builder().build());
        if (req.getToken() != null && !req.getToken().isBlank()) {
            // "-" 입력 시 토큰 삭제, 그 외 값은 교체 (빈 값은 기존 유지)
            config.setToken("-".equals(req.getToken().trim()) ? null : req.getToken().trim());
        }
        if (req.getApiBaseUrl() != null) config.setApiBaseUrl(blankToNull(req.getApiBaseUrl()));
        if (req.getOwner() != null) config.setOwner(blankToNull(req.getOwner()));
        configRepository.save(config);
        auditLogService.log("GITHUB_CONFIG_UPDATED", "SOURCE_SCAN", config.getId(), "GitHub 연동 설정 변경");
        return getConfig();
    }

    @Transactional(readOnly = true)
    public SourceScanDto.TestResult testConnection() {
        GithubConfig config = requireConfig();
        try {
            JsonNode user = githubApiClient.getUser(config.getApiBaseUrl(), config.getToken());
            String login = user != null ? user.path("login").asText("?") : "?";
            return SourceScanDto.TestResult.builder()
                    .success(true)
                    .message("연결 성공 — 계정: " + login)
                    .build();
        } catch (GithubApiClient.GithubApiException e) {
            return SourceScanDto.TestResult.builder()
                    .success(false)
                    .message("연결 실패 (HTTP " + e.getStatus() + "): " + e.getMessage())
                    .build();
        }
    }

    // ── 저장소 목록 ──────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<SourceScanDto.RepoResponse> listRepos() {
        GithubConfig config = requireConfig();
        try {
            return githubApiClient.listRepos(config.getApiBaseUrl(), config.getToken()).stream()
                    .map(r -> SourceScanDto.RepoResponse.builder()
                            .fullName(r.path("full_name").asText())
                            .privateRepo(r.path("private").asBoolean(false))
                            .defaultBranch(r.path("default_branch").asText(null))
                            .description(r.path("description").asText(null))
                            .updatedAt(r.path("updated_at").asText(null))
                            .build())
                    .toList();
        } catch (GithubApiClient.GithubApiException e) {
            throw new BusinessException("저장소 목록 조회 실패 (HTTP " + e.getStatus() + "): " + e.getMessage());
        }
    }

    // ── 점검 실행 ────────────────────────────────────────────────────────────

    @Transactional
    public SourceScanDto.ScanDetailResponse runScan(String repository) {
        GithubConfig config = requireConfig();
        String repo = repository.trim();
        if (!repo.matches("[\\w.\\-]+/[\\w.\\-]+")) {
            throw new BusinessException("저장소는 owner/repo 형식으로 입력하세요: " + repository);
        }

        SourceScan scan = SourceScan.builder().repository(repo).build();
        List<String> notes = new ArrayList<>();
        int okCategories = 0;
        final int TOTAL_CATEGORIES = 4;

        // 1) 의존성 취약점 (Dependabot alerts)
        try {
            for (JsonNode alert : githubApiClient.dependabotAlerts(config.getApiBaseUrl(), config.getToken(), repo)) {
                JsonNode advisory = alert.path("security_advisory");
                addFinding(scan, SourceScanFinding.Category.DEPENDENCY,
                        SourceScanFinding.Severity.fromString(advisory.path("severity").asText(null)),
                        advisory.path("summary").asText("의존성 취약점"),
                        alert.path("security_vulnerability").path("package").path("name").asText(null),
                        alert.path("dependency").path("manifest_path").asText(null),
                        advisory.path("cve_id").asText(null),
                        alert.path("html_url").asText(null));
            }
            okCategories++;
        } catch (GithubApiClient.GithubApiException e) {
            notes.add(categoryNote("의존성(Dependabot)", e));
        }

        // 2) 코드 취약점 (Code scanning alerts — CodeQL 등)
        try {
            for (JsonNode alert : githubApiClient.codeScanningAlerts(config.getApiBaseUrl(), config.getToken(), repo)) {
                JsonNode rule = alert.path("rule");
                JsonNode location = alert.path("most_recent_instance").path("location");
                String loc = location.path("path").asText(null);
                if (loc != null && location.hasNonNull("start_line")) {
                    loc += ":" + location.path("start_line").asInt();
                }
                addFinding(scan, SourceScanFinding.Category.CODE_SCANNING,
                        codeSeverity(rule),
                        rule.path("description").asText(rule.path("id").asText("코드 취약점")),
                        rule.path("id").asText(null),
                        loc,
                        null,
                        alert.path("html_url").asText(null));
            }
            okCategories++;
        } catch (GithubApiClient.GithubApiException e) {
            notes.add(categoryNote("코드(Code scanning)", e));
        }

        // 3) 시크릿 노출 (Secret scanning alerts)
        try {
            for (JsonNode alert : githubApiClient.secretScanningAlerts(config.getApiBaseUrl(), config.getToken(), repo)) {
                addFinding(scan, SourceScanFinding.Category.SECRET,
                        SourceScanFinding.Severity.CRITICAL,
                        "시크릿 노출: " + alert.path("secret_type_display_name").asText(alert.path("secret_type").asText("unknown")),
                        alert.path("secret_type").asText(null),
                        null,
                        null,
                        alert.path("html_url").asText(null));
            }
            okCategories++;
        } catch (GithubApiClient.GithubApiException e) {
            notes.add(categoryNote("시크릿(Secret scanning)", e));
        }

        // 4) 내장 OWASP 정적분석 (소스코드 직접 점검)
        try {
            JsonNode repoInfo = githubApiClient.getRepo(config.getApiBaseUrl(), config.getToken(), repo);
            String branch = repoInfo != null ? repoInfo.path("default_branch").asText(null) : null;
            String htmlBase = repoInfo != null ? repoInfo.path("html_url").asText(null) : null;
            byte[] tarball = githubApiClient.downloadTarball(config.getApiBaseUrl(), config.getToken(), repo, branch);
            for (SastEngine.Hit hit : sastEngine.scanArchive(tarball)) {
                String link = (htmlBase != null && branch != null)
                        ? htmlBase + "/blob/" + branch + "/" + hit.path() + "#L" + hit.line() : null;
                addFinding(scan, SourceScanFinding.Category.SAST, hit.severity(),
                        hit.title(), hit.owasp() + " · " + hit.cwe(), hit.path() + ":" + hit.line(), null, link);
            }
            okCategories++;
        } catch (GithubApiClient.GithubApiException e) {
            notes.add(categoryNote("정적분석(SAST)", e));
        }

        scan.setStatus(okCategories == TOTAL_CATEGORIES ? SourceScan.Status.SUCCESS
                : okCategories > 0 ? SourceScan.Status.PARTIAL
                : SourceScan.Status.FAILED);
        scan.setMessage(notes.isEmpty() ? null : String.join("\n", notes));

        SourceScan saved = scanRepository.save(scan);
        auditLogService.log("SOURCE_SCAN_RUN", "SOURCE_SCAN", saved.getId(),
                repo + " — " + saved.getStatus() + " (발견 " +
                (saved.getDependencyCount() + saved.getCodeCount() + saved.getSecretCount() + saved.getSastCount()) + "건)");
        return SourceScanDto.ScanDetailResponse.from(saved);
    }

    // ── 이력 ────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Page<SourceScanDto.ScanResponse> listScans(Pageable pageable) {
        return scanRepository.findAllByOrderByCreatedAtDesc(pageable).map(SourceScanDto.ScanResponse::from);
    }

    @Transactional(readOnly = true)
    public SourceScanDto.ScanDetailResponse getScan(Long id) {
        return SourceScanDto.ScanDetailResponse.from(findScan(id));
    }

    @Transactional
    public void deleteScan(Long id) {
        SourceScan scan = findScan(id);
        auditLogService.log("SOURCE_SCAN_DELETED", "SOURCE_SCAN", id, scan.getRepository());
        scanRepository.delete(scan);
    }

    // ── 내부 유틸 ────────────────────────────────────────────────────────────

    private void addFinding(SourceScan scan, SourceScanFinding.Category category,
                            SourceScanFinding.Severity severity, String title,
                            String identifier, String location, String cveId, String htmlUrl) {
        SourceScanFinding finding = SourceScanFinding.builder()
                .scan(scan)
                .category(category)
                .severity(severity)
                .severityRank(severity.rank())
                .title(truncate(title, 500))
                .identifier(truncate(identifier, 300))
                .location(truncate(location, 500))
                .cveId(blankToNull(cveId))
                .htmlUrl(truncate(htmlUrl, 500))
                .build();
        scan.getFindings().add(finding);

        switch (severity) {
            case CRITICAL -> scan.setCriticalCount(scan.getCriticalCount() + 1);
            case HIGH -> scan.setHighCount(scan.getHighCount() + 1);
            case MEDIUM -> scan.setMediumCount(scan.getMediumCount() + 1);
            default -> scan.setLowCount(scan.getLowCount() + 1);
        }
        switch (category) {
            case DEPENDENCY -> scan.setDependencyCount(scan.getDependencyCount() + 1);
            case CODE_SCANNING -> scan.setCodeCount(scan.getCodeCount() + 1);
            case SECRET -> scan.setSecretCount(scan.getSecretCount() + 1);
            case SAST -> scan.setSastCount(scan.getSastCount() + 1);
        }
    }

    private SourceScanFinding.Severity codeSeverity(JsonNode rule) {
        String securityLevel = rule.path("security_severity_level").asText(null);
        if (securityLevel != null && !securityLevel.isBlank()) {
            return SourceScanFinding.Severity.fromString(securityLevel);
        }
        return switch (rule.path("severity").asText("")) {
            case "error" -> SourceScanFinding.Severity.HIGH;
            case "warning" -> SourceScanFinding.Severity.MEDIUM;
            default -> SourceScanFinding.Severity.LOW;
        };
    }

    private String categoryNote(String label, GithubApiClient.GithubApiException e) {
        // 403/404는 대부분 해당 기능 미활성 또는 토큰 권한 부족
        if (e.getStatus() == 404 || e.getStatus() == 403) {
            return label + ": 조회 불가 — 저장소에서 기능이 비활성이거나 토큰 권한이 없습니다 (HTTP " + e.getStatus() + ")";
        }
        return label + ": 조회 실패 (HTTP " + e.getStatus() + "): " + e.getMessage();
    }

    private GithubConfig requireConfig() {
        GithubConfig config = configRepository.findFirstByOrderByIdAsc().orElse(null);
        if (config == null || config.getToken() == null || config.getToken().isBlank()) {
            throw new BusinessException("GitHub 연동이 설정되지 않았습니다. 설정관리 > API 연동에서 토큰을 등록하세요.");
        }
        return config;
    }

    private SourceScan findScan(Long id) {
        return scanRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("SourceScan", id));
    }

    private String mask(String token) {
        if (token.length() <= 8) return "****";
        return token.substring(0, 4) + "****" + token.substring(token.length() - 4);
    }

    private String truncate(String v, int max) {
        String s = blankToNull(v);
        return (s != null && s.length() > max) ? s.substring(0, max) : s;
    }

    private String blankToNull(String v) {
        return (v == null || v.isBlank()) ? null : v.trim();
    }
}
