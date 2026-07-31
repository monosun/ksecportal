package com.monosun.secportal.threat.dto;

import com.monosun.secportal.threat.entity.Threat;
import com.monosun.secportal.threat.entity.ThreatDefault;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class ThreatDto {

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ThreatRequest {
        @NotBlank
        private String name;

        @NotBlank
        private String type;

        private String category;
        private String assetDetail;
        /** 대상 자산유형(복수) — 코드관리 ASSET_TYPE 값 목록 */
        private List<String> assetTypes;
        private String description;
        private int likelihood = 3;
        private int impact = 3;
        private String remark;
    }

    /** 저장 형식("서버,네트워크") → 목록 */
    public static List<String> splitAssetTypes(String stored) {
        if (stored == null || stored.isBlank()) return List.of();
        return Arrays.stream(stored.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }

    /** 목록 → 저장 형식. 비어 있으면 null 로 저장한다. */
    public static String joinAssetTypes(List<String> values) {
        if (values == null || values.isEmpty()) return null;
        String joined = values.stream()
                .filter(v -> v != null && !v.isBlank())
                .map(String::trim)
                .distinct()
                .collect(Collectors.joining(","));
        return joined.isEmpty() ? null : joined;
    }

    @Getter
    @Builder
    public static class ThreatResponse {
        private Long id;
        private String name;
        private String type;
        private String category;
        private String assetDetail;
        private List<String> assetTypes;
        private String description;
        private int likelihood;
        private int impact;
        private int riskScore;
        private String remark;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        public static ThreatResponse from(Threat t) {
            return ThreatResponse.builder()
                    .id(t.getId())
                    .name(t.getName())
                    .type(t.getType())
                    .category(t.getCategory())
                    .assetDetail(t.getAssetDetail())
                    .assetTypes(splitAssetTypes(t.getAssetTypes()))
                    .description(t.getDescription())
                    .likelihood(t.getLikelihood())
                    .impact(t.getImpact())
                    .riskScore(t.getLikelihood() * t.getImpact())
                    .remark(t.getRemark())
                    .createdAt(t.getCreatedAt())
                    .updatedAt(t.getUpdatedAt())
                    .build();
        }
    }

    @Getter
    @Builder
    public static class DefaultCheckResponse {
        private boolean alreadyLoaded;
        private int existingCount;
        private int defaultCount;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DefaultRequest {
        private String riskId;
        @NotBlank private String name;
        private String type;
        private String category;
        private String assetDetail;
        private List<String> assetTypes;
        private String description;
        private int likelihood = 3;
        private int impact = 3;
    }

    @Getter
    @Builder
    public static class DefaultResponse {
        private Long id;
        private String riskId;
        private String name;
        private String type;
        private String category;
        private String assetDetail;
        private List<String> assetTypes;
        private String description;
        private int likelihood;
        private int impact;

        public static DefaultResponse from(ThreatDefault d) {
            return DefaultResponse.builder()
                    .id(d.getId())
                    .riskId(d.getRiskId())
                    .name(d.getName())
                    .type(d.getType())
                    .category(d.getCategory())
                    .assetDetail(d.getAssetDetail())
                    .assetTypes(splitAssetTypes(d.getAssetTypes()))
                    .description(d.getDescription())
                    .likelihood(d.getLikelihood())
                    .impact(d.getImpact())
                    .build();
        }
    }

    @Getter
    @Builder
    public static class DefaultListResponse {
        private long total;
        private int page;
        private int size;
        private List<DefaultResponse> items;
    }
}
