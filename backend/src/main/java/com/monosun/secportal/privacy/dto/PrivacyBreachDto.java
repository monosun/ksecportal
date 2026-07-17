package com.monosun.secportal.privacy.dto;

import com.monosun.secportal.privacy.entity.PrivacyBreach;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class PrivacyBreachDto {

    @Getter
    @Setter
    public static class Request {
        @NotBlank @Size(max = 200)
        private String title;
        private LocalDateTime occurredAt;
        private LocalDateTime detectedAt;
        private LocalDateTime reportDueAt;
        private Integer affectedCount;
        private String infoItems;
        private String cause;
        private Boolean subjectNotified;
        private LocalDate notifiedDate;
        @Size(max = 200)
        private String notifyMethod;
        private Boolean authorityReported;
        @Size(max = 100)
        private String authorityName;
        private LocalDate reportedDate;
        private String investigation;
        private String preventionPlan;
        private LocalDate closedDate;
        private String status;
        private String notes;
    }

    @Getter
    @Builder
    public static class Response {
        private Long id;
        private String title;
        private LocalDateTime occurredAt;
        private LocalDateTime detectedAt;
        private LocalDateTime reportDueAt;
        private Integer affectedCount;
        private String infoItems;
        private String cause;
        private Boolean subjectNotified;
        private LocalDate notifiedDate;
        private String notifyMethod;
        private Boolean authorityReported;
        private String authorityName;
        private LocalDate reportedDate;
        private String investigation;
        private String preventionPlan;
        private LocalDate closedDate;
        private String status;
        private String notes;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
        /** 신고기한까지 남은 시간(시간 단위) — 음수면 기한 경과 */
        private Long hoursUntilReportDue;
        /** 신고기한 경과 여부 (미신고 상태에서만 의미 있음) */
        private Boolean reportOverdue;

        public static Response from(PrivacyBreach b) {
            Long hours = null;
            Boolean overdue = null;
            if (b.getReportDueAt() != null) {
                hours = java.time.temporal.ChronoUnit.HOURS.between(LocalDateTime.now(), b.getReportDueAt());
                overdue = !Boolean.TRUE.equals(b.getAuthorityReported()) && hours < 0;
            }
            return Response.builder()
                    .id(b.getId())
                    .title(b.getTitle())
                    .occurredAt(b.getOccurredAt())
                    .detectedAt(b.getDetectedAt())
                    .reportDueAt(b.getReportDueAt())
                    .affectedCount(b.getAffectedCount())
                    .infoItems(b.getInfoItems())
                    .cause(b.getCause())
                    .subjectNotified(b.getSubjectNotified())
                    .notifiedDate(b.getNotifiedDate())
                    .notifyMethod(b.getNotifyMethod())
                    .authorityReported(b.getAuthorityReported())
                    .authorityName(b.getAuthorityName())
                    .reportedDate(b.getReportedDate())
                    .investigation(b.getInvestigation())
                    .preventionPlan(b.getPreventionPlan())
                    .closedDate(b.getClosedDate())
                    .status(b.getStatus().name())
                    .notes(b.getNotes())
                    .createdAt(b.getCreatedAt())
                    .updatedAt(b.getUpdatedAt())
                    .hoursUntilReportDue(hours)
                    .reportOverdue(overdue)
                    .build();
        }
    }
}
