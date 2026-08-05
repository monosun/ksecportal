package com.monosun.secportal.bcp.entity;

import com.monosun.secportal.auth.entity.User;
import com.monosun.secportal.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** 재해복구·BCP 훈련 실시 기록. 단계 수행 결과를 누적해 최종 점수·판정을 남긴다. */
@Entity
@Table(name = "bcp_exercises")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class BcpExercise extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "scenario_id", nullable = false)
    private BcpScenario scenario;

    /** 훈련 방식 — 도상훈련 / 시뮬레이션 / 실제 전환 */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private Method method = Method.TABLETOP;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private Status status = Status.DRAFT;

    private LocalDateTime plannedAt;
    private LocalDateTime startedAt;
    private LocalDateTime endedAt;

    /** 훈련 총괄자 */
    private String leaderName;

    /** 참가자 명단(쉼표 구분) */
    @Column(columnDefinition = "TEXT")
    private String participants;

    /** 참가 인원 */
    private Integer participantCount;

    /** 실제 복구시간(분) */
    private Integer actualRtoMinutes;

    /** 실제 복구시점(분) */
    private Integer actualRpoMinutes;

    /** 단계 수행 결과로 산출한 달성률(0~100) */
    private Integer score;

    /** 최종 판정 */
    @Enumerated(EnumType.STRING)
    private Result result;

    /** 훈련 총평 */
    @Column(columnDefinition = "TEXT")
    private String summary;

    /** 도출된 개선사항 */
    @Column(columnDefinition = "TEXT")
    private String improvement;

    @Column(columnDefinition = "TEXT")
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @OneToMany(mappedBy = "exercise", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("stepOrder ASC")
    @Builder.Default
    private List<BcpExerciseStep> steps = new ArrayList<>();

    public enum Method { TABLETOP, SIMULATION, FAILOVER }
    public enum Status { DRAFT, RUNNING, COMPLETED, CANCELLED }
    public enum Result { PASS, PARTIAL, FAIL }
}
