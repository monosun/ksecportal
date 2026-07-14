package com.monosun.secportal.phishing.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "phishing_campaign_targets")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class PhishingCampaignTarget {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "campaign_id", nullable = false)
    private PhishingCampaign campaign;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "target_id", nullable = false)
    private PhishingTarget target;

    @Column(length = 64, unique = true)
    private String trackingToken;

    private LocalDateTime sentAt;
    private LocalDateTime openedAt;
    private LocalDateTime clickedAt;
    private LocalDateTime reportedAt;

    /** 발송 처리 결과: SUCCESS / FAILED (미발송 시 null) */
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private SendStatus sendStatus;

    /** 발송 실패 사유 (성공 시 null) */
    @Column(columnDefinition = "TEXT")
    private String sendError;

    public enum SendStatus { SUCCESS, FAILED }
}
