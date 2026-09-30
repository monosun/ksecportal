package com.monosun.secportal.ismsdefect.entity;

import com.monosun.secportal.auth.entity.User;
import com.monosun.secportal.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

/**
 * 연도별 ISMS-P 결함 조치 보고서 — 연도당 1건(upsert).
 *
 * 결함 개별 건이 아니라 "그 해 심사 전체"에 대한 개요·총평·보고서 본문을 담는다.
 */
@Entity
@Table(name = "isms_defect_reports",
        uniqueConstraints = @UniqueConstraint(name = "uk_isms_defect_report_year", columnNames = "year"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IsmsDefectReport extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private int year;

    @Column(length = 500)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(name = "audit_type", nullable = false, length = 20)
    @Builder.Default
    private IsmsDefect.AuditType auditType = IsmsDefect.AuditType.RENEWAL;

    /** 심사기관(인증심사기관) */
    @Column(name = "audit_org", length = 200)
    private String auditOrg;

    /** 심사원 명단 */
    @Column(length = 500)
    private String auditors;

    /** 인증 범위 */
    @Column(name = "audit_scope", length = 1000)
    private String auditScope;

    @Column(name = "audit_start_date")
    private LocalDate auditStartDate;

    @Column(name = "audit_end_date")
    private LocalDate auditEndDate;

    /** 심사 총평 */
    @Column(columnDefinition = "TEXT")
    private String summary;

    /** 보고서 내용(본문) */
    @Column(columnDefinition = "TEXT")
    private String content;

    /** 종합 의견·결론 */
    @Column(columnDefinition = "TEXT")
    private String conclusion;

    /** 보고일 */
    @Column(name = "reported_at")
    private LocalDate reportedAt;

    /** 보고자 */
    @Column(length = 100)
    private String reporter;

    @Column(name = "file_name", length = 500)
    private String fileName;

    @Column(name = "file_path", length = 1000)
    private String filePath;

    @Column(name = "file_size")
    private Long fileSize;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "registrant_id")
    private User registrant;
}
