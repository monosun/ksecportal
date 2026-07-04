package com.monosun.secportal.metrics.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

public class MetricsDto {

    @Getter
    @Builder
    public static class Summary {
        private long totalAssets;
        private long highCriticalityAssets;
        private long overdueVulns;
        private long openVulns;
        private long openIncidents;
        private long criticalIncidents;
        private double policyAckRate;
        private double trainingCompletionRate;
        private List<TrendPoint> vulnTrend;
        // 보안이벤트 현황
        private long totalIntegrations;
        private long connectedIntegrations;
        private long events24h;
        private long criticalEvents24h;
        private long highEvents24h;
        private List<SecurityEventSummary> recentSecurityEvents;
    }

    @Getter
    @Builder
    public static class TrendPoint {
        private String month;
        private long count;
    }

    @Getter
    @Builder
    public static class SecurityEventSummary {
        private Long id;
        private String integrationName;
        private String severity;
        private String eventType;
        private String message;
        private LocalDateTime detectedAt;
    }
}
