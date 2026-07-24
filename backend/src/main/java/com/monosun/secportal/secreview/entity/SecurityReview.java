package com.monosun.secportal.secreview.entity;

import com.monosun.secportal.auth.entity.User;
import com.monosun.secportal.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 보안성 심의 — 신규 시스템 구축·변경 시 설계 단계에서 보안 요구사항 충족 여부를 검토한 기록.
 * 요청(현업) → 검토(보안담당) → 심의 결과(승인/조건부승인/반려) 순으로 진행한다.
 */
@Entity
@Table(name = "security_reviews", indexes = {
        @Index(name = "idx_sec_review_status", columnList = "status"),
        @Index(name = "idx_sec_review_created", columnList = "created_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SecurityReview extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 심의 제목 (예: 고객포털 2.0 신규 구축 보안성 심의) */
    @Column(nullable = false, length = 300)
    private String title;

    /** 대상 시스템·서비스명 */
    @Column(name = "system_name", nullable = false, length = 200)
    private String systemName;

    @Enumerated(EnumType.STRING)
    @Column(name = "review_type", nullable = false, length = 20)
    @Builder.Default
    private ReviewType reviewType = ReviewType.NEW;

    /** 요청 부서 */
    @Column(length = 100)
    private String department;

    /** 요청자 (현업 담당자) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requester_id")
    private User requester;

    /** 구축·변경 개요, 주요 기능, 처리 데이터 등 */
    @Column(columnDefinition = "TEXT")
    private String description;

    /** 개인정보 처리 여부 — 처리 시 개인정보 보호조치 항목을 반드시 검토한다 */
    @Column(name = "handles_personal_data", nullable = false)
    @Builder.Default
    private boolean handlesPersonalData = false;

    /** 외부(인터넷) 공개 여부 */
    @Column(name = "internet_facing", nullable = false)
    @Builder.Default
    private boolean internetFacing = false;

    /** 오픈(적용) 예정일 */
    @Column(name = "target_date")
    private LocalDate targetDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private Status status = Status.REQUESTED;

    /** 심의 결과 (심의 완료 시 확정) */
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private Decision decision;

    /** 심의 의견 — 승인 조건, 보완 요구사항 */
    @Column(name = "review_comment", columnDefinition = "TEXT")
    private String reviewComment;

    /** 심의자 (보안 담당) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewer_id")
    private User reviewer;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    /** 설계서 등 첨부 */
    @Column(name = "file_name", length = 500)
    private String fileName;

    @Column(name = "file_path", length = 1000)
    private String filePath;

    @OneToMany(mappedBy = "review", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC, id ASC")
    @Builder.Default
    private List<SecurityReviewItem> items = new ArrayList<>();

    public enum ReviewType {
        NEW,            // 신규 구축
        CHANGE,         // 변경·고도화
        INTEGRATION,    // 외부 연계
        DECOMMISSION    // 폐기·종료
    }

    public enum Status {
        REQUESTED,   // 심의 요청 접수
        IN_REVIEW,   // 검토중
        REVISION,    // 보완 요청 (요청부서 조치 대기)
        COMPLETED    // 심의 완료 (decision 확정)
    }

    public enum Decision {
        APPROVED,      // 승인
        CONDITIONAL,   // 조건부 승인 (조건 이행 필요)
        REJECTED       // 반려
    }
}
