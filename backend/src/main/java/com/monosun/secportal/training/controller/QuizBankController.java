package com.monosun.secportal.training.controller;

import com.monosun.secportal.common.response.ApiResponse;
import com.monosun.secportal.training.dto.QuizBankDto;
import com.monosun.secportal.training.service.QuizBankService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/quiz-bank")
@RequiredArgsConstructor
public class QuizBankController {

    private final QuizBankService service;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ApiResponse<Page<QuizBankDto.Response>> list(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String difficulty,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(service.list(category, difficulty, keyword, page, size));
    }

    @GetMapping("/categories")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ApiResponse<List<String>> categories() {
        return ApiResponse.ok(service.categories());
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ApiResponse<QuizBankDto.Response> create(@Valid @RequestBody QuizBankDto.Request req) {
        return ApiResponse.ok(service.create(req));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ApiResponse<QuizBankDto.Response> update(
            @PathVariable Long id, @Valid @RequestBody QuizBankDto.Request req) {
        return ApiResponse.ok(service.update(id, req));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ApiResponse.noContent();
    }

    @GetMapping("/bulk/template")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<byte[]> downloadTemplate() throws IOException {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"quiz-bank-template.xlsx\"")
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(service.generateTemplate());
    }

    @PostMapping("/bulk")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ApiResponse<QuizBankDto.BulkResult> bulkUpload(@RequestParam("file") MultipartFile file) throws IOException {
        return ApiResponse.ok(service.upload(file));
    }
}
