package com.monosun.secportal.bcp.entity;

import com.monosun.secportal.auth.entity.User;
import com.monosun.secportal.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

/**
 * 재해복구(DR)·업무연속성(BCP) 훈련 시나리오 템플릿.
 *
 * 시나리오는 훈련의 "대본"이며, 실제 훈련은 {@link BcpExercise} 로 실시한다.
 * 훈련 실시 시점에 시나리오의 단계를 복사해 두므로, 이후 시나리오를 고쳐도 과거 훈련 기록은 보존된다.
 */
@Entity
@Table(name = "bcp_scenarios")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class BcpScenario extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    /** 재해 유형 — 자연재해 / 시스템장애 / 사이버공격 / 시설 / 인적 / 협력사 등 */
    @Column(nullable = false)
    private String category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private Difficulty difficulty = Difficulty.MEDIUM;

    /** 훈련 대상 시스템·업무 */
    private String targetSystem;

    /** 목표 복구시간(RTO, 분) */
    private Integer rtoMinutes;

    /** 목표 복구시점(RPO, 분) */
    private Integer rpoMinutes;

    /** 상황 설정 — 훈련 참가자에게 제시되는 재해 상황 */
    @Column(columnDefinition = "TEXT")
    private String situation;

    /** 훈련 목표 */
    @Column(columnDefinition = "TEXT")
    private String objective;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @OneToMany(mappedBy = "scenario", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("stepOrder ASC")
    @Builder.Default
    private List<BcpScenarioStep> steps = new ArrayList<>();

    public enum Difficulty { EASY, MEDIUM, HARD }
}
