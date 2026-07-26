package com.monosun.secportal.relatedsite.controller;

import com.monosun.secportal.common.response.ApiResponse;
import com.monosun.secportal.relatedsite.dto.RelatedSiteDto;
import com.monosun.secportal.relatedsite.service.RelatedSiteService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 관련 사이트 — 조회는 로그인 사용자(보안 가이드 및 자료 &gt; 관련 사이트),
 * 등록·수정·삭제·새로고침은 ADMIN·MANAGER.
 */
@RestController
@RequestMapping("/related-sites")
@RequiredArgsConstructor
public class RelatedSiteController {

    private final RelatedSiteService service;

    @GetMapping
    public ApiResponse<List<RelatedSiteDto.SiteResponse>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String category,
            @RequestParam(required = false, defaultValue = "true") Boolean activeOnly) {
        return ApiResponse.ok(service.list(keyword, category, activeOnly));
    }

    @GetMapping("/{id}")
    public ApiResponse<RelatedSiteDto.SiteResponse> get(@PathVariable Long id) {
        return ApiResponse.ok(service.get(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ApiResponse<RelatedSiteDto.SiteResponse> create(@RequestBody RelatedSiteDto.SiteRequest req) {
        return ApiResponse.created(service.create(req));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ApiResponse<RelatedSiteDto.SiteResponse> update(@PathVariable Long id,
                                                           @RequestBody RelatedSiteDto.SiteRequest req) {
        return ApiResponse.ok(service.update(id, req));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ApiResponse.noContent();
    }

    /** 사이트 한 곳의 내용을 지금 다시 가져온다 */
    @PostMapping("/{id}/refresh")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ApiResponse<RelatedSiteDto.SiteResponse> refresh(@PathVariable Long id) {
        return ApiResponse.ok(service.refresh(id));
    }

    /** 사용 중인 사이트 전체를 다시 가져온다 (사이트 수만큼 외부 접속이 일어나므로 다소 걸릴 수 있음) */
    @PostMapping("/refresh")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ApiResponse<RelatedSiteDto.RefreshResult> refreshAll() {
        return ApiResponse.ok(service.refreshAll());
    }
}
