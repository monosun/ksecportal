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
import com.monosun.secportal.common.excel.ExcelWriter;
import com.monosun.secportal.common.excel.ExportSupport;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Sheet;
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

    // ── Excel export ──────────────────────────────────────────────────────

    /** 재해복구·BCP 훈련 1건의 개요·단계별 수행 결과·총평을 엑셀로 만든다. */
    @Transactional(readOnly = true)
    public byte[] exportExerciseExcel(Long id) {
        BcpExercise e = findExercise(id);
        BcpScenario s = e.getScenario();
        List<BcpExerciseStep> steps = e.getSteps();
        long passed = countStep(steps, BcpExerciseStep.StepResult.PASS);
        long partial = countStep(steps, BcpExerciseStep.StepResult.PARTIAL);
        long failed = countStep(steps, BcpExerciseStep.StepResult.FAIL);
        long pending = countStep(steps, BcpExerciseStep.StepResult.PENDING);

        try (ExcelWriter xw = new ExcelWriter()) {
            Sheet sheet = xw.sheet("BCP 훈련 결과");
            int r = xw.title(sheet, 0, "재해복구·BCP 훈련 결과 — " + e.getName(), 8);
            r++;
            r = xw.meta(sheet, r, new String[][]{
                    {"훈련명", e.getName()},
                    {"시나리오", s != null ? s.getName() : "-"},
                    {"재해 유형", s != null ? s.getCategory() : "-"},
                    {"대상 시스템·업무", s != null ? s.getTargetSystem() : "-"},
                    {"훈련 방식", methodLabel(e.getMethod())},
                    {"상태", statusLabel(e.getStatus())},
                    {"총괄자", e.getLeaderName()},
                    {"참가자", e.getParticipants()},
                    {"참가 인원", e.getParticipantCount() != null ? String.valueOf(e.getParticipantCount()) : "-"},
                    {"계획 일시", ExportSupport.dt(e.getPlannedAt())},
                    {"시작 일시", ExportSupport.dt(e.getStartedAt())},
                    {"종료 일시", ExportSupport.dt(e.getEndedAt())},
                    {"목표 RTO(분)", s != null && s.getRtoMinutes() != null ? String.valueOf(s.getRtoMinutes()) : "-"},
                    {"실제 RTO(분)", e.getActualRtoMinutes() != null ? String.valueOf(e.getActualRtoMinutes()) : "-"},
                    {"목표 RPO(분)", s != null && s.getRpoMinutes() != null ? String.valueOf(s.getRpoMinutes()) : "-"},
                    {"실제 RPO(분)", e.getActualRpoMinutes() != null ? String.valueOf(e.getActualRpoMinutes()) : "-"},
                    {"달성률", e.getScore() != null ? e.getScore() + "%" : "-"},
                    {"판정", resultLabel(e.getResult())},
                    {"단계 수행", "총 " + steps.size() + " · 성공 " + passed + " · 부분 " + partial
                            + " · 실패 " + failed + " · 미수행 " + pending},
                    {"내려받은 시각", ExportSupport.now()},
            });
            r++;

            r = xw.header(sheet, r, new String[]{
                    "No", "단계명", "담당 역할", "수행 절차", "성공 판정 기준",
                    "목표(분)", "실제(분)", "수행 결과", "비고", "완료 시각"});
            for (BcpExerciseStep step : steps) {
                r = xw.row(sheet, r, new Object[]{
                        step.getStepOrder(),
                        step.getTitle(),
                        step.getRoleName(),
                        step.getAction(),
                        step.getSuccessCriteria(),
                        step.getTargetMinutes() != null ? step.getTargetMinutes() : "-",
                        step.getActualMinutes() != null ? step.getActualMinutes() : "-",
                        stepResultLabel(step.getResult()),
                        step.getNote(),
                        ExportSupport.dt(step.getCompletedAt()),
                }, 0, 5, 6, 7, 9);
            }
            xw.widths(sheet, 6, 26, 14, 40, 30, 10, 10, 12, 30, 20);

            r++;
            r = xw.textBlock(sheet, r, "상황 설정", s != null ? s.getSituation() : "", 9, 60);
            r++;
            r = xw.textBlock(sheet, r, "훈련 총평", e.getSummary(), 9, 60);
            r++;
            xw.textBlock(sheet, r, "도출된 개선사항", e.getImprovement(), 9, 60);

            return xw.toBytes();
        }
    }

    /** 파일명에 쓸 훈련명 */
    @Transactional(readOnly = true)
    public String exerciseName(Long id) {
        return exerciseRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("BcpExercise", id))
                .getName();
    }

    private static long countStep(List<BcpExerciseStep> steps, BcpExerciseStep.StepResult r) {
        return steps.stream().filter(s -> s.getResult() == r).count();
    }

    private static String methodLabel(BcpExercise.Method m) {
        if (m == null) return "-";
        return switch (m) {
            case TABLETOP -> "도상훈련";
            case SIMULATION -> "시뮬레이션";
            case FAILOVER -> "실제 전환";
        };
    }

    private static String statusLabel(BcpExercise.Status s) {
        if (s == null) return "-";
        return switch (s) {
            case DRAFT -> "계획";
            case RUNNING -> "진행중";
            case COMPLETED -> "완료";
            case CANCELLED -> "취소";
        };
    }

    private static String resultLabel(BcpExercise.Result r) {
        if (r == null) return "-";
        return switch (r) {
            case PASS -> "적합";
            case PARTIAL -> "보완필요";
            case FAIL -> "부적합";
        };
    }

    private static String stepResultLabel(BcpExerciseStep.StepResult r) {
        if (r == null) return "-";
        return switch (r) {
            case PASS -> "성공";
            case PARTIAL -> "부분";
            case FAIL -> "실패";
            case PENDING -> "미수행";
        };
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
