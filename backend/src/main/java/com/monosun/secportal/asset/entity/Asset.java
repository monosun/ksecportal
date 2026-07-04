package com.monosun.secportal.asset.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "assets")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Asset {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    private AssetCategory assetCategory;

    @Column(nullable = false, length = 100)
    private String type;

    @Column(columnDefinition = "TEXT")
    private String description;

    // ── 책임 ──────────────────────────────────────────────────────────────────

    private String owner;
    private String department;
    private String operator;

    // ── 현황 ──────────────────────────────────────────────────────────────────

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private Environment environment = Environment.PRODUCTION;

    private String location;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private Status status = Status.OPERATIONAL;

    // ── 클라우드 관련 (기존 필드 유지) ────────────────────────────────────────

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private CloudProvider cloudProvider = CloudProvider.ON_PREMISE;

    private String cloudResourceId;
    private String region;
    private String ipAddress;
    private String osType;

    @Column(columnDefinition = "TEXT")
    private String spec;

    // ── 평가 ──────────────────────────────────────────────────────────────────

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private Criticality criticality = Criticality.MEDIUM;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private Criticality confidentiality = Criticality.MEDIUM;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private Criticality integrity = Criticality.MEDIUM;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private Criticality availability = Criticality.MEDIUM;

    // ── 개인정보 ──────────────────────────────────────────────────────────────

    @Column(nullable = false)
    @Builder.Default
    private boolean personalInfoIncluded = false;

    private String personalInfoType;

    @Column(nullable = false)
    @Builder.Default
    private boolean personalInfoProcessing = false;

    // ── 연계 ──────────────────────────────────────────────────────────────────

    @Column(columnDefinition = "TEXT")
    private String linkedSystems;

    // ── 보안 관리 ─────────────────────────────────────────────────────────────

    @Column(nullable = false)
    @Builder.Default
    private boolean accessControlTarget = false;

    @Column(nullable = false)
    @Builder.Default
    private boolean backupTarget = false;

    @Column(nullable = false)
    @Builder.Default
    private boolean logManagementTarget = false;

    // ── 비용 / 계약 (기존 유지) ───────────────────────────────────────────────

    @Column(precision = 12, scale = 2)
    private BigDecimal monthlyCost;

    private LocalDate contractExpiry;

    // ── 점검 / 검토 ───────────────────────────────────────────────────────────

    private LocalDate lastInspectionDate;
    private LocalDate nextInspectionDate;
    private LocalDate lastReviewDate;

    // ── 비고 ──────────────────────────────────────────────────────────────────

    @Column(columnDefinition = "TEXT")
    private String remarks;

    // ── 활성 여부 (status 와 동기화 — 하위 호환용) ───────────────────────────

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;

    // ── 타임스탬프 ───────────────────────────────────────────────────────────

    @Column(nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(nullable = false)
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PreUpdate
    void onUpdate() {
        this.updatedAt = LocalDateTime.now();
        this.active = (this.status == Status.OPERATIONAL);
    }

    @PrePersist
    void onPersist() {
        if (this.status == null) this.status = Status.OPERATIONAL;
        this.active = (this.status == Status.OPERATIONAL);
    }

    // ── 열거형 ────────────────────────────────────────────────────────────────

    public enum AssetCategory {
        INFO, SW, HW, SERVICE, PERSONNEL, FACILITY
    }

    public enum CloudProvider {
        AWS, GCP, AZURE, ON_PREMISE, OTHER
    }

    public enum Environment {
        PRODUCTION, STAGING, DEVELOPMENT, TEST
    }

    public enum Status {
        OPERATIONAL, SUSPENDED, DISPOSED
    }

    public enum Criticality {
        HIGH, MEDIUM, LOW
    }
}
