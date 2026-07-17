package com.monosun.secportal.privacy.entity;

import com.monosun.secportal.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

/** 개인정보 보호조치 관리 — 접근권한·암호화·접속기록·출력물·반출·휴면계정 등 관리현황 */
@Entity
@Table(name = "privacy_safeguards",
        indexes = {
                @Index(name = "idx_psafe_status", columnList = "status"),
                @Index(name = "idx_psafe_type", columnList = "safeguard_type")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PrivacySafeguard extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 보호조치 유형 */
    @Enumerated(EnumType.STRING)
    @Column(name = "safeguard_type", nullable = false, length = 30)
    @Builder.Default
    private SafeguardType safeguardType = SafeguardType.ACCESS_REVIEW;

    /** 조치·점검명 */
    @Column(nullable = false, length = 200)
    private String title;

    /** 대상 시스템 */
    @Column(name = "target_system", length = 200)
    private String targetSystem;

    /** 담당부서 */
    @Column(length = 100)
    private String department;

    /** 점검(조치)일 */
    @Column(name = "check_date")
    private LocalDate checkDate;

    /** 수행자 */
    @Column(length = 100)
    private String performer;

    /** 점검 대상 건수 */
    @Column(name = "target_count")
    private Integer targetCount;

    /** 조치 건수 (권한회수·휴면전환 등) */
    @Column(name = "action_count")
    private Integer actionCount;

    /** 점검·조치 결과 */
    @Column(columnDefinition = "TEXT")
    private String result;

    /** 발견된 문제점 */
    @Column(columnDefinition = "TEXT")
    private String findings;

    /** 개선조치 내용 */
    @Column(columnDefinition = "TEXT")
    private String improvement;

    /** 차기 점검 예정일 */
    @Column(name = "next_check_date")
    private LocalDate nextCheckDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private Status status = Status.PLANNED;

    @Column(columnDefinition = "TEXT")
    private String notes;

    /**
     * ACCESS_REVIEW: 접근권한 점검, ACCESS_REVOKE: 권한회수,
     * ENCRYPTION: 암호화 적용현황, ACCESS_LOG_REVIEW: 접속기록 점검결과,
     * PRINTOUT: 출력물 관리, EXPORT: 반출관리, DORMANT_ACCOUNT: 휴면계정 관리
     */
    public enum SafeguardType {
        ACCESS_REVIEW, ACCESS_REVOKE, ENCRYPTION, ACCESS_LOG_REVIEW,
        PRINTOUT, EXPORT, DORMANT_ACCOUNT
    }

    /** PLANNED: 계획, IN_PROGRESS: 진행중, COMPLETED: 완료 */
    public enum Status { PLANNED, IN_PROGRESS, COMPLETED }
}
