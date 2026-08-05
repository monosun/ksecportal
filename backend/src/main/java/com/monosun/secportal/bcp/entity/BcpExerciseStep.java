package com.monosun.secportal.bcp.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 훈련 실시 시점에 시나리오 단계를 복사해 둔 수행 기록.
 *
 * 시나리오 원본을 참조하지 않고 값을 복사하므로, 시나리오가 수정·삭제되어도 과거 훈련 결과는 그대로 남는다.
 */
@Entity
@Table(name = "bcp_exercise_steps")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class BcpExerciseStep {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exercise_id", nullable = false)
    private BcpExercise exercise;

    @Column(nullable = false)
    private Integer stepOrder;

    @Column(nullable = false)
    private String title;

    private String roleName;

    @Column(columnDefinition = "TEXT")
    private String action;

    private Integer targetMinutes;

    @Column(columnDefinition = "TEXT")
    private String successCriteria;

    /** 실제 소요시간(분) */
    private Integer actualMinutes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private StepResult result = StepResult.PENDING;

    @Column(columnDefinition = "TEXT")
    private String note;

    private LocalDateTime completedAt;

    public enum StepResult { PENDING, PASS, PARTIAL, FAIL }
}
