package com.monosun.secportal.security.dto;

import com.monosun.secportal.security.entity.SecurityEvent;
import com.monosun.secportal.security.entity.SecurityIntegration;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

public class SecurityDto {

    @Getter
    public static class IntegrationCreateRequest {
        @NotBlank private String name;
        @NotBlank private String solutionType;
        private String vendor;
        private String host;
        private String apiKey;
        private String description;
    }

    @Getter
    public static class IntegrationUpdateRequest {
        private String name;
        private String solutionType;
        private String vendor;
        private String host;
        private String apiKey;
        private String description;
        private SecurityIntegration.ConnectionStatus status;
    }

    @Getter
    @Builder
    public static class IntegrationResponse {
        private Long id;
        private String name;
        private String solutionType;
        private String vendor;
        private String host;
        private String description;
        private SecurityIntegration.ConnectionStatus status;
        private LocalDateTime lastSyncAt;
        private LocalDateTime createdAt;
        private long eventCount;

        public static IntegrationResponse from(SecurityIntegration i, long eventCount) {
            return IntegrationResponse.builder()
                    .id(i.getId())
                    .name(i.getName())
                    .solutionType(i.getSolutionType())
                    .vendor(i.getVendor())
                    .host(i.getHost())
                    .description(i.getDescription())
                    .status(i.getStatus())
                    .lastSyncAt(i.getLastSyncAt())
                    .createdAt(i.getCreatedAt())
                    .eventCount(eventCount)
                    .build();
        }
    }

    @Getter
    public static class EventCreateRequest {
        @NotNull  private SecurityEvent.EventSeverity severity;
        @NotBlank private String eventType;
        private String sourceIp;
        private String destinationIp;
        @NotBlank private String message;
        private LocalDateTime detectedAt;
    }

    @Getter
    @Builder
    public static class EventResponse {
        private Long id;
        private Long integrationId;
        private String integrationName;
        private SecurityEvent.EventSeverity severity;
        private String eventType;
        private String sourceIp;
        private String destinationIp;
        private String message;
        private LocalDateTime detectedAt;
        private LocalDateTime createdAt;

        public static EventResponse from(SecurityEvent e) {
            return EventResponse.builder()
                    .id(e.getId())
                    .integrationId(e.getIntegration().getId())
                    .integrationName(e.getIntegration().getName())
                    .severity(e.getSeverity())
                    .eventType(e.getEventType())
                    .sourceIp(e.getSourceIp())
                    .destinationIp(e.getDestinationIp())
                    .message(e.getMessage())
                    .detectedAt(e.getDetectedAt())
                    .createdAt(e.getCreatedAt())
                    .build();
        }
    }
}
