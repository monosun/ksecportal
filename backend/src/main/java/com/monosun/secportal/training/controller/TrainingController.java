package com.monosun.secportal.training.controller;

import com.monosun.secportal.auth.entity.User;
import com.monosun.secportal.common.excel.ExportSupport;
import com.monosun.secportal.common.response.ApiResponse;
import com.monosun.secportal.training.dto.TrainingDto;
import com.monosun.secportal.training.entity.TrainingCourse;
import com.monosun.secportal.training.service.TrainingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/training")
@RequiredArgsConstructor
public class TrainingController {

    private final TrainingService trainingService;

    @GetMapping("/courses")
    public ApiResponse<Page<TrainingDto.CourseSummary>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Boolean mandatory,
            @RequestParam(required = false) TrainingCourse.ContentType contentType,
            @PageableDefault(size = 20) Pageable pageable,
            @AuthenticationPrincipal User user) {
        return ApiResponse.ok(trainingService.list(keyword, mandatory, contentType, user.getId(), pageable));
    }

    @GetMapping("/results")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ApiResponse<java.util.List<TrainingDto.CourseResultSummary>> results() {
        return ApiResponse.ok(trainingService.resultSummary());
    }

    @GetMapping("/results/completions")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ApiResponse<java.util.List<TrainingDto.CompletionRow>> resultCompletions(
            @RequestParam(required = false) Long courseId) {
        return ApiResponse.ok(trainingService.resultCompletions(courseId));
    }

    /** 교육 1건의 개요·이수 이력·퀴즈 문항 엑셀 내려받기 */
    @GetMapping("/results/courses/{id}/export/excel")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<byte[]> exportCourseExcel(@PathVariable Long id) {
        byte[] data = trainingService.exportCourseExcel(id);
        String filename = "교육결과_" + ExportSupport.safeFileName(trainingService.courseTitle(id)) + ".xlsx";
        return ExportSupport.xlsx(data, filename);
    }

    @GetMapping("/courses/{id}")
    public ApiResponse<TrainingDto.CourseResponse> get(
            @PathVariable Long id,
            @AuthenticationPrincipal User user) {
        return ApiResponse.ok(trainingService.get(id, user.getId()));
    }

    @PostMapping("/courses")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ApiResponse<TrainingDto.CourseResponse> create(
            @Valid @RequestBody TrainingDto.CreateRequest request,
            @AuthenticationPrincipal User user) {
        return ApiResponse.created(trainingService.create(request, user.getId()));
    }

    @PostMapping("/courses/{id}/submit")
    public ApiResponse<TrainingDto.QuizResult> submitQuiz(
            @PathVariable Long id,
            @Valid @RequestBody TrainingDto.SubmitQuizRequest request,
            @AuthenticationPrincipal User user) {
        return ApiResponse.ok(trainingService.submitQuiz(id, user.getId(), request.getAnswers()));
    }

    @PatchMapping("/courses/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ApiResponse<TrainingDto.CourseResponse> update(
            @PathVariable Long id,
            @RequestBody TrainingDto.UpdateRequest request) {
        return ApiResponse.ok(trainingService.update(id, request));
    }

    @DeleteMapping("/courses/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        trainingService.delete(id);
        return ApiResponse.noContent();
    }
}
