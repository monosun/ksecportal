package com.monosun.secportal.privacy.entity;

import com.monosun.secportal.auth.entity.User;
import com.monosun.secportal.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

/** 개인정보 수집·이용 동의서의 버전 이력 — 버전별 변경사유 및 첨부파일을 보관한다. */
@Entity
@Table(name = "privacy_consent_versions",
        indexes = @Index(name = "idx_pcv_consent", columnList = "consent_id"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PrivacyConsentVersion extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "consent_id", nullable = false)
    private PrivacyConsent consent;

    /** 버전 (예: 1.0, 2.0) */
    @Column(nullable = false, length = 50)
    private String version;

    /** 시행일 */
    @Column(name = "effective_date")
    private LocalDate effectiveDate;

    /** 변경 사유 */
    @Column(name = "change_note", columnDefinition = "TEXT")
    private String changeNote;

    /** 첨부파일 */
    @Column(name = "file_name", length = 500)
    private String fileName;

    @Column(name = "file_path", length = 1000)
    private String filePath;

    @Column(name = "file_size")
    private Long fileSize;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id")
    private User author;
}
