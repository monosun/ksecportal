package com.monosun.secportal.phishing.dto;

import com.monosun.secportal.phishing.entity.PhishingCampaign;
import com.monosun.secportal.phishing.entity.PhishingCampaignTarget;
import com.monosun.secportal.phishing.entity.PhishingTarget;
import com.monosun.secportal.phishing.entity.PhishingTemplate;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

public class PhishingDto {

    // ── Template ───────────────────────────────────────────────────────────

    @Getter
    public static class TemplateRequest {
        @NotBlank private String name;
        @NotBlank private String category;
        private String difficulty;
        @NotBlank private String subject;
        @NotBlank private String senderName;
        @NotBlank @Email private String senderEmail;
        @NotBlank private String bodyHtml;
        private String description;
    }

    @Getter @Builder
    public static class TemplateResponse {
        private Long id;
        private String name;
        private String category;
        private String difficulty;
        private String subject;
        private String senderName;
        private String senderEmail;
        private String bodyHtml;
        private String description;
        private String createdBy;
        private LocalDateTime createdAt;

        public static TemplateResponse from(PhishingTemplate t) {
            return TemplateResponse.builder()
                    .id(t.getId())
                    .name(t.getName())
                    .category(t.getCategory())
                    .difficulty(t.getDifficulty().name())
                    .subject(t.getSubject())
                    .senderName(t.getSenderName())
                    .senderEmail(t.getSenderEmail())
                    .bodyHtml(t.getBodyHtml())
                    .description(t.getDescription())
                    .createdBy(t.getCreatedBy() != null ? t.getCreatedBy().getName() : null)
                    .createdAt(t.getCreatedAt())
                    .build();
        }
    }

    // ── Target ─────────────────────────────────────────────────────────────

    @Getter
    public static class TargetRequest {
        @NotBlank private String name;
        @NotBlank @Email private String email;
        private String department;
        private String position;
    }

    @Getter @Builder
    public static class TargetResponse {
        private Long id;
        private String name;
        private String email;
        private String department;
        private String position;
        private boolean active;
        private LocalDateTime createdAt;

        public static TargetResponse from(PhishingTarget t) {
            return TargetResponse.builder()
                    .id(t.getId())
                    .name(t.getName())
                    .email(t.getEmail())
                    .department(t.getDepartment())
                    .position(t.getPosition())
                    .active(t.isActive())
                    .createdAt(t.getCreatedAt())
                    .build();
        }
    }

    // ── Campaign ───────────────────────────────────────────────────────────

    @Getter
    public static class CampaignRequest {
        @NotBlank private String name;
        @NotNull private Long templateId;
        private String description;
        private LocalDateTime scheduledAt;
        @NotNull private List<Long> targetIds;
    }

    @Getter @Builder
    public static class CampaignResponse {
        private Long id;
        private String name;
        private Long templateId;
        private String templateName;
        private String status;
        private String description;
        private LocalDateTime scheduledAt;
        private int totalTargets;
        private long sentCount;
        private long openedCount;
        private long clickedCount;
        private long reportedCount;
        private String createdBy;
        private LocalDateTime createdAt;

        public static CampaignResponse from(PhishingCampaign c) {
            List<PhishingCampaignTarget> targets = c.getCampaignTargets();
            return CampaignResponse.builder()
                    .id(c.getId())
                    .name(c.getName())
                    .templateId(c.getTemplate().getId())
                    .templateName(c.getTemplate().getName())
                    .status(c.getStatus().name())
                    .description(c.getDescription())
                    .scheduledAt(c.getScheduledAt())
                    .totalTargets(targets.size())
                    .sentCount(targets.stream().filter(t -> t.getSentAt() != null).count())
                    .openedCount(targets.stream().filter(t -> t.getOpenedAt() != null).count())
                    .clickedCount(targets.stream().filter(t -> t.getClickedAt() != null).count())
                    .reportedCount(targets.stream().filter(t -> t.getReportedAt() != null).count())
                    .createdBy(c.getCreatedBy() != null ? c.getCreatedBy().getName() : null)
                    .createdAt(c.getCreatedAt())
                    .build();
        }
    }

    @Getter @Builder
    public static class CampaignTargetResult {
        private Long id;
        private String targetName;
        private String targetEmail;
        private String department;
        private LocalDateTime sentAt;
        private LocalDateTime openedAt;
        private LocalDateTime clickedAt;
        private LocalDateTime reportedAt;
        private String sendStatus;
        private String sendError;

        public static CampaignTargetResult from(PhishingCampaignTarget ct) {
            return CampaignTargetResult.builder()
                    .id(ct.getId())
                    .targetName(ct.getTarget().getName())
                    .targetEmail(ct.getTarget().getEmail())
                    .department(ct.getTarget().getDepartment())
                    .sentAt(ct.getSentAt())
                    .openedAt(ct.getOpenedAt())
                    .clickedAt(ct.getClickedAt())
                    .reportedAt(ct.getReportedAt())
                    .sendStatus(ct.getSendStatus() != null ? ct.getSendStatus().name() : null)
                    .sendError(ct.getSendError())
                    .build();
        }
    }

    @Getter @Builder
    public static class CampaignDetail {
        private CampaignResponse campaign;
        private List<CampaignTargetResult> results;
    }

    // ── 발송 처리 결과 로그 ──────────────────────────────────────────────────

    @Getter @Builder
    public static class SendLogEntry {
        private Long id;
        private Long campaignId;
        private String campaignName;
        private String targetName;
        private String targetEmail;
        private String department;
        private String sendStatus;
        private String sendError;
        private LocalDateTime sentAt;
        private LocalDateTime openedAt;
        private LocalDateTime clickedAt;

        public static SendLogEntry from(PhishingCampaignTarget ct) {
            return SendLogEntry.builder()
                    .id(ct.getId())
                    .campaignId(ct.getCampaign().getId())
                    .campaignName(ct.getCampaign().getName())
                    .targetName(ct.getTarget().getName())
                    .targetEmail(ct.getTarget().getEmail())
                    .department(ct.getTarget().getDepartment())
                    .sendStatus(ct.getSendStatus() != null ? ct.getSendStatus().name() : null)
                    .sendError(ct.getSendError())
                    .sentAt(ct.getSentAt())
                    .openedAt(ct.getOpenedAt())
                    .clickedAt(ct.getClickedAt())
                    .build();
        }
    }
}
