package com.monosun.secportal.glossary.entity;

import com.monosun.secportal.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

/**
 * 보안 용어집 항목 — 보안 가이드 및 자료 &gt; 보안용어집에서 조회하고,
 * 관리 &gt; 코드 관리 &gt; 용어집 탭에서 관리한다.
 */
@Entity
@Table(name = "glossary_terms",
        indexes = {
                @Index(name = "idx_glossary_category", columnList = "category"),
                @Index(name = "idx_glossary_name", columnList = "name"),
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GlossaryTerm extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 한글 용어 */
    @Column(nullable = false, length = 200)
    private String name;

    /** 영문 표기 */
    @Column(name = "name_en", length = 300)
    private String nameEn;

    /** 약어 (AAA·MFA·SIEM 등, 없을 수 있음) */
    @Column(length = 50)
    private String abbreviation;

    /** 분류 (접근통제·암호기술·사고대응 등) */
    @Column(length = 100)
    private String category;

    /** 의미 */
    @Column(columnDefinition = "TEXT")
    private String definition;

    /** 관련 키워드 (쉼표 구분) */
    @Column(columnDefinition = "TEXT")
    private String keywords;

    @Column(name = "sort_order")
    @Builder.Default
    private Integer sortOrder = 0;

    /** 미사용 처리하면 용어집 화면에서 제외된다 */
    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;
}
