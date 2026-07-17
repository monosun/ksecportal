package com.monosun.secportal.privacy.entity;

import com.monosun.secportal.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

/** 개인정보 파일관리 — 개인정보파일대장 */
@Entity
@Table(name = "privacy_files",
        indexes = {
                @Index(name = "idx_pfile_status", columnList = "status"),
                @Index(name = "idx_pfile_name", columnList = "name")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PrivacyFile extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 개인정보파일명 (예: 가입자정보, 배송정보, 본인인증정보, VOC, 마케팅) */
    @Column(nullable = false, length = 200)
    private String name;

    /** 담당부서 */
    @Column(length = 100)
    private String department;

    /** 운영 시스템 */
    @Column(name = "system_name", length = 200)
    private String systemName;

    /** DB 테이블 */
    @Column(name = "db_table", length = 200)
    private String dbTable;

    /** 개인정보 항목 */
    @Column(name = "info_items", columnDefinition = "TEXT")
    private String infoItems;

    /** 보유기간 */
    @Column(name = "retention_period", length = 200)
    private String retentionPeriod;

    /** 민감정보 포함 여부 */
    @Column(name = "sensitive_info")
    @Builder.Default
    private Boolean sensitiveInfo = false;

    /** 고유식별정보 포함 여부 */
    @Column(name = "unique_identifier")
    @Builder.Default
    private Boolean uniqueIdentifier = false;

    /** 보유 건수 */
    @Column(name = "record_count")
    private Integer recordCount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private Status status = Status.ACTIVE;

    @Column(columnDefinition = "TEXT")
    private String notes;

    public enum Status { ACTIVE, INACTIVE }
}
