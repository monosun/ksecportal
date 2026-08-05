package com.monosun.secportal.bcp.entity;

import jakarta.persistence.*;
import lombok.*;

/** 시나리오의 대응 단계 — 훈련 실시 시 {@link BcpExerciseStep} 으로 복사된다. */
@Entity
@Table(name = "bcp_scenario_steps")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class BcpScenarioStep {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "scenario_id", nullable = false)
    private BcpScenario scenario;

    @Column(nullable = false)
    private Integer stepOrder;

    @Column(nullable = false)
    private String title;

    /** 수행 주체(담당 역할) */
    private String roleName;

    /** 수행 절차 */
    @Column(columnDefinition = "TEXT")
    private String action;

    /** 목표 소요시간(분) */
    private Integer targetMinutes;

    /** 성공 판정 기준 */
    @Column(columnDefinition = "TEXT")
    private String successCriteria;
}
