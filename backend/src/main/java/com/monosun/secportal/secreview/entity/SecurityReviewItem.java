package com.monosun.secportal.secreview.entity;

import com.monosun.secportal.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

/**
 * 보안성 심의 검토 항목 — 심의 1건에 딸린 설계 검토 체크리스트 한 줄.
 * 심의를 만들 때 기본 체크리스트가 복사되며, 심의별로 항목을 추가·삭제할 수 있다.
 */
@Entity
@Table(name = "security_review_items", indexes = @Index(name = "idx_sec_review_item", columnList = "review_id"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SecurityReviewItem extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "review_id", nullable = false)
    private SecurityReview review;

    /** 검토 영역 (인증·권한, 암호화, 로그·감사 …) */
    @Column(nullable = false, length = 100)
    private String category;

    /** 검토 항목 */
    @Column(name = "item_name", nullable = false, length = 500)
    private String itemName;

    /** 검토 기준·확인 방법 */
    @Column(columnDefinition = "TEXT")
    private String criteria;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private Result result = Result.PENDING;

    /** 검토 의견·보완 요구사항 */
    @Column(columnDefinition = "TEXT")
    private String comment;

    @Column(name = "sort_order", nullable = false)
    @Builder.Default
    private int sortOrder = 0;

    public enum Result {
        PENDING,    // 미검토
        PASS,       // 적합
        FAIL,       // 부적합 (보완 필요)
        NA          // 해당없음
    }
}
