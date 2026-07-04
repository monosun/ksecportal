package com.monosun.secportal.secfinding.entity;

import com.monosun.secportal.auth.entity.User;
import com.monosun.secportal.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "security_findings",
        indexes = {
                @Index(name = "idx_sec_findings_year", columnList = "year"),
                @Index(name = "idx_sec_findings_status", columnList = "status")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SecurityFinding extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private int year;

    @Enumerated(EnumType.STRING)
    @Column(name = "audit_type", nullable = false, length = 20)
    @Builder.Default
    private AuditType auditType = AuditType.ISMS_P;

    @Column(length = 200)
    private String domain;

    @Column(name = "requirement_code", length = 50)
    private String requirementCode;

    @Column(name = "requirement_name", length = 300)
    private String requirementName;

    @Column(name = "finding_summary", nullable = false, length = 500)
    private String findingSummary;

    @Column(name = "finding_detail", columnDefinition = "TEXT")
    private String findingDetail;

    @Enumerated(EnumType.STRING)
    @Column(name = "risk_level", nullable = false, length = 20)
    @Builder.Default
    private RiskLevel riskLevel = RiskLevel.MEDIUM;

    @Column(name = "corrective_action", columnDefinition = "TEXT")
    private String correctiveAction;

    @Column(name = "action_deadline")
    private LocalDate actionDeadline;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private Status status = Status.OPEN;

    @Column(name = "resolved_at")
    private LocalDate resolvedAt;

    @Column(length = 300)
    private String resolver;

    @Column(name = "file_name", length = 500)
    private String fileName;

    @Column(name = "file_path", length = 1000)
    private String filePath;

    @Column(name = "file_size")
    private Long fileSize;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    public enum AuditType { ISMS_P, INTERNAL, OTHER }
    public enum RiskLevel { CRITICAL, HIGH, MEDIUM, LOW }
    public enum Status { OPEN, IN_PROGRESS, RESOLVED, ACCEPTED }
}
