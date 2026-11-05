package com.monosun.secportal.policydoc.entity;

import com.monosun.secportal.auth.entity.User;
import com.monosun.secportal.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

/**
 * 정책 문서의 버전(개정본) — 버전 1건 = 파일 1개 + 개정사항.
 * 현행본은 개정일 최신(같으면 id 최신) 개정본이다.
 */
@Entity
@Table(name = "policy_mgmt_revisions",
        indexes = @Index(name = "idx_policy_mgmt_revisions_doc", columnList = "doc_id"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PolicyDocRevision extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "doc_id", nullable = false)
    private PolicyDoc document;

    @Column(nullable = false, length = 50)
    private String version;

    @Enumerated(EnumType.STRING)
    @Column(name = "revision_type", nullable = false, length = 20)
    @Builder.Default
    private RevisionType revisionType = RevisionType.AMENDMENT;

    /** 제·개정일 */
    @Column(name = "revision_date", nullable = false)
    private LocalDate revisionDate;

    /** 시행일 */
    @Column(name = "effective_date")
    private LocalDate effectiveDate;

    /** 승인자 (예: 정보보호최고책임자) */
    @Column(length = 100)
    private String approver;

    /** 개정 사유 */
    @Column(name = "change_reason", columnDefinition = "TEXT")
    private String changeReason;

    /** 주요 개정사항 */
    @Column(name = "change_details", columnDefinition = "TEXT")
    private String changeDetails;

    @Column(name = "file_name", length = 500)
    private String fileName;

    @Column(name = "file_path", length = 1000)
    private String filePath;

    @Column(name = "file_size")
    private Long fileSize;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uploader_id")
    private User uploader;

    /** 제정 / 개정 / 폐지 */
    public enum RevisionType {
        ENACTMENT, AMENDMENT, ABOLITION
    }
}
