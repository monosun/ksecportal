package com.monosun.secportal.incident.dto;

import com.monosun.secportal.incident.entity.Incident;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

public class IncidentDto {

    @Getter
    public static class CreateRequest {
        @NotBlank private String title;
        private String description;
        @NotNull private Incident.Severity severity;
        @NotNull private Incident.IncidentType type;
        private String affectedSystems;
        private Long assigneeId;
        private String assigneeText;
        private LocalDateTime detectedAt;
    }

    @Getter
    public static class UpdateRequest {
        private String title;
        private String description;
        private Incident.Severity severity;
        private Incident.Status status;
        private Incident.IncidentType type;
        private String affectedSystems;
        private Long assigneeId;
        private String assigneeText;
        private LocalDateTime detectedAt;
        private LocalDateTime resolvedAt;
    }

    @Getter
    @Builder
    public static class Response {
        private Long id;
        private String title;
        private String description;
        private String severity;
        private String status;
        private String type;
        private String affectedSystems;
        private String reporterName;
        private Long reporterId;
        private String assigneeName;
        private Long assigneeId;
        private LocalDateTime detectedAt;
        private LocalDateTime resolvedAt;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        public static Response from(Incident i) {
            return Response.builder()
                    .id(i.getId())
                    .title(i.getTitle())
                    .description(i.getDescription())
                    .severity(i.getSeverity().name())
                    .status(i.getStatus().name())
                    .type(i.getType().name())
                    .affectedSystems(i.getAffectedSystems())
                    .reporterName(i.getReporter().getName())
                    .reporterId(i.getReporter().getId())
                    .assigneeName(i.getAssignee() != null ? i.getAssignee().getName()
                            : i.getAssigneeText())
                    .assigneeId(i.getAssignee() != null ? i.getAssignee().getId() : null)
                    .detectedAt(i.getDetectedAt())
                    .resolvedAt(i.getResolvedAt())
                    .createdAt(i.getCreatedAt())
                    .updatedAt(i.getUpdatedAt())
                    .build();
        }
    }
}
