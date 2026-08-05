package com.monosun.secportal.bcp.service;

import com.monosun.secportal.audit.service.AuditLogService;
import com.monosun.secportal.auth.entity.User;
import com.monosun.secportal.bcp.dto.BcpDto;
import com.monosun.secportal.bcp.entity.BcpExercise;
import com.monosun.secportal.bcp.entity.BcpExerciseStep;
import com.monosun.secportal.bcp.entity.BcpScenario;
import com.monosun.secportal.bcp.entity.BcpScenarioStep;
import com.monosun.secportal.bcp.repository.BcpExerciseRepository;
import com.monosun.secportal.bcp.repository.BcpExerciseStepRepository;
import com.monosun.secportal.bcp.repository.BcpScenarioRepository;
import com.monosun.secportal.common.exception.BusinessException;
import com.monosun.secportal.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BcpService {

    private final BcpScenarioRepository scenarioRepo;
    private final BcpExerciseRepository exerciseRepo;
    private final BcpExerciseStepRepository exerciseStepRepo;
    private final AuditLogService auditLogService;

    // ── Scenarios ─────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<BcpDto.ScenarioResponse> listScenarios() {
        return scenarioRepo.findAllWithSteps()
                .stream().map(BcpDto.ScenarioResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public BcpDto.ScenarioResponse getScenario(Long id) {
        return BcpDto.ScenarioResponse.from(findScenario(id));
    }

    @Transactional
    public BcpDto.ScenarioResponse createScenario(BcpDto.ScenarioRequest req, User actor) {
        BcpScenario s = BcpScenario.builder()
                .name(req.getName())
                .category(req.getCategory())
                .difficulty(parseDifficulty(req.getDifficulty()))
                .targetSystem(req.getTargetSystem())
                .rtoMinutes(req.getRtoMinutes())
                .rpoMinutes(req.getRpoMinutes())
                .situation(req.getSituation())
                .objective(req.getObjective())
                .description(req.getDescription())
                .createdBy(actor)
                .build();
        applySteps(s, req.getSteps());

        BcpScenario saved = scenarioRepo.save(s);
        auditLogService.log("BCP_SCENARIO_CREATED", "BCP_SCENARIO", saved.getId(), "name=" + saved.getName());
        return BcpDto.ScenarioResponse.from(saved);
    }

    @Transactional
    public BcpDto.ScenarioResponse updateScenario(Long id, BcpDto.ScenarioRequest req) {
        BcpScenario s = findScenario(id);
        s.setName(req.getName());
        s.setCategory(req.getCategory());
        s.setDifficulty(parseDifficulty(req.getDifficulty()));
        s.setTargetSystem(req.getTargetSystem());
        s.setRtoMinutes(req.getRtoMinutes());
        s.setRpoMinutes(req.getRpoMinutes());
        s.setSituation(req.getSituation());
        s.setObjective(req.getObjective());
        s.setDescription(req.getDescription());

        // 단계는 전량 교체한다. 실시된 훈련은 단계를 복사해 두므로 과거 기록에는 영향이 없다.
        s.getSteps().clear();
        applySteps(s, req.getSteps());

        auditLogService.log("BCP_SCENARIO_UPDATED", "BCP_SCENARIO", id, "name=" + s.getName());
        return BcpDto.ScenarioResponse.from(scenarioRepo.save(s));
    }

    @Transactional
    public void toggleScenarioActive(Long id) {
        BcpScenario s = findScenario(id);
        s.setActive(!s.isActive());
        scenarioRepo.save(s);
    }

    @Transactional
    public void deleteScenario(Long id) {
        findScenario(id);
        if (exerciseRepo.countByScenarioId(id) > 0) {
            throw new BusinessException("이 시나리오로 실시된 훈련 기록이 있어 삭제할 수 없습니다. 비활성 처리하세요.");
        }
        scenarioRepo.deleteById(id);
        auditLogService.log("BCP_SCENARIO_DELETED", "BCP_SCENARIO", id, "");
    }

    // ── Exercises ─────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<BcpDto.ExerciseResponse> listExercises() {
        return exerciseRepo.findAllWithSteps()
                .stream().map(BcpDto.ExerciseResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public BcpDto.ExerciseDetail getExercise(Long id) {
        BcpExercise e = findExercise(id);
        return BcpDto.ExerciseDetail.builder()
                .exercise(BcpDto.ExerciseResponse.from(e))
                .steps(e.getSteps().stream().map(BcpDto.ExerciseStepResponse::from).toList())
                .build();
    }

    @Transactional
    public BcpDto.ExerciseResponse createExercise(BcpDto.ExerciseRequest req, User actor) {
        BcpScenario scenario = findScenario(req.getScenarioId());
        if (scenario.getSteps().isEmpty()) {
            throw new BusinessException("대응 단계가 없는 시나리오로는 훈련을 진행할 수 없습니다.");
        }

        BcpExercise e = BcpExercise.builder()
                .name(req.getName())
                .scenario(scenario)
                .method(parseMethod(req.getMethod()))
                .plannedAt(req.getPlannedAt())
                .leaderName(req.getLeaderName())
                .participants(req.getParticipants())
                .participantCount(req.getParticipantCount())
                .description(req.getDescription())
                .createdBy(actor)
                .build();

        // 시나리오 단계를 훈련 시점 값으로 복사해 둔다.
        for (BcpScenarioStep ss : scenario.getSteps()) {
            e.getSteps().add(BcpExerciseStep.builder()
                    .exercise(e)
                    .stepOrder(ss.getStepOrder())
                    .title(ss.getTitle())
                    .roleName(ss.getRoleName())
                    .action(ss.getAction())
                    .targetMinutes(ss.getTargetMinutes())
                    .successCriteria(ss.getSuccessCriteria())
                    .build());
        }

        BcpExercise saved = exerciseRepo.save(e);
        auditLogService.log("BCP_EXERCISE_CREATED", "BCP_EXERCISE", saved.getId(),
                "name=" + saved.getName() + ", scenario=" + scenario.getName());
        return BcpDto.ExerciseResponse.from(saved);
    }

    @Transactional
    public BcpDto.ExerciseResponse startExercise(Long id, User actor) {
        BcpExercise e = findExercise(id);
        if (e.getStatus() != BcpExercise.Status.DRAFT) {
            throw new BusinessException("계획 상태의 훈련만 시작할 수 있습니다.");
        }
        e.setStatus(BcpExercise.Status.RUNNING);
        e.setStartedAt(LocalDateTime.now());
        auditLogService.log("BCP_EXERCISE_STARTED", "BCP_EXERCISE", id, "actor=" + actor.getName());
        return BcpDto.ExerciseResponse.from(exerciseRepo.save(e));
    }

    @Transactional
    public BcpDto.ExerciseStepResponse recordStep(Long exerciseId, Long stepId, BcpDto.StepResultRequest req) {
        BcpExercise e = findExercise(exerciseId);
        if (e.getStatus() != BcpExercise.Status.RUNNING) {
            throw new BusinessException("진행중인 훈련만 단계 결과를 기록할 수 있습니다.");
        }
        BcpExerciseStep step = e.getSteps().stream()
                .filter(s -> s.getId().equals(stepId)).findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("BcpExerciseStep", stepId));

        BcpExerciseStep.StepResult result = parseStepResult(req.getResult());
        step.setResult(result);
        step.setActualMinutes(req.getActualMinutes());
        step.setNote(req.getNote());
        step.setCompletedAt(result == BcpExerciseStep.StepResult.PENDING ? null : LocalDateTime.now());

        return BcpDto.ExerciseStepResponse.from(exerciseStepRepo.save(step));
    }

    @Transactional
    public BcpDto.ExerciseResponse completeExercise(Long id, BcpDto.CompleteRequest req, User actor) {
        BcpExercise e = findExercise(id);
        if (e.getStatus() != BcpExercise.Status.RUNNING) {
            throw new BusinessException("진행중인 훈련만 완료 처리할 수 있습니다.");
        }
        boolean pending = e.getSteps().stream()
                .anyMatch(s -> s.getResult() == BcpExerciseStep.StepResult.PENDING);
        if (pending) {
            throw new BusinessException("모든 단계의 수행 결과를 기록해야 완료 처리할 수 있습니다.");
        }

        e.setStatus(BcpExercise.Status.COMPLETED);
        e.setEndedAt(LocalDateTime.now());
        e.setActualRtoMinutes(req.getActualRtoMinutes());
        e.setActualRpoMinutes(req.getActualRpoMinutes());
        e.setSummary(req.getSummary());
        e.setImprovement(req.getImprovement());

        int score = calculateScore(e);
        e.setScore(score);
        e.setResult(judge(score));

        auditLogService.log("BCP_EXERCISE_COMPLETED", "BCP_EXERCISE", id,
                "actor=" + actor.getName() + ", 달성률=" + score + "%, 판정=" + e.getResult());
        return BcpDto.ExerciseResponse.from(exerciseRepo.save(e));
    }

    @Transactional
    public void cancelExercise(Long id) {
        BcpExercise e = findExercise(id);
        if (e.getStatus() == BcpExercise.Status.COMPLETED) {
            throw new BusinessException("완료된 훈련은 취소할 수 없습니다.");
        }
        e.setStatus(BcpExercise.Status.CANCELLED);
        exerciseRepo.save(e);
    }

    @Transactional
    public void deleteExercise(Long id) {
        findExercise(id);
        exerciseRepo.deleteById(id);
        auditLogService.log("BCP_EXERCISE_DELETED", "BCP_EXERCISE", id, "");
    }

    // ── Private helpers ───────────────────────────────────────────────────

    /** 단계 결과를 가중 평균한 달성률 — PASS 1.0, PARTIAL 0.5, FAIL 0 */
    private int calculateScore(BcpExercise e) {
        List<BcpExerciseStep> steps = e.getSteps();
        if (steps.isEmpty()) return 0;
        double earned = steps.stream().mapToDouble(s -> switch (s.getResult()) {
            case PASS -> 1.0;
            case PARTIAL -> 0.5;
            default -> 0.0;
        }).sum();
        return (int) Math.round(earned / steps.size() * 100);
    }

    private BcpExercise.Result judge(int score) {
        if (score >= 80) return BcpExercise.Result.PASS;
        if (score >= 60) return BcpExercise.Result.PARTIAL;
        return BcpExercise.Result.FAIL;
    }

    private void applySteps(BcpScenario s, List<BcpDto.ScenarioStepRequest> reqSteps) {
        if (reqSteps == null) return;
        int order = 1;
        for (BcpDto.ScenarioStepRequest r : reqSteps) {
            s.getSteps().add(BcpScenarioStep.builder()
                    .scenario(s)
                    .stepOrder(r.getStepOrder() != null ? r.getStepOrder() : order)
                    .title(r.getTitle())
                    .roleName(r.getRoleName())
                    .action(r.getAction())
                    .targetMinutes(r.getTargetMinutes())
                    .successCriteria(r.getSuccessCriteria())
                    .build());
            order++;
        }
    }

    private BcpScenario findScenario(Long id) {
        return scenarioRepo.findByIdWithSteps(id)
                .orElseThrow(() -> new ResourceNotFoundException("BcpScenario", id));
    }

    private BcpExercise findExercise(Long id) {
        return exerciseRepo.findByIdWithSteps(id)
                .orElseThrow(() -> new ResourceNotFoundException("BcpExercise", id));
    }

    private BcpScenario.Difficulty parseDifficulty(String d) {
        try { return BcpScenario.Difficulty.valueOf(d); }
        catch (Exception e) { return BcpScenario.Difficulty.MEDIUM; }
    }

    private BcpExercise.Method parseMethod(String m) {
        try { return BcpExercise.Method.valueOf(m); }
        catch (Exception e) { return BcpExercise.Method.TABLETOP; }
    }

    private BcpExerciseStep.StepResult parseStepResult(String r) {
        try { return BcpExerciseStep.StepResult.valueOf(r); }
        catch (Exception e) { throw new BusinessException("수행 결과는 PASS·PARTIAL·FAIL·PENDING 중 하나여야 합니다."); }
    }
}
