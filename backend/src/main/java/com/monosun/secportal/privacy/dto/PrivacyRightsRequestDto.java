package com.monosun.secportal.privacy.dto;

import com.monosun.secportal.privacy.entity.PrivacyRightsRequest;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class PrivacyRightsRequestDto {

    @Getter
    @Setter
    public static class Request {
        private String requestType;
        @NotBlank @Size(max = 100)
        private String subjectName;
        @Size(max = 100)
        private String contact;
        @Size(max = 100)
        private String channel;
        private String content;
        @NotNull
        private LocalDate requestDate;
        private LocalDate dueDate;
        private LocalDate completedDate;
        @Size(max = 100)
        private String handler;
        private String result;
        private String rejectReason;
        private String status;
        private String notes;
    }

    @Getter
    @Builder
    public static class Response {
        private Long id;
        private String requestType;
        private String subjectName;
        private String contact;
        private String channel;
        private String content;
        private LocalDate requestDate;
        private LocalDate dueDate;
        private LocalDate completedDate;
        private String handler;
        private String result;
        private String rejectReason;
        private String status;
        private String notes;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
        /** 처리기한까지 남은 일수 — 음수면 기한 경과 */
        private Long daysUntilDue;
        /** SLA 위반 여부 — 미완료 상태에서 기한이 지났거나, 완료일이 기한을 넘긴 경우 */
        private Boolean slaBreached;

        public static Response from(PrivacyRightsRequest r) {
            Long days = null;
            boolean breached = false;
            if (r.getDueDate() != null) {
                boolean closed = r.getStatus() == PrivacyRightsRequest.Status.COMPLETED
                        || r.getStatus() == PrivacyRightsRequest.Status.REJECTED;
                if (closed) {
                    // 종결된 건은 완료일 기준으로 기한 준수 여부를 판정한다
                    breached = r.getCompletedDate() != null && r.getCompletedDate().isAfter(r.getDueDate());
                } else {
                    days = java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), r.getDueDate());
                    breached = days < 0;
                }
            }
            return Response.builder()
                    .id(r.getId())
                    .requestType(r.getRequestType().name())
                    .subjectName(r.getSubjectName())
                    .contact(r.getContact())
                    .channel(r.getChannel())
                    .content(r.getContent())
                    .requestDate(r.getRequestDate())
                    .dueDate(r.getDueDate())
                    .completedDate(r.getCompletedDate())
                    .handler(r.getHandler())
                    .result(r.getResult())
                    .rejectReason(r.getRejectReason())
                    .status(r.getStatus().name())
                    .notes(r.getNotes())
                    .createdAt(r.getCreatedAt())
                    .updatedAt(r.getUpdatedAt())
                    .daysUntilDue(days)
                    .slaBreached(breached)
                    .build();
        }
    }
}
