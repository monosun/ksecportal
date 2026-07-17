package com.monosun.secportal.privacy.entity;

import com.monosun.secportal.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

/** 개인정보 처리현황 — 개인정보 처리업무 단위 등록 */
@Entity
@Table(name = "privacy_processing_activities",
        indexes = {
                @Index(name = "idx_pproc_status", columnList = "status"),
                @Index(name = "idx_pproc_name", columnList = "name")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PrivacyProcessing extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 처리업무명 (예: 셀프개통, 고객센터, 배송, 정산, 마케팅) */
    @Column(nullable = false, length = 200)
    private String name;

    /** 처리부서 */
    @Column(length = 100)
    private String department;

    /** 처리목적 */
    @Column(columnDefinition = "TEXT")
    private String purpose;

    /** 개인정보 항목 */
    @Column(name = "info_items", columnDefinition = "TEXT")
    private String infoItems;

    /** 보유기간 */
    @Column(name = "retention_period", length = 200)
    private String retentionPeriod;

    /** 처리근거 (동의/계약이행/법령상 의무 등) */
    @Column(name = "legal_basis", length = 500)
    private String legalBasis;

    /** 처리시스템 */
    @Column(name = "system_name", length = 200)
    private String systemName;

    /** 업무흐름도 */
    @Column(columnDefinition = "TEXT")
    private String workflow;

    /** 개인정보 라이프사이클 — 수집,보유,이용,제공,파기 중 해당 단계 (쉼표 구분) */
    @Column(length = 200)
    private String lifecycle;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private Status status = Status.ACTIVE;

    @Column(columnDefinition = "TEXT")
    private String notes;

    public enum Status { ACTIVE, INACTIVE }
}
