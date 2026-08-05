package com.monosun.secportal.bcp.dto;

import com.monosun.secportal.bcp.entity.BcpExercise;
import com.monosun.secportal.bcp.entity.BcpExerciseStep;
import com.monosun.secportal.bcp.entity.BcpScenario;
import com.monosun.secportal.bcp.entity.BcpScenarioStep;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

public class BcpDto {

    // ── Scenario ───────────────────────────────────────────────────────────

    @Getter
    public static class ScenarioStepRequest {
        private Integer stepOrder;
        @NotBlank private String title;
        private String roleName;
        private String action;
        private Integer targetMinutes;
        private String successCriteria;
    }

    @Getter
    public static class ScenarioRequest {
        @NotBlank private String name;
        @NotBlank private String category;
        private String difficulty;
        private String targetSystem;
        private Integer rtoMinutes;
        private Integer rpoMinutes;
        private String situation;
        private String objective;
        private String description;
        @Valid private List<ScenarioStepRequest> steps;
    }

    @Getter @Builder
    public static class ScenarioStepResponse {
        private Long id;
        private Integer stepOrder;
        private String title;
        private String roleName;
        private String action;
        private Integer targetMinutes;
        private String successCriteria;

        public static ScenarioStepResponse from(BcpScenarioStep s) {
            return ScenarioStepResponse.builder()
                    .id(s.getId())
                    .stepOrder(s.getStepOrder())
                    .title(s.getTitle())
                    .roleName(s.getRoleName())
                    .action(s.getAction())
                    .targetMinutes(s.getTargetMinutes())
                    .successCriteria(s.getSuccessCriteria())
                    .build();
        }
    }

    @Getter @Builder
    public static class ScenarioResponse {
        private Long id;
        private String name;
        private String category;
        private String difficulty;
        private String targetSystem;
        private Integer rtoMinutes;
        private Integer rpoMinutes;
        private String situation;
        private String objective;
        private String description;
        private boolean active;
        private int stepCount;
        private List<ScenarioStepResponse> steps;
        private String createdBy;
        private LocalDateTime createdAt;

        public static ScenarioResponse from(BcpScenario s) {
            List<ScenarioStepResponse> steps = s.getSteps().stream()
                    .map(ScenarioStepResponse::from).toList();
            return ScenarioResponse.builder()
                    .id(s.getId())
                    .name(s.getName())
                    .category(s.getCategory())
                    .difficulty(s.getDifficulty().name())
                    .targetSystem(s.getTargetSystem())
                    .rtoMinutes(s.getRtoMinutes())
                    .rpoMinutes(s.getRpoMinutes())
                    .situation(s.getSituation())
                    .objective(s.getObjective())
                    .description(s.getDescription())
                    .active(s.isActive())
                    .stepCount(steps.size())
                    .steps(steps)
                    .createdBy(s.getCreatedBy() != null ? s.getCreatedBy().getName() : null)
                    .createdAt(s.getCreatedAt())
                    .build();
        }
    }

    // ── Exercise ───────────────────────────────────────────────────────────

    @Getter
    public static class ExerciseRequest {
        @NotBlank private String name;
        @NotNull private Long scenarioId;
        private String method;
        private LocalDateTime plannedAt;
        private String leaderName;
        private String participants;
        private Integer participantCount;
        private String description;
    }

    /** 단계별 수행 결과 기록 */
    @Getter
    public static class StepResultRequest {
        private String result;          // PENDING / PASS / PARTIAL / FAIL
        private Integer actualMinutes;
        private String note;
    }

    /** 훈련 완료 처리 — 실제 RTO/RPO와 총평·개선사항을 함께 남긴다 */
    @Getter
    public static class CompleteRequest {
        private Integer actualRtoMinutes;
        private Integer actualRpoMinutes;
        private String summary;
        private String improvement;
    }

    @Getter @Builder
    public static class ExerciseStepResponse {
        private Long id;
        private Integer stepOrder;
        private String title;
        private String roleName;
        private String action;
        private Integer targetMinutes;
        private String successCriteria;
        private Integer actualMinutes;
        private String result;
        private String note;
        private LocalDateTime completedAt;

