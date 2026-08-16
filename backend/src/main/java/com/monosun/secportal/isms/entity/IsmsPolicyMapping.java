package com.monosun.secportal.isms.entity;

import com.monosun.secportal.policy.entity.Policy;
import com.monosun.secportal.policy.entity.PolicyArticle;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * ISMS-P 통제항목 ↔ 보안정책 매핑.
 *
 * <p>{@link #policyArticle} 이 null 이면 <b>장(章) 전체</b> 매핑, 값이 있으면 그 장 안의
 * <b>조(條) 단위</b> 매핑이다. 같은 장의 여러 조를 한 통제항목에 걸 수 있어야 하므로
 * 유니크 제약에 조까지 포함한다.
 */
@Entity
@Table(name = "isms_policy_mappings",
       uniqueConstraints = @UniqueConstraint(columnNames = {"isms_item_id", "policy_id", "policy_article_id"}))
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IsmsPolicyMapping {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "isms_item_id", nullable = false)
    private IsmsItem ismsItem;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "policy_id", nullable = false)
    private Policy policy;

    /** 조 단위 매핑 대상. null 이면 장 전체를 매핑한 것 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "policy_article_id")
    private PolicyArticle policyArticle;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void prePersist() {
        this.createdAt = LocalDateTime.now();
    }
}
