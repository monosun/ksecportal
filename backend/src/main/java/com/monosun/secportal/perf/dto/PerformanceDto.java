package com.monosun.secportal.perf.dto;

import com.monosun.secportal.perf.entity.SlowLog;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

public class PerformanceDto {

    @Getter
    @Builder
    public static class LogResponse {
        private Long id;
        private String logType;
        private String target;
        private String detail;
        private long durationMs;
        private int thresholdMs;
        private String username;
        private String ipAddress;
        private String httpMethod;
        private Integer statusCode;
        private LocalDateTime occurredAt;

        public static LogResponse from(SlowLog s) {
            return LogResponse.builder()
                    .id(s.getId())
                    .logType(s.getLogType().name())
                    .target(s.getTarget())
                    .detail(s.getDetail())
                    .durationMs(s.getDurationMs())
                    .thresholdMs(s.getThresholdMs())
                    .username(s.getUsername())
                    .ipAddress(s.getIpAddress())
                    .httpMethod(s.getHttpMethod())
                    .statusCode(s.getStatusCode())
                    .occurredAt(s.getOccurredAt())
                    .build();
        }
    }

    /** 성능관리 기본값 — 임계시간(ms)·수집 사용 여부 */
    @Getter
    @Setter
    @Builder
    public static class Config {
        private int thresholdMs;
        private boolean enabled;
        private boolean sqlEnabled;
        private int retentionDays;
    }

    @Getter
    @Setter
    public static class ConfigRequest {
        private Integer thresholdMs;
        private Boolean enabled;
        private Boolean sqlEnabled;
        private Integer retentionDays;
    }

    @Getter
    @Builder
    public static class Stats {
        private long total;
        private long screenCount;
        private long sqlCount;
        private long maxDurationMs;
    }
}
