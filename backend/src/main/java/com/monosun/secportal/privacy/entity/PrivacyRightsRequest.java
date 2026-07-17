package com.monosun.secportal.privacy.entity;

import com.monosun.secportal.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

/** 정보주체 권리행사 관리 — 열람·정정·삭제·처리정지·동의철회 및 SLA */
@Entity
@Table(name = "privacy_rights_requests",
        indexes = {
                @Index(name = "idx_prights_status", columnList = "status"),
                @Index(name = "idx_prights_type", columnList = "request_type"),
                @Index(name = "idx_prights_due", columnList = "due_date")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PrivacyRightsRequest extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 요청 유형 */
    @Enumerated(EnumType.STRING)
    @Column(name = "request_type", nullable = false, length = 30)
    @Builder.Default
    private RequestType requestType = RequestType.ACCESS;

    /** 정보주체명 */
    @Column(name = "subject_name", nullable = false, length = 100)
    private String subjectName;

    /** 연락처 */
    @Column(length = 100)
    private String contact;

    /** 접수 채널 */
    @Column(length = 100)
    private String channel;

    /** 요청 내용 */
    @Column(columnDefinition = "TEXT")
    private String content;

    /** 요청일(접수일) */
    @Column(name = "request_date", nullable = false)
    private LocalDate requestDate;

    /** 처리기한 — SLA (법정 10일) */
    @Column(name = "due_date")
    private LocalDate dueDate;

    /** 처리 완료일 */
    @Column(name = "completed_date")
    private LocalDate completedDate;

    /** 처리 담당자 */
    @Column(length = 100)
    private String handler;

    /** 처리결과 */
    @Column(columnDefinition = "TEXT")
    private String result;

    /** 거절 사유 (거절 시) */
    @Column(name = "reject_reason", columnDefinition = "TEXT")
    private String rejectReason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private Status status = Status.RECEIVED;

    @Column(columnDefinition = "TEXT")
    private String notes;

    /**
     * ACCESS: 열람, CORRECTION: 정정, DELETION: 삭제,
     * SUSPENSION: 처리정지, CONSENT_WITHDRAWAL: 동의철회
     */
    public enum RequestType { ACCESS, CORRECTION, DELETION, SUSPENSION, CONSENT_WITHDRAWAL }

    /** RECEIVED: 접수, IN_PROGRESS: 진행중, COMPLETED: 완료, REJECTED: 거절 */
    public enum Status { RECEIVED, IN_PROGRESS, COMPLETED, REJECTED }
}
