package com.monosun.secportal.privacy.entity;

import com.monosun.secportal.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

/** 개인정보 영향평가(DPIA) — 공공기관 PIA 및 민간 자체 영향평가 */
@Entity
@Table(name = "privacy_dpia",
        indexes = {
                @Index(name = "idx_pdpia_status", columnList = "status"),
                @Index(name = "idx_pdpia_risk", columnList = "risk_level")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PrivacyDpia extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 영향평가명 */
    @Column(nullable = false, length = 200)
    private String title;

    /** 영향평가 대상 (시스템·서비스) */
    @Column(name = "target_system", length = 200)
    private String targetSystem;

    /** 담당부서 */
    @Column(length = 100)
    private String department;

    /** 대상 개인정보 항목 */
    @Column(name = "info_items", columnDefinition = "TEXT")
    private String infoItems;

    /** 대상 정보주체 수 */
    @Column(name = "subject_count")
    private Integer subjectCount;

    /** 평가일 */
    @Column(name = "assessment_date")
    private LocalDate assessmentDate;

    /** 평가자(수행기관) */
    @Column(length = 100)
    private String assessor;

    /** 체크리스트 결과 */
    @Column(columnDefinition = "TEXT")
    private String checklist;

    /** 종합 위험도 */
    @Enumerated(EnumType.STRING)
    @Column(name = "risk_level", length = 20)
    @Builder.Default
    private RiskLevel riskLevel = RiskLevel.MEDIUM;

    /** 개선계획 */
    @Column(name = "improvement_plan", columnDefinition = "TEXT")
    private String improvementPlan;

    /** 개선 완료 예정일 */
    @Column(name = "improvement_due_date")
    private LocalDate improvementDueDate;

    /** 완료보고 */
    @Column(name = "completion_report", columnDefinition = "TEXT")
    private String completionReport;

    /** 완료일 */
    @Column(name = "completed_date")
    private LocalDate completedDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private Status status = Status.PLANNED;

    @Column(columnDefinition = "TEXT")
    private String notes;

    public enum RiskLevel { HIGH, MEDIUM, LOW }

    /** PLANNED: 대상선정, IN_PROGRESS: 평가중, IMPROVING: 개선중, COMPLETED: 완료 */
    public enum Status { PLANNED, IN_PROGRESS, IMPROVING, COMPLETED }
}
