package com.monosun.secportal.privacy.entity;

import com.monosun.secportal.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

/** 개인정보 수집·이용 관리 — 수집동의서 및 버전 관리 */
@Entity
@Table(name = "privacy_consents",
        indexes = {
                @Index(name = "idx_pconsent_status", columnList = "status"),
                @Index(name = "idx_pconsent_title", columnList = "title")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PrivacyConsent extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 동의서명 */
    @Column(nullable = false, length = 200)
    private String title;

    /** 동의서 버전 */
    @Column(length = 50)
    @Builder.Default
    private String version = "1.0";

    /** 필수/선택 구분 */
    @Enumerated(EnumType.STRING)
    @Column(name = "consent_type", nullable = false, length = 20)
    @Builder.Default
    private ConsentType consentType = ConsentType.REQUIRED;

    /** 수집 개인정보 항목 */
    @Column(name = "info_items", columnDefinition = "TEXT")
    private String infoItems;

    /** 이용목적 */
    @Column(columnDefinition = "TEXT")
    private String purpose;

    /** 처리근거 */
    @Column(name = "legal_basis", length = 500)
    private String legalBasis;

    /** 보유·이용기간 */
    @Column(name = "retention_period", length = 200)
    private String retentionPeriod;

    /** 수집 채널/화면 */
    @Column(length = 200)
    private String channel;

    /** 시행일 */
    @Column(name = "effective_date")
    private LocalDate effectiveDate;

    /** 동의서 본문 */
    @Column(columnDefinition = "TEXT")
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private Status status = Status.DRAFT;

    @Column(columnDefinition = "TEXT")
    private String notes;

    public enum ConsentType { REQUIRED, OPTIONAL }

    public enum Status { DRAFT, ACTIVE, ARCHIVED }
}
