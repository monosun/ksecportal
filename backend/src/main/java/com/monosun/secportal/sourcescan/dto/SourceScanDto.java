package com.monosun.secportal.sourcescan.dto;

import com.monosun.secportal.sourcescan.entity.SourceScan;
import com.monosun.secportal.sourcescan.entity.SourceScanFinding;
import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

public class SourceScanDto {

    // ── GitHub 연동 설정 ─────────────────────────────────────────────────────

    @Getter
    public static class ConfigRequest {
        private String token;       // null/빈 값이면 기존 토큰 유지, "-"는 삭제
        private String apiBaseUrl;
        private String owner;
    }

    @Getter
    @Builder
    public static class ConfigResponse {
        private boolean tokenStored;
        private String tokenMasked;
        private String apiBaseUrl;
        private String owner;
        private LocalDateTime updatedAt;
    }

    @Getter
    @Builder
    public static class TestResult {
        private boolean success;
        private String message;   // 성공 시 로그인 계정, 실패 시 사유
    }

    // ── 저장소 ──────────────────────────────────────────────────────────────

    @Getter
    @Builder
    public static class RepoResponse {
        private String fullName;
        private boolean privateRepo;
        private String defaultBranch;
        private String description;
        private String updatedAt;
    }

    // ── 점검 ────────────────────────────────────────────────────────────────

    @Getter
    public static class ScanRequest {
        @NotBlank private String repository;  // owner/repo
    }

    @Getter
    @Builder
    public static class ScanResponse {
        private Long id;
        private String repository;
        private String status;
        private String message;
        private int criticalCount;
        private int highCount;
        private int mediumCount;
        private int lowCount;
        private int dependencyCount;
        private int codeCount;
        private int secretCount;
        private int sastCount;
        private int totalCount;
        private LocalDateTime createdAt;

        public static ScanResponse from(SourceScan s) {
            return ScanResponse.builder()
                    .id(s.getId())
                    .repository(s.getRepository())
                    .status(s.getStatus().name())
                    .message(s.getMessage())
                    .criticalCount(s.getCriticalCount())
                    .highCount(s.getHighCount())
                    .mediumCount(s.getMediumCount())
                    .lowCount(s.getLowCount())
                    .dependencyCount(s.getDependencyCount())
                    .codeCount(s.getCodeCount())
                    .secretCount(s.getSecretCount())
                    .sastCount(s.getSastCount())
                    .totalCount(s.getDependencyCount() + s.getCodeCount() + s.getSecretCount() + s.getSastCount())
                    .createdAt(s.getCreatedAt())
                    .build();
        }
    }

    @Getter
    @Builder
    public static class FindingResponse {
        private Long id;
        private String category;
        private String severity;
        private String title;
        private String identifier;
        private String location;
        private String cveId;
        private String htmlUrl;

        public static FindingResponse from(SourceScanFinding f) {
            return FindingResponse.builder()
                    .id(f.getId())
                    .category(f.getCategory().name())
                    .severity(f.getSeverity().name())
                    .title(f.getTitle())
                    .identifier(f.getIdentifier())
                    .location(f.getLocation())
                    .cveId(f.getCveId())
                    .htmlUrl(f.getHtmlUrl())
                    .build();
        }
    }

    @Getter
    @Builder
    public static class ScanDetailResponse {
        private ScanResponse scan;
        private List<FindingResponse> findings;

        public static ScanDetailResponse from(SourceScan s) {
            return ScanDetailResponse.builder()
                    .scan(ScanResponse.from(s))
                    .findings(s.getFindings().stream().map(FindingResponse::from).toList())
                    .build();
        }
    }
}
