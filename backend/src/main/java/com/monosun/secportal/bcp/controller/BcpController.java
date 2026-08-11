package com.monosun.secportal.bcp.controller;

import com.monosun.secportal.auth.entity.User;
import com.monosun.secportal.bcp.dto.BcpDto;
import com.monosun.secportal.bcp.service.BcpService;
import com.monosun.secportal.common.excel.ExportSupport;
import com.monosun.secportal.common.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/bcp")
@RequiredArgsConstructor
public class BcpController {

    private final BcpService service;

    // ── 훈련 시나리오 ──────────────────────────────────────────────────────

    @GetMapping("/scenarios")
    public ApiResponse<List<BcpDto.ScenarioResponse>> listScenarios() {
        return ApiResponse.ok(service.listScenarios());
    }

    @GetMapping("/scenarios/{id}")
    public ApiResponse<BcpDto.ScenarioResponse> getScenario(@PathVariable Long id) {
        return ApiResponse.ok(service.getScenario(id));
    }

    @PostMapping("/scenarios")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ApiResponse<BcpDto.ScenarioResponse> createScenario(
            @Valid @RequestBody BcpDto.ScenarioRequest req,
            @AuthenticationPrincipal User user) {
        return ApiResponse.created(service.createScenario(req, user));
    }

    @PatchMapping("/scenarios/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ApiResponse<BcpDto.ScenarioResponse> updateScenario(
            @PathVariable Long id,
            @Valid @RequestBody BcpDto.ScenarioRequest req) {
        return ApiResponse.ok(service.updateScenario(id, req));
    }

    @PatchMapping("/scenarios/{id}/toggle")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ApiResponse<Void> toggleScenario(@PathVariable Long id) {
        service.toggleScenarioActive(id);
        return ApiResponse.noContent();
    }

    @DeleteMapping("/scenarios/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> deleteScenario(@PathVariable Long id) {
        service.deleteScenario(id);
        return ApiResponse.noContent();
    }

    // ── 훈련 실시 ─────────────────────────────────────────────────────────

    @GetMapping("/exercises")
    public ApiResponse<List<BcpDto.ExerciseResponse>> listExercises() {
        return ApiResponse.ok(service.listExercises());
    }

    @GetMapping("/exercises/{id}")
    public ApiResponse<BcpDto.ExerciseDetail> getExercise(@PathVariable Long id) {
        return ApiResponse.ok(service.getExercise(id));
    }

    /** 재해복구·BCP 훈련 1건의 개요·단계 결과·총평 엑셀 내려받기 */
    @GetMapping("/exercises/{id}/export/excel")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<byte[]> exportExerciseExcel(@PathVariable Long id) {
        byte[] data = service.exportExerciseExcel(id);
        String filename = "BCP훈련결과_" + ExportSupport.safeFileName(service.exerciseName(id)) + ".xlsx";
        return ExportSupport.xlsx(data, filename);
    }

    @PostMapping("/exercises")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ApiResponse<BcpDto.ExerciseResponse> createExercise(
            @Valid @RequestBody BcpDto.ExerciseRequest req,
            @AuthenticationPrincipal User user) {
        return ApiResponse.created(service.createExercise(req, user));
    }

    @PostMapping("/exercises/{id}/start")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ApiResponse<BcpDto.ExerciseResponse> start(
            @PathVariable Long id,
            @AuthenticationPrincipal User user) {
        return ApiResponse.ok(service.startExercise(id, user));
    }

    @PatchMapping("/exercises/{id}/steps/{stepId}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ApiResponse<BcpDto.ExerciseStepResponse> recordStep(
            @PathVariable Long id,
            @PathVariable Long stepId,
            @RequestBody BcpDto.StepResultRequest req) {
        return ApiResponse.ok(service.recordStep(id, stepId, req));
    }

    @PostMapping("/exercises/{id}/complete")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ApiResponse<BcpDto.ExerciseResponse> complete(
            @PathVariable Long id,
            @RequestBody BcpDto.CompleteRequest req,
            @AuthenticationPrincipal User user) {
        return ApiResponse.ok(service.completeExercise(id, req, user));
    }

    @PostMapping("/exercises/{id}/cancel")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ApiResponse<Void> cancel(@PathVariable Long id) {
        service.cancelExercise(id);
        return ApiResponse.noContent();
    }

    @DeleteMapping("/exercises/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> deleteExercise(@PathVariable Long id) {
        service.deleteExercise(id);
        return ApiResponse.noContent();
    }
}
