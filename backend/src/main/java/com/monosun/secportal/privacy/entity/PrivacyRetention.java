package com.monosun.secportal.privacy.entity;

import com.monosun.secportal.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

/** 개인정보 보유기간 관리 — 만료·삭제 예정 추적 및 파기 연계 */
@Entity
@Table(name = "privacy_retentions",
        indexes = {
                @Index(name = "idx_pret_status", columnList = "status"),
                @Index(name = "idx_pret_expiry", columnList = "expiry_date")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PrivacyRetention extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 보유기간 관리 대상 (개인정보파일 / 처리업무 등) */
    @Column(name = "target_name", nullable = false, length = 200)
    private String targetName;

    /** 연계된 개인정보파일 */
    @Column(name = "privacy_file_id")
    private Long privacyFileId;

    /** 담당부서 */
    @Column(length = 100)
    private String department;

    /** 개인정보 항목 */
    @Column(name = "info_items", columnDefinition = "TEXT")
    private String infoItems;

    /** 보유기간 (예: 수집일로부터 3년) */
    @Column(name = "retention_period", length = 200)
    private String retentionPeriod;

    /** 보유근거 */
    @Column(name = "legal_basis", length = 500)
    private String legalBasis;

    /** 보유 시작일 */
    @Column(name = "start_date")
    private LocalDate startDate;

    /** 만료예정일 */
    @Column(name = "expiry_date")
    private LocalDate expiryDate;

    /** 삭제(파기)예정일 */
    @Column(name = "disposal_due_date")
    private LocalDate disposalDueDate;

    /** 보유기간 연장사유 */
    @Column(name = "extension_reason", columnDefinition = "TEXT")
    private String extensionReason;

    /** 자동알림 사용 여부 */
    @Column(name = "notify_enabled")
    @Builder.Default
    private Boolean notifyEnabled = true;

    /** 만료 며칠 전 알림 */
    @Column(name = "notify_days_before")
    @Builder.Default
    private Integer notifyDaysBefore = 30;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private Status status = Status.ACTIVE;

    @Column(columnDefinition = "TEXT")
    private String notes;

    /** ACTIVE: 보유중, EXTENDED: 연장, DISPOSED: 파기완료 */
    public enum Status { ACTIVE, EXTENDED, DISPOSED }
}
