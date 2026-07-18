package com.monosun.secportal.isms.controller;

import com.monosun.secportal.auth.entity.User;
import com.monosun.secportal.common.response.ApiResponse;
import com.monosun.secportal.isms.dto.IsmsDto;
import com.monosun.secportal.isms.entity.IsmsEvidence;
import com.monosun.secportal.isms.service.IsmsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/isms")
@RequiredArgsConstructor
public class IsmsController {

    private final IsmsService ismsService;

    @GetMapping("/items")
    public ApiResponse<List<IsmsDto.ItemResponse>> listItems(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) String domainCode) {
        return ApiResponse.ok(ismsService.listItems(year, domainCode));
    }

    @GetMapping("/items/{id}")
    public ApiResponse<IsmsDto.ItemResponse> getItem(@PathVariable Long id) {
        return ApiResponse.ok(ismsService.getItem(id));
    }

    /** 항목의 연도별 의견·현재 상태 조회 (없으면 빈 값) */
    @GetMapping("/items/{id}/note")
    public ApiResponse<IsmsDto.ItemNoteResponse> getItemNote(
            @PathVariable Long id,
            @RequestParam(required = false) Integer year) {
        int y = year != null ? year : LocalDate.now().getYear();
        return ApiResponse.ok(ismsService.getItemNote(id, y));
    }

    /** 항목의 연도별 의견·현재 상태 저장 (항목·연도당 1건, upsert) */
    @PutMapping("/items/{id}/note")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ApiResponse<IsmsDto.ItemNoteResponse> saveItemNote(
            @PathVariable Long id,
            @RequestParam(required = false) Integer year,
            @RequestBody IsmsDto.ItemNoteRequest req,
            @AuthenticationPrincipal User user) {
        int y = year != null ? year : LocalDate.now().getYear();
        return ApiResponse.ok(ismsService.saveItemNote(id, y, req, user));
    }

    /** 항목 이행 가이드 저장 (연도 무관) */
    @PatchMapping("/items/{id}/guide")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ApiResponse<IsmsDto.ItemResponse> updateItemGuide(
            @PathVariable Long id,
            @RequestBody IsmsDto.ItemGuideRequest req) {
        return ApiResponse.ok(ismsService.updateItemGuide(id, req));
    }

    @PostMapping("/items/{itemId}/policies/{policyId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ApiResponse<Void> mapPolicy(@PathVariable Long itemId, @PathVariable Long policyId) {
        ismsService.mapPolicy(itemId, policyId);
        return ApiResponse.ok(null);
    }

    @DeleteMapping("/items/{itemId}/policies/{policyId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ApiResponse<Void> unmapPolicy(@PathVariable Long itemId, @PathVariable Long policyId) {
        ismsService.unmapPolicy(itemId, policyId);
        return ApiResponse.noContent();
    }

    @GetMapping("/items/{id}/evidences")
    public ApiResponse<List<IsmsDto.EvidenceResponse>> listEvidences(
            @PathVariable Long id,
            @RequestParam(required = false) Integer year) {
        return ApiResponse.ok(ismsService.listEvidences(id, year));
    }

    @PostMapping(value = "/items/{id}/evidences", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<IsmsDto.EvidenceResponse> createEvidence(
            @PathVariable Long id,
            @Valid @ModelAttribute IsmsDto.EvidenceCreateRequest request,
            @RequestParam(value = "file", required = false) MultipartFile file,
            @AuthenticationPrincipal User user) throws IOException {
        return ApiResponse.created(ismsService.createEvidence(id, request, file, user));
    }

    @PatchMapping(value = "/evidences/{evidenceId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<IsmsDto.EvidenceResponse>> updateEvidence(
            @PathVariable Long evidenceId,
            @ModelAttribute IsmsDto.EvidenceUpdateRequest request,
            @RequestParam(value = "file", required = false) MultipartFile file) {
        try {
            return ResponseEntity.ok(ApiResponse.ok(ismsService.updateEvidence(evidenceId, request, file)));
        } catch (IOException e) {
            return ResponseEntity.badRequest().body(ApiResponse.badRequest("파일 처리 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }

    @DeleteMapping("/evidences/{evidenceId}")
    public ApiResponse<Void> deleteEvidence(@PathVariable Long evidenceId) throws IOException {
        ismsService.deleteEvidence(evidenceId);
        return ApiResponse.noContent();
    }

    @GetMapping("/evidences/search")
    public ApiResponse<List<IsmsDto.EvidenceSearchResult>> searchEvidences(
            @RequestParam Long excludeItemId,
            @RequestParam int year,
            @RequestParam(required = false, defaultValue = "") String keyword) {
        return ApiResponse.ok(ismsService.searchEvidences(excludeItemId, year, keyword));
    }

    @PostMapping("/items/{id}/evidence-refs")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<IsmsDto.EvidenceResponse> createEvidenceRef(
            @PathVariable Long id,
            @RequestBody @Valid IsmsDto.EvidenceRefRequest request,
            @AuthenticationPrincipal User user) {
        return ApiResponse.created(ismsService.createEvidenceRef(id, request, user));
    }

    @GetMapping("/evidences/{evidenceId}/file")
    public ResponseEntity<Resource> downloadFile(@PathVariable Long evidenceId) {
        Resource resource = ismsService.downloadEvidenceFile(evidenceId);
        IsmsEvidence evidence = ismsService.getEvidence(evidenceId);
        String displayName = evidence.getFileName() != null ? evidence.getFileName()
                : (evidence.getSourceEvidence() != null ? evidence.getSourceEvidence().getFileName() : null);
        if (displayName == null) displayName = "attachment";
        String encoded = URLEncoder.encode(displayName, StandardCharsets.UTF_8).replace("+", "%20");
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encoded)
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(resource);
    }

    @DeleteMapping("/evidences/{evidenceId}/file")
    public ApiResponse<IsmsDto.EvidenceResponse> removeFile(@PathVariable Long evidenceId) throws IOException {
        return ApiResponse.ok(ismsService.removeEvidenceFile(evidenceId));
    }

    @GetMapping("/summary")
    public ApiResponse<IsmsDto.SummaryResponse> summary(
            @RequestParam(required = false) Integer year) {
        int targetYear = year != null ? year : LocalDate.now().getYear();
        return ApiResponse.ok(ismsService.summary(targetYear));
    }

    @GetMapping("/export/csv")
    public ResponseEntity<byte[]> exportCsv(@RequestParam(required = false) Integer year) {
        int targetYear = year != null ? year : LocalDate.now().getYear();
        byte[] csv = ismsService.exportCsv(targetYear);
        String filename = "isms_evidences_" + targetYear + ".csv";
        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename*=UTF-8''" +
                        URLEncoder.encode(filename, StandardCharsets.UTF_8))
                .contentType(MediaType.parseMediaType("text/csv;charset=UTF-8"))
                .body(csv);
    }

    @GetMapping("/import/template")
    public ResponseEntity<byte[]> importTemplate() throws IOException {
        byte[] xlsx = ismsService.getImportTemplate();
        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename*=UTF-8''isms_import_template.xlsx")
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(xlsx);
    }

    @PostMapping("/import")
    public ApiResponse<IsmsDto.BulkImportResult> bulkImport(
            @RequestParam int year,
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal User user) throws IOException {
        return ApiResponse.ok(ismsService.bulkImport(year, file, user));
    }
}
