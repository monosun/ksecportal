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

    /**
     * 이 결함사항이 어디서 왔는지.
     * <ul>
     *   <li>{@code MANUAL} — 보안 결함사항 화면에서 직접 등록한 건</li>
     *   <li>{@code ISMS_DEFECT} — ISMS 결함관리에서 자동으로 가져온 건(원본이 바뀌면 함께 갱신·삭제된다)</li>
     * </ul>
     * 기존 행에는 값이 없을 수 있으므로(ddl-auto 로 추가된 컬럼) 읽을 때 null 을 MANUAL 로 본다.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", length = 20)
    @Builder.Default
    private SourceType sourceType = SourceType.MANUAL;

    /** 원본 ISMS 결함 id — ISMS 결함 1건당 결함사항 1건(유니크). 직접 등록한 건은 null */
    @Column(name = "source_defect_id", unique = true)
    private Long sourceDefectId;

    /** 출처 표시용 라벨 — 예: "ISMS-P 갱신심사 · 결함-01" */
    @Column(name = "source_label", length = 100)
    private String sourceLabel;

    /** null(구 데이터)은 직접 등록으로 본다 */
    public SourceType resolvedSourceType() {
        return sourceType != null ? sourceType : SourceType.MANUAL;
    }

    /** ISMS 결함관리에서 가져온 건은 이 화면에서 수정·삭제할 수 없다 */
    public boolean isFromIsmsDefect() {
        return resolvedSourceType() == SourceType.ISMS_DEFECT;
    }

    public enum SourceType { MANUAL, ISMS_DEFECT }

    public enum AuditType { ISMS_P, INTERNAL, OTHER }
    public enum RiskLevel { CRITICAL, HIGH, MEDIUM, LOW }
    public enum Status { OPEN, IN_PROGRESS, RESOLVED, ACCEPTED }
}
