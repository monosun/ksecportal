package com.monosun.secportal.privacy.entity;

import com.monosun.secportal.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

/** 개인정보 파기관리 — 파기계획·승인·이력·증적 */
@Entity
@Table(name = "privacy_disposals",
        indexes = {
                @Index(name = "idx_pdisp_status", columnList = "status"),
                @Index(name = "idx_pdisp_planned", columnList = "planned_date")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PrivacyDisposal extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 파기계획명 */
    @Column(nullable = false, length = 200)
    private String title;

    /** 파기대상 */
    @Column(name = "target_name", nullable = false, length = 200)
    private String targetName;

    /** 연계된 보유기간 항목 */
    @Column(name = "retention_id")
    private Long retentionId;

    /** 담당부서 */
    @Column(length = 100)
    private String department;

    /** 파기 개인정보 항목 */
    @Column(name = "info_items", columnDefinition = "TEXT")
    private String infoItems;

    /** 파기 건수 */
    @Column(name = "record_count")
    private Integer recordCount;

    /** 파기방법 (전자적 파일 영구삭제 / 파쇄 / 소각 등) */
    @Column(length = 200)
    private String method;

    /** 파기예정일 */
    @Column(name = "planned_date")
    private LocalDate plannedDate;

    /** 실제 파기일 */
    @Column(name = "disposal_date")
    private LocalDate disposalDate;

    /** 파기 수행자 */
    @Column(length = 100)
    private String performer;

    /** 파기 승인자 */
    @Column(length = 100)
    private String approver;

    /** 승인 상태 */
    @Enumerated(EnumType.STRING)
    @Column(name = "approval_status", nullable = false, length = 20)
    @Builder.Default
    private ApprovalStatus approvalStatus = ApprovalStatus.PENDING;

    /** 승인일 */
    @Column(name = "approved_date")
    private LocalDate approvedDate;

    /** 파기증적 (증적 자료 설명·경로) */
    @Column(columnDefinition = "TEXT")
    private String evidence;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private Status status = Status.PLANNED;

    @Column(columnDefinition = "TEXT")
    private String notes;

    public enum ApprovalStatus { PENDING, APPROVED, REJECTED }

    /** PLANNED: 계획, IN_PROGRESS: 진행중, COMPLETED: 파기완료 */
    public enum Status { PLANNED, IN_PROGRESS, COMPLETED }
}
