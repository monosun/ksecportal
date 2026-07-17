package com.monosun.secportal.privacy.entity;

import com.monosun.secportal.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

/** 개인정보 제공관리 — 제3자 제공 / 공동이용 / 국외이전 (위탁과는 별도) */
@Entity
@Table(name = "privacy_provisions",
        indexes = {
                @Index(name = "idx_pprov_status", columnList = "status"),
                @Index(name = "idx_pprov_type", columnList = "provision_type")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PrivacyProvision extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 제공 유형 */
    @Enumerated(EnumType.STRING)
    @Column(name = "provision_type", nullable = false, length = 20)
    @Builder.Default
    private ProvisionType provisionType = ProvisionType.THIRD_PARTY;

    /** 제공받는 자 */
    @Column(nullable = false, length = 200)
    private String recipient;

    /** 국외이전 시 이전 국가 */
    @Column(length = 100)
    private String country;

    /** 제공 개인정보 항목 */
    @Column(name = "info_items", columnDefinition = "TEXT")
    private String infoItems;

    /** 제공목적 */
    @Column(columnDefinition = "TEXT")
    private String purpose;

    /** 제공근거 (동의/법령 등) */
    @Column(name = "legal_basis", length = 500)
    private String legalBasis;

    /** 제공받는 자의 보유·이용기간 */
    @Column(name = "retention_period", length = 200)
    private String retentionPeriod;

    /** 제공 방법 */
    @Column(length = 200)
    private String method;

    /** 제공 계약 정보 */
    @Column(name = "contract_info", columnDefinition = "TEXT")
    private String contractInfo;

    /** 계약 기간 */
    @Column(name = "contract_start")
    private LocalDate contractStart;

    @Column(name = "contract_end")
    private LocalDate contractEnd;

    /** 최초 제공일 */
    @Column(name = "provision_date")
    private LocalDate provisionDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private Status status = Status.ACTIVE;

    @Column(columnDefinition = "TEXT")
    private String notes;

    /** THIRD_PARTY: 제3자 제공, JOINT_USE: 공동이용, OVERSEAS: 국외이전 */
    public enum ProvisionType { THIRD_PARTY, JOINT_USE, OVERSEAS }

    public enum Status { ACTIVE, TERMINATED }
}
