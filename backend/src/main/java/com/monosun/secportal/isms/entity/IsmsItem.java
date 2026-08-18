package com.monosun.secportal.isms.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "isms_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IsmsItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 20)
    private String itemCode;

    @Column(nullable = false, length = 200)
    private String itemName;

    @Column(nullable = false, length = 10)
    private String domainCode;

    @Column(nullable = false, length = 100)
    private String domainName;

    @Column(nullable = false)
    private int sectionNum;

    @Column(nullable = false, length = 100)
    private String sectionName;

    @Column(columnDefinition = "TEXT")
    private String description;

    /** 이행 가이드 — 이 인증기준을 어떻게 충족·준비하는지에 대한 설명.
     *  인증기준 자체에 대한 안내이므로 연도와 무관하게 항목에 저장한다.
     *  (시더는 없는 항목만 INSERT 하므로 사용자가 작성한 값은 유지된다) */
    @Column(columnDefinition = "TEXT")
    private String guide;

    /** 기본 증적제목 — 항목별 대표 증적의 제목. 일괄등록 템플릿·기본값으로 사용한다.
     *  연도와 무관한 항목 속성이며 코드관리 화면에서 편집한다. */
    @Column(columnDefinition = "TEXT")
    private String defaultEvidenceTitle;

    /** 기본 증적내용 — 항목별 대표 증적의 내용(증적예시). */
    @Column(columnDefinition = "TEXT")
    private String defaultEvidenceContent;

    /** 예시 증적자료명 — 이 인증기준에서 통상 요구되는 증적 문서의 이름 목록.
     *  줄바꿈으로 구분된 한 줄 = 자료명 하나이며, 이행 가이드와 함께 표시한다.
     *  연도와 무관한 항목 속성이다. */
    @Column(columnDefinition = "TEXT")
    private String evidenceExamples;

    @Column(nullable = false)
    private int sortOrder;

    @OneToMany(mappedBy = "item", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<IsmsEvidence> evidences = new ArrayList<>();
}
