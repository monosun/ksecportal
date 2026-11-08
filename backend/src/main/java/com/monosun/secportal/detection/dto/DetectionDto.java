package com.monosun.secportal.detection.dto;

import com.monosun.secportal.detection.entity.BlockedIp;
import com.monosun.secportal.detection.entity.DetectionEvent;
import com.monosun.secportal.detection.service.DetectionEventService;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public class DetectionDto {

    @Getter
    @Builder
    public static class EventResponse {
        private Long id;
        private String category;
        private String categoryLabel;
        private String severity;
        private String status;
        private String title;
        private String detail;
        private String sourceIp;
        private String userEmail;
        private String userAgent;
        private String requestPath;
        private Integer score;
        private Integer occurrences;
        private Boolean autoBlocked;
        private Long incidentId;
        private String handledBy;
        private LocalDateTime handledAt;
        private String handlerNote;
        private LocalDateTime detectedAt;
        private LocalDateTime lastSeenAt;

        public static EventResponse from(DetectionEvent e) {
            return EventResponse.builder()
                    .id(e.getId())
                    .category(e.getCategory().name())
                    .categoryLabel(DetectionEventService.categoryLabel(e.getCategory()))
                    .severity(e.getSeverity().name())
                    .status(e.getStatus().name())
                    .title(e.getTitle())
                    .detail(e.getDetail())
                    .sourceIp(e.getSourceIp())
                    .userEmail(e.getUserEmail())
                    .userAgent(e.getUserAgent())
                    .requestPath(e.getRequestPath())
                    .score(e.getScore())
                    .occurrences(e.getOccurrences())
                    .autoBlocked(e.getAutoBlocked())
                    .incidentId(e.getIncidentId())
                    .handledBy(e.getHandledBy())
                    .handledAt(e.getHandledAt())
                    .handlerNote(e.getHandlerNote())
                    .detectedAt(e.getDetectedAt())
                    .lastSeenAt(e.getLastSeenAt())
                    .build();
        }
    }

    @Getter
    @Setter
    public static class StatusRequest {
        private DetectionEvent.Status status;
        private String note;
    }

    @Getter
    @Setter
    public static class BlockRequest {
        private String ip;
        /** 0 = 해제할 때까지 */
        private Integer minutes;
        private String reason;
    }

    @Getter
    @Builder
    public static class BlockResponse {
        private Long id;
        private String ip;
        private String reason;
        private LocalDateTime blockedUntil;
        private String createdBy;
        private Long eventId;
        private boolean active;
        private boolean effective;
        private LocalDateTime createdAt;
        private LocalDateTime releasedAt;
        private String releasedBy;

        public static BlockResponse from(BlockedIp b) {
            return BlockResponse.builder()
                    .id(b.getId())
                    .ip(b.getIp())
                    .reason(b.getReason())
                    .blockedUntil(b.getBlockedUntil())
                    .createdBy(b.getCreatedBy())
                    .eventId(b.getEventId())
                    .active(Boolean.TRUE.equals(b.getActive()))
                    .effective(b.isEffective(LocalDateTime.now()))
                    .createdAt(b.getCreatedAt())
                    .releasedAt(b.getReleasedAt())
                    .releasedBy(b.getReleasedBy())
                    .build();
        }
    }

    /** 실시간 의심 IP(행위 점수) */
    @Getter
    @Setter
    public static class IpScore {
        private String ip;
        private int score;
        private List<String> reasons;
        private int requests;
        private int distinctPaths;
        private int notFound;
        private int denied;
        private int rateLimited;
        private String userAgent;
        private String lastUser;
        private List<String> samplePaths;
        private boolean blocked;
        private boolean whitelisted;
    }

    @Getter
    @Builder
    public static class LiveResponse {
        private LocalDateTime analyzedAt;
        private int windowMinutes;
        private int scoreThreshold;
        private int trackedIps;
        private List<IpScore> ips;
    }

    @Getter
    @Builder
    public static class Summary {
        private boolean enabled;
        private long last24h;
        private long openCount;
        private long activeBlocks;
        private Map<String, Long> bySeverity24h;
        private Map<String, Long> byCategory7d;
        private Map<String, Long> daily7d;
        private List<Map<String, Object>> topIps;
    }
}
