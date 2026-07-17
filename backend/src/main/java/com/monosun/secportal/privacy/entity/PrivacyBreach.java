package com.monosun.secportal.privacy.entity;

import com.monosun.secportal.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** 개인정보 유출관리 — 유출사고 등록·신고기한·통지·재발방지 (보안 인시던트와 별도 운영) */
@Entity
@Table(name = "privacy_breaches",
        indexes = {
                @Index(name = "idx_pbreach_status", columnList = "status"),
                @Index(name = "idx_pbreach_detected", columnList = "detected_at")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PrivacyBreach extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 사고명 */
    @Column(nullable = false, length = 200)
    private String title;

    /** 유출 발생일시 */
    @Column(name = "occurred_at")
    private LocalDateTime occurredAt;

    /** 인지일시 — 신고기한 기산점 */
    @Column(name = "detected_at")
    private LocalDateTime detectedAt;

    /** 신고기한 (인지 후 72시간) */
    @Column(name = "report_due_at")
    private LocalDateTime reportDueAt;

    /** 유출 정보주체 수 */
    @Column(name = "affected_count")
    private Integer affectedCount;

    /** 유출 개인정보 항목 */
    @Column(name = "info_items", columnDefinition = "TEXT")
    private String infoItems;

    /** 유출 경로·원인 */
    @Column(columnDefinition = "TEXT")
    private String cause;

    /** 정보주체 통지 여부 */
    @Column(name = "subject_notified")
    @Builder.Default
    private Boolean subjectNotified = false;

    /** 정보주체 통지일 */
    @Column(name = "notified_date")
    private LocalDate notifiedDate;

    /** 통지 방법 */
    @Column(name = "notify_method", length = 200)
    private String notifyMethod;

    /** 관계기관 신고 여부 */
    @Column(name = "authority_reported")
    @Builder.Default
    private Boolean authorityReported = false;

    /** 신고 관계기관 (개인정보보호위원회 / KISA 등) */
    @Column(name = "authority_name", length = 100)
    private String authorityName;

    /** 관계기관 신고일 */
    @Column(name = "reported_date")
    private LocalDate reportedDate;

    /** 조사결과 */
    @Column(columnDefinition = "TEXT")
    private String investigation;

    /** 재발방지 대책 */
    @Column(name = "prevention_plan", columnDefinition = "TEXT")
    private String preventionPlan;

    /** 종결일 */
    @Column(name = "closed_date")
    private LocalDate closedDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private Status status = Status.DETECTED;

    @Column(columnDefinition = "TEXT")
    private String notes;

    /** DETECTED: 인지, INVESTIGATING: 조사중, NOTIFIED: 통지·신고완료, CLOSED: 종결 */
    public enum Status { DETECTED, INVESTIGATING, NOTIFIED, CLOSED }
}
