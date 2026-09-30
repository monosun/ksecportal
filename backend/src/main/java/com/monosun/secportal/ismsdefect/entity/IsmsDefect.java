package com.monosun.secportal.ismsdefect.entity;

import com.monosun.secportal.auth.entity.User;
import com.monosun.secportal.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

/**
 * ISMS-P 심사 결함(부적합) 1건.
 *
 * 증적관리(isms_evidences)와 마찬가지로 <b>연도</b>가 1차 구분 기준이며,
 * 한 연도 안에서 결함번호 순으로 관리한다.
 */
@Entity
@Table(name = "isms_defects",
        indexes = {
                @Index(name = "idx_isms_defects_year", columnList = "year"),
                @Index(name = "idx_isms_defects_status", columnList = "status")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IsmsDefect extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 심사 연도 */
    @Column(nullable = false)
    private int year;

    /** 심사 구분 — 최초/사후/갱신/내부/기타 */
    @Enumerated(EnumType.STRING)
    @Column(name = "audit_type", nullable = false, length = 20)
    @Builder.Default
    private AuditType auditType = AuditType.RENEWAL;

    /** 결함번호 — 심사기관이 부여한 번호(예: 결함-01). 비어 있어도 된다. */
    @Column(name = "defect_no", length = 50)
    private String defectNo;

    /** 결함 구분 — 결함/권고/개선사항 */
    @Enumerated(EnumType.STRING)
    @Column(name = "defect_type", nullable = false, length = 20)
    @Builder.Default
    private DefectType defectType = DefectType.DEFECT;

    /** 중요도 */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private Severity severity = Severity.MEDIUM;

    /** 인증기준 분야(예: 2.10 시스템 및 서비스 보안관리) */
    @Column(name = "domain_name", length = 200)
    private String domainName;

    /** 인증기준 코드(예: 2.10.1) — ISMS-P 101개 항목의 itemCode 와 같은 값을 쓴다 */
    @Column(name = "item_code", length = 50)
    private String itemCode;

    /** 인증기준명 */
    @Column(name = "item_name", length = 300)
    private String itemName;

    /** 결함 제목(요약) */
    @Column(nullable = false, length = 500)
    private String title;

    /** 결함 내용 — 심사원이 지적한 내용 전문 */
    @Column(columnDefinition = "TEXT")
    private String content;

    /** 결함 원인 */
    @Column(columnDefinition = "TEXT")
    private String cause;

    /** 조치 계획 */
    @Column(name = "action_plan", columnDefinition = "TEXT")
    private String actionPlan;

    /** 조치 내용(결과) */
    @Column(name = "action_result", columnDefinition = "TEXT")
    private String actionResult;

    /** 재발방지 대책 */
    @Column(name = "prevention_plan", columnDefinition = "TEXT")
    private String preventionPlan;

    /** 조치 기한 */
    @Column(name = "due_date")
    private LocalDate dueDate;

    /** 조치 완료일 */
    @Column(name = "completed_date")
    private LocalDate completedDate;

    /** 조치 담당자 */
    @Column(length = 100)
    private String assignee;

    /** 담당 부서 */
    @Column(length = 100)
    private String department;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private Status status = Status.OPEN;

    @Column(name = "file_name", length = 500)
    private String fileName;

    @Column(name = "file_path", length = 1000)
    private String filePath;

    @Column(name = "file_size")
    private Long fileSize;

    /** 목록 정렬용 — 같은 연도 안에서의 표시 순서 */
    @Column(name = "sort_order")
    @Builder.Default
    private Integer sortOrder = 0;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "registrant_id")
    private User registrant;

    public enum AuditType { INITIAL, FOLLOWUP, RENEWAL, INTERNAL, OTHER }

    public enum DefectType { DEFECT, RECOMMENDATION, IMPROVEMENT }

    public enum Severity { CRITICAL, HIGH, MEDIUM, LOW }

    public enum Status { OPEN, IN_PROGRESS, COMPLETED, HOLD }
}
