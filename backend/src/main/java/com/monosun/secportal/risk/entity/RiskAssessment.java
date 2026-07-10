package com.monosun.secportal.risk.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "risk_assessments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RiskAssessment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long roundId;

    private Long assetId;
    private String assetName;
    @Column(length = 100)
    private String assetType;
    @Column(length = 50)
    private String assetEnvironment;
    private Long threatId;
    private String threatName;
    @Column(length = 100)
    private String threatType;
    private String vulnerability;

    @Builder.Default
    private int likelihood = 3;

    @Builder.Default
    private int impact = 3;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private Grade riskGrade = Grade.MEDIUM;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private Treatment treatment = Treatment.감소;

    @Column(columnDefinition = "TEXT")
    private String notes;

    // ── 위험 처리 계획 (완료 차수의 '감소' 항목 대상 — 연도/차수별 스냅샷) ──
    @Column(columnDefinition = "TEXT")
    private String plan;            // 조치 계획
    private String planAssignee;    // 담당자
    private LocalDate planDueDate;  // 완료 기한
    private Integer planProgress;   // 진행률 0~100
    private String planStatus;      // 계획중/진행중/완료/기한초과

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PreUpdate
    void onUpdate() { this.updatedAt = LocalDateTime.now(); }

    public enum Grade { HIGH, MEDIUM, LOW }

    public enum Treatment { 수용, 감소, 회피, 이전 }
}
