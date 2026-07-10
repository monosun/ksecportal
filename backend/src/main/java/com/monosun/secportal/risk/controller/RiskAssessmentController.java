package com.monosun.secportal.risk.controller;

import com.monosun.secportal.common.response.ApiResponse;
import com.monosun.secportal.risk.dto.RiskAssessmentDto;
import com.monosun.secportal.risk.service.RiskAssessmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/risk")
@RequiredArgsConstructor
public class RiskAssessmentController {

    private final RiskAssessmentService riskAssessmentService;

    @GetMapping("/years")
    public ApiResponse<List<Integer>> listYears() {
        return ApiResponse.ok(riskAssessmentService.listYears());
    }

    @GetMapping("/rounds")
    public ApiResponse<List<RiskAssessmentDto.RoundResponse>> listRounds(@RequestParam int year) {
        return ApiResponse.ok(riskAssessmentService.listRounds(year));
    }

    @PostMapping("/rounds")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ApiResponse<RiskAssessmentDto.RoundResponse> createRound(
            @Valid @RequestBody RiskAssessmentDto.RoundRequest request) {
        return ApiResponse.created(riskAssessmentService.createRound(request));
    }

    @PatchMapping("/rounds/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ApiResponse<RiskAssessmentDto.RoundResponse> updateRound(
            @PathVariable Long id,
            @RequestBody RiskAssessmentDto.RoundRequest request) {
        return ApiResponse.ok(riskAssessmentService.updateRound(id, request));
    }

    @DeleteMapping("/rounds/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ApiResponse<Void> deleteRound(@PathVariable Long id) {
        riskAssessmentService.deleteRound(id);
        return ApiResponse.noContent();
    }

    @GetMapping("/rounds/{roundId}/assessments")
    public ApiResponse<List<RiskAssessmentDto.AssessmentResponse>> listAssessments(
            @PathVariable Long roundId,
            @RequestParam(required = false) String assetName,
            @RequestParam(required = false) String assetType,
            @RequestParam(required = false) String threatName,
            @RequestParam(required = false) String threatType,
            @RequestParam(required = false) String riskGrade) {
        return ApiResponse.ok(riskAssessmentService.listAssessments(
                roundId, assetName, assetType, threatName, threatType, riskGrade));
    }

    @GetMapping("/rounds/{roundId}/export/excel")
    public ResponseEntity<byte[]> exportExcel(@PathVariable Long roundId) {
        byte[] data = riskAssessmentService.exportExcel(roundId);
        String filename = URLEncoder.encode("위험평가_" + roundId + ".xlsx", StandardCharsets.UTF_8)
                .replace("+", "%20");
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + filename);
        return ResponseEntity.ok().headers(headers).body(data);
    }

    @PostMapping("/rounds/{roundId}/auto-populate")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ApiResponse<Integer> autoPopulate(@PathVariable Long roundId) {
        int count = riskAssessmentService.autoPopulateFromAssets(roundId);
        return ApiResponse.ok("자산 " + count + "개가 추가되었습니다.", count);
    }

    @PostMapping("/assessments")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ApiResponse<RiskAssessmentDto.AssessmentResponse> createAssessment(
            @Valid @RequestBody RiskAssessmentDto.AssessmentRequest request) {
        return ApiResponse.created(riskAssessmentService.createAssessment(request));
    }

    @PatchMapping("/assessments/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ApiResponse<RiskAssessmentDto.AssessmentResponse> updateAssessment(
            @PathVariable Long id,
            @RequestBody RiskAssessmentDto.AssessmentRequest request) {
        return ApiResponse.ok(riskAssessmentService.updateAssessment(id, request));
    }

    @DeleteMapping("/assessments/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ApiResponse<Void> deleteAssessment(@PathVariable Long id) {
        riskAssessmentService.deleteAssessment(id);
        return ApiResponse.noContent();
    }

    @PatchMapping("/assessments/bulk-treatment")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ApiResponse<Integer> bulkUpdateTreatment(@RequestBody RiskAssessmentDto.BulkTreatmentRequest request) {
        int count = riskAssessmentService.bulkUpdateTreatment(request.getIds(), request.getTreatment());
        return ApiResponse.ok(count + "개 항목이 업데이트되었습니다.", count);
    }

    // ── 위험 처리 계획 ───────────────────────────────────────────────────────

    @GetMapping("/rounds/{roundId}/treatment-plans")
    public ApiResponse<List<RiskAssessmentDto.AssessmentResponse>> listTreatmentPlans(@PathVariable Long roundId) {
        return ApiResponse.ok(riskAssessmentService.listTreatmentPlans(roundId));
    }

    @PatchMapping("/assessments/{id}/treatment-plan")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ApiResponse<RiskAssessmentDto.AssessmentResponse> updateTreatmentPlan(
            @PathVariable Long id,
            @RequestBody RiskAssessmentDto.TreatmentPlanRequest request) {
        return ApiResponse.ok(riskAssessmentService.updateTreatmentPlan(id, request));
    }
}
