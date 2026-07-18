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

    @Column(nullable = false)
    private int sortOrder;

    @OneToMany(mappedBy = "item", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<IsmsEvidence> evidences = new ArrayList<>();
}
