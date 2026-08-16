package com.monosun.secportal.policy.controller;

import com.monosun.secportal.asset.dto.AssetBulkUploadResult;
import com.monosun.secportal.auth.entity.User;
import com.monosun.secportal.common.response.ApiResponse;
import com.monosun.secportal.policy.dto.PolicyArticleDto;
import com.monosun.secportal.policy.dto.PolicyDto;
import com.monosun.secportal.policy.entity.Policy;
import com.monosun.secportal.policy.service.PolicyArticleService;
import com.monosun.secportal.policy.service.PolicyBulkService;
import com.monosun.secportal.policy.service.PolicyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/policies")
@RequiredArgsConstructor
public class PolicyController {

    private final PolicyService policyService;
    private final PolicyBulkService policyBulkService;
    private final PolicyArticleService policyArticleService;

    @GetMapping
    public ApiResponse<Page<PolicyDto.Summary>> list(
            @RequestParam(required = false) Policy.Status status,
            @RequestParam(required = false) Policy.Category category,
            @RequestParam(required = false) String keyword,
            @PageableDefault(size = 20) Pageable pageable) {
        return ApiResponse.ok(policyService.list(status, category, keyword, pageable));
    }

    // ── 조문(條) 세분화 검색 ──────────────────────────────────────────────────
    // "/articles" 는 "/{id}" 보다 먼저 선언해 경로 충돌을 피한다.

    /**
     * 지침 &gt; 장 &gt; 조 계층 검색.
     *
     * @param scope 검색 범위 — ALL(전체) · TITLE(조 제목) · CONTENT(본문) · GUIDELINE(지침) · CHAPTER(장) · ARTICLE(조 표기)
     */
    @GetMapping("/articles")
    public ApiResponse<Page<PolicyArticleDto.Response>> searchArticles(
            @RequestParam(required = false) String guideline,
            @RequestParam(required = false) Integer chapterNo,
            @RequestParam(required = false) Long policyId,
            @RequestParam(required = false) Integer articleNo,
            @RequestParam(required = false) Policy.Status status,
            @RequestParam(required = false) Policy.Category category,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) PolicyArticleService.Scope scope,
            @PageableDefault(size = 20) Pageable pageable) {
        return ApiResponse.ok(policyArticleService.search(
                guideline, chapterNo, policyId, articleNo, status, category, keyword, scope, pageable));
    }

    /** 검색 필터 드롭다운용 지침 · 장 목록 */
    @GetMapping("/articles/facets")
    public ApiResponse<PolicyArticleDto.Facets> articleFacets() {
        return ApiResponse.ok(policyArticleService.facets());
    }

    /** 전체 정책 본문을 다시 파싱해 조를 재등록한다. */
    @PostMapping("/articles/resync")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ApiResponse<Integer> resyncArticles() {
        return ApiResponse.ok(policyArticleService.syncAll());
    }

    /** 특정 장에 속한 조 목록 */
    @GetMapping("/{id}/articles")
    public ApiResponse<List<PolicyArticleDto.Response>> articlesOf(@PathVariable Long id) {
        return ApiResponse.ok(policyArticleService.listByPolicy(id));
    }

    @GetMapping("/{id}")
    public ApiResponse<PolicyDto.Response> get(
            @PathVariable Long id,
            @AuthenticationPrincipal User user) {
        return ApiResponse.ok(policyService.get(id, user.getId()));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ApiResponse<PolicyDto.Response> create(
            @Valid @RequestBody PolicyDto.CreateRequest request,
            @AuthenticationPrincipal User user) {
        return ApiResponse.created(policyService.create(request, user.getId()));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ApiResponse<PolicyDto.Response> update(
            @PathVariable Long id,
            @RequestBody PolicyDto.UpdateRequest request) {
        return ApiResponse.ok(policyService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        policyService.delete(id);
        return ApiResponse.noContent();
    }

    /** 목록에서 선택한 정책 일괄 삭제 — 삭제 건수를 반환 */
    @DeleteMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Integer> deleteBulk(@RequestParam List<Long> ids) {
        return ApiResponse.ok(policyService.deleteAll(ids));
    }

    @PostMapping("/{id}/acknowledge")
    public ApiResponse<Void> acknowledge(
            @PathVariable Long id,
            @AuthenticationPrincipal User user) {
        policyService.acknowledge(id, user.getId());
        return ApiResponse.ok("Policy acknowledged", null);
    }

    @GetMapping("/bulk/template")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<byte[]> downloadTemplate() throws IOException {
        byte[] data = policyBulkService.generateTemplate();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"policy-upload-template.xlsx\"")
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(data);
    }

    @PostMapping("/bulk")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ApiResponse<AssetBulkUploadResult> bulkUpload(
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal User user) throws IOException {
        return ApiResponse.ok(policyBulkService.upload(file, user));
    }
}