        public static ExerciseStepResponse from(BcpExerciseStep s) {
            return ExerciseStepResponse.builder()
                    .id(s.getId())
                    .stepOrder(s.getStepOrder())
                    .title(s.getTitle())
                    .roleName(s.getRoleName())
                    .action(s.getAction())
                    .targetMinutes(s.getTargetMinutes())
                    .successCriteria(s.getSuccessCriteria())
                    .actualMinutes(s.getActualMinutes())
                    .result(s.getResult().name())
                    .note(s.getNote())
                    .completedAt(s.getCompletedAt())
                    .build();
        }
    }

    @Getter @Builder
    public static class ExerciseResponse {
        private Long id;
        private String name;
        private Long scenarioId;
        private String scenarioName;
        private String category;
        private String method;
        private String status;
        private LocalDateTime plannedAt;
        private LocalDateTime startedAt;
        private LocalDateTime endedAt;
        private String leaderName;
        private String participants;
        private Integer participantCount;
        private Integer rtoMinutes;         // 시나리오 목표 RTO
        private Integer rpoMinutes;         // 시나리오 목표 RPO
        private Integer actualRtoMinutes;
        private Integer actualRpoMinutes;
        private Boolean rtoMet;             // 목표 RTO 달성 여부 (측정값이 있을 때만)
        private Integer score;
        private String result;
        private String summary;
        private String improvement;
        private String description;
        private int totalSteps;
        private long passedSteps;
        private long partialSteps;
        private long failedSteps;
        private long pendingSteps;
        private String createdBy;
        private LocalDateTime createdAt;

        public static ExerciseResponse from(BcpExercise e) {
            List<BcpExerciseStep> steps = e.getSteps();
            BcpScenario sc = e.getScenario();
            Integer targetRto = sc != null ? sc.getRtoMinutes() : null;
            Boolean rtoMet = (targetRto != null && e.getActualRtoMinutes() != null)
                    ? e.getActualRtoMinutes() <= targetRto : null;
            return ExerciseResponse.builder()
                    .id(e.getId())
                    .name(e.getName())
                    .scenarioId(sc != null ? sc.getId() : null)
                    .scenarioName(sc != null ? sc.getName() : null)
                    .category(sc != null ? sc.getCategory() : null)
                    .method(e.getMethod().name())
                    .status(e.getStatus().name())
                    .plannedAt(e.getPlannedAt())
                    .startedAt(e.getStartedAt())
                    .endedAt(e.getEndedAt())
                    .leaderName(e.getLeaderName())
                    .participants(e.getParticipants())
                    .participantCount(e.getParticipantCount())
                    .rtoMinutes(targetRto)
                    .rpoMinutes(sc != null ? sc.getRpoMinutes() : null)
                    .actualRtoMinutes(e.getActualRtoMinutes())
                    .actualRpoMinutes(e.getActualRpoMinutes())
                    .rtoMet(rtoMet)
                    .score(e.getScore())
                    .result(e.getResult() != null ? e.getResult().name() : null)
                    .summary(e.getSummary())
                    .improvement(e.getImprovement())
                    .description(e.getDescription())
                    .totalSteps(steps.size())
                    .passedSteps(countBy(steps, BcpExerciseStep.StepResult.PASS))
                    .partialSteps(countBy(steps, BcpExerciseStep.StepResult.PARTIAL))
                    .failedSteps(countBy(steps, BcpExerciseStep.StepResult.FAIL))
                    .pendingSteps(countBy(steps, BcpExerciseStep.StepResult.PENDING))
                    .createdBy(e.getCreatedBy() != null ? e.getCreatedBy().getName() : null)
                    .createdAt(e.getCreatedAt())
                    .build();
        }

        private static long countBy(List<BcpExerciseStep> steps, BcpExerciseStep.StepResult r) {
            return steps.stream().filter(s -> s.getResult() == r).count();
        }
    }

    @Getter @Builder
    public static class ExerciseDetail {
        private ExerciseResponse exercise;
        private List<ExerciseStepResponse> steps;
    }
}
