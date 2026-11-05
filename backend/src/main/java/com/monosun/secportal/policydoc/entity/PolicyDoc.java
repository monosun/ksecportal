package com.monosun.secportal.policydoc.entity;

import com.monosun.secportal.auth.entity.User;
import com.monosun.secportal.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

/**
 * 보안정책관리 — 정책 문서(규정·지침·가이드·절차서·매뉴얼) 마스터.
 * 버전별 파일과 개정사항은 {@link PolicyDocRevision} 이 1:N 으로 가진다.
 */
@Entity
@Table(name = "policy_mgmt_docs",
        indexes = {
                @Index(name = "idx_policy_mgmt_docs_category", columnList = "category"),
                @Index(name = "idx_policy_mgmt_docs_status", columnList = "status")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PolicyDoc extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 문서번호 (예: REG-001) — 비워 두면 분류 키워드로 자동 부여 */
    @Column(name = "doc_no", length = 50)
    private String docNo;

    @Column(nullable = false, length = 300)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Category category;

    /** 주관부서 */
    @Column(length = 100)
    private String department;

    /** 문서 관리 담당자 */
    @Column(length = 100)
    private String owner;

    @Column(columnDefinition = "TEXT")
    private String description;

    /** 최신 개정 유형이 폐지이면 ABOLISHED — 개정 이력이 바뀔 때마다 서비스가 다시 맞춘다 */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private Status status = Status.ACTIVE;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "registrant_id")
    private User registrant;

    /**
     * 규정 / 지침 / 가이드 / 절차서 / 매뉴얼 / 기타.
     * prefix 는 문서번호를 비워 두었을 때 자동 부여하는 구분 키워드 — {@code REG-001}.
     * MySQL ENUM 컬럼이므로 값을 추가하면 마이그레이션(ALTER ... MODIFY)이 필요하다.
     */
    public enum Category {
        REGULATION("REG"), GUIDELINE("GDL"), GUIDE("GDE"), PROCEDURE("PRC"), MANUAL("MNL"), OTHER("ETC");

        private final String prefix;

        Category(String prefix) { this.prefix = prefix; }

        public String getPrefix() { return prefix; }
    }

    /** 시행 / 폐지 */
    public enum Status {
        ACTIVE, ABOLISHED
    }
}
