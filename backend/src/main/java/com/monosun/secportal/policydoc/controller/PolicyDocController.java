package com.monosun.secportal.policydoc.controller;

import com.monosun.secportal.auth.entity.User;
import com.monosun.secportal.common.response.ApiResponse;
import com.monosun.secportal.policydoc.dto.PolicyDocDto;
import com.monosun.secportal.policydoc.service.PolicyDocService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * 정보보호관리체계 > 보안정책관리 — 정책 문서(규정·지침·가이드·절차서·매뉴얼)의 버전·개정사항을 파일 단위로 관리한다.
 * 조회·쓰기·삭제 모두 RBAC 메뉴 권한(policy_management)으로 서버에서 검사한다.
 */
@RestController
@RequestMapping("/policy-docs")
@RequiredArgsConstructor
public class PolicyDocController {

    private final PolicyDocService service;

    @GetMapping
    @PreAuthorize("@menuAuth.canRead('policy_management')")
    public ApiResponse<Page<PolicyDocDto.Response>> list(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(service.list(category, status, keyword, page, size));
    }

    @GetMapping("/summary")
    @PreAuthorize("@menuAuth.canRead('policy_management')")
    public ApiResponse<Map<String, Long>> summary(@RequestParam(required = false) String status) {
        return ApiResponse.ok(service.summary(status));
    }

    /** 문서번호를 비웠을 때 부여될 번호 미리보기 (등록 화면 안내용) */
    @GetMapping("/next-doc-no")
    @PreAuthorize("@menuAuth.canWrite('policy_management')")
    public ApiResponse<Map<String, String>> nextDocNo(@RequestParam String category) {
        // 문자열 하나를 ok(...) 로 넘기면 ok(String message) 오버로드로 해석되므로 객체로 감싼다
        return ApiResponse.ok(Map.of("docNo", service.nextDocNo(category)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("@menuAuth.canRead('policy_management')")
    public ApiResponse<PolicyDocDto.Response> get(@PathVariable Long id) {
        return ApiResponse.ok(service.get(id));
    }

    @PostMapping
    @PreAuthorize("@menuAuth.canWrite('policy_management')")
    public ApiResponse<PolicyDocDto.Response> create(
            @RequestPart("data") PolicyDocDto.CreateRequest req,
            @RequestPart(value = "file", required = false) MultipartFile file,
            @AuthenticationPrincipal User user) throws IOException {
        return ApiResponse.created(service.create(req, file, user));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@menuAuth.canWrite('policy_management')")
    public ApiResponse<PolicyDocDto.Response> update(@PathVariable Long id, @RequestBody PolicyDocDto.DocRequest req) {
        return ApiResponse.ok(service.update(id, req));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@menuAuth.canDelete('policy_management')")
    public ApiResponse<Void> delete(@PathVariable Long id) throws IOException {
        service.delete(id);
        return ApiResponse.noContent();
    }

    // ── 버전(개정본) ──────────────────────────────────────────────────────

    @PostMapping("/{id}/revisions")
    @PreAuthorize("@menuAuth.canWrite('policy_management')")
    public ApiResponse<PolicyDocDto.Response> addRevision(
            @PathVariable Long id,
            @RequestPart("data") PolicyDocDto.RevisionRequest req,
            @RequestPart(value = "file", required = false) MultipartFile file,
            @AuthenticationPrincipal User user) throws IOException {
        return ApiResponse.created(service.addRevision(id, req, file, user));
    }

    @PutMapping("/revisions/{revisionId}")
    @PreAuthorize("@menuAuth.canWrite('policy_management')")
    public ApiResponse<PolicyDocDto.Response> updateRevision(
            @PathVariable Long revisionId, @RequestBody PolicyDocDto.RevisionRequest req) {
        return ApiResponse.ok(service.updateRevision(revisionId, req));
    }

    @PostMapping("/revisions/{revisionId}/file")
    @PreAuthorize("@menuAuth.canWrite('policy_management')")
    public ApiResponse<PolicyDocDto.Response> replaceFile(
            @PathVariable Long revisionId, @RequestPart("file") MultipartFile file) throws IOException {
        return ApiResponse.ok(service.replaceRevisionFile(revisionId, file));
    }

    @DeleteMapping("/revisions/{revisionId}")
    @PreAuthorize("@menuAuth.canDelete('policy_management')")
    public ApiResponse<PolicyDocDto.Response> deleteRevision(@PathVariable Long revisionId) throws IOException {
        return ApiResponse.ok(service.deleteRevision(revisionId));
    }

    @GetMapping("/revisions/{revisionId}/download")
    @PreAuthorize("@menuAuth.canRead('policy_management')")
    public ResponseEntity<Resource> download(@PathVariable Long revisionId) {
        String name = service.fileName(revisionId);
        Resource resource = service.download(revisionId);
        String encoded = URLEncoder.encode(name != null ? name : "document", StandardCharsets.UTF_8).replace("+", "%20");
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encoded)
                .body(resource);
    }

    /** 뷰어용 — DOC·PPT 등은 서버에서 PDF 로 변환해 inline 으로 내려준다 */
    @GetMapping("/revisions/{revisionId}/preview")
    @PreAuthorize("@menuAuth.canRead('policy_management')")
    public ResponseEntity<Resource> preview(@PathVariable Long revisionId) {
        Resource pdf = service.previewPdf(revisionId);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline")
                .header(HttpHeaders.CACHE_CONTROL, "private, max-age=300")
                .body(pdf);
    }
}
