package com.monosun.secportal.isms.entity;

import com.monosun.secportal.auth.entity.User;
import com.monosun.secportal.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "isms_evidences")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IsmsEvidence extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_id", nullable = false)
    private IsmsItem item;

    @Column(nullable = false)
    private int year;

    @Column(nullable = false, length = 500)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(length = 500)
    private String fileName;

    @Column(length = 1000)
    private String filePath;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private Status status = Status.COMPLIANT;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "source_evidence_id")
    private IsmsEvidence sourceEvidence;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "registrant_id")
    private User registrant;

    /** 전년도 증적 가져오기로 생성된 경우 원본 연도. 직접 등록한 증적은 null (가져오기 초기화 대상 식별용) */
    @Column(name = "copied_from_year")
    private Integer copiedFromYear;

    /** 유효기간 기준일시. null 이면 등록일시(createdAt)를 기준으로 한다.
     *  전년도 증적을 복사하면 원본의 기준일시를 이어받아 복사만으로 유효기간이 갱신되지 않게 하고,
     *  첨부파일을 새로 올리면 그 시점으로 갱신한다. */
    @Column(name = "valid_from")
    private LocalDateTime validFrom;

    /** 유효기간 계산 기준일시 (validFrom 이 없으면 등록일시) */
    public LocalDateTime getEffectiveValidFrom() {
        return validFrom != null ? validFrom : getCreatedAt();
    }

    public enum Status {
        COMPLIANT, PARTIAL, NON_COMPLIANT, NA
    }
}
