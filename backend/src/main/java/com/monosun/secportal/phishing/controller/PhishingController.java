package com.monosun.secportal.phishing.controller;

import com.monosun.secportal.auth.entity.User;
import com.monosun.secportal.common.response.ApiResponse;
import com.monosun.secportal.phishing.dto.PhishingDto;
import com.monosun.secportal.phishing.service.PhishingService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.Base64;
import java.util.List;

@RestController
@RequestMapping("/phishing")
@RequiredArgsConstructor
public class PhishingController {

    private final PhishingService service;

    // ── Templates ─────────────────────────────────────────────────────────

    @GetMapping("/templates")
    public ApiResponse<List<PhishingDto.TemplateResponse>> listTemplates() {
        return ApiResponse.ok(service.listTemplates());
    }

    @GetMapping("/templates/{id}")
    public ApiResponse<PhishingDto.TemplateResponse> getTemplate(@PathVariable Long id) {
        return ApiResponse.ok(service.getTemplate(id));
    }

    @PostMapping("/templates")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ApiResponse<PhishingDto.TemplateResponse> createTemplate(
            @Valid @RequestBody PhishingDto.TemplateRequest req,
            @AuthenticationPrincipal User user) {
        return ApiResponse.created(service.createTemplate(req, user));
    }

    @PatchMapping("/templates/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ApiResponse<PhishingDto.TemplateResponse> updateTemplate(
            @PathVariable Long id,
            @Valid @RequestBody PhishingDto.TemplateRequest req) {
        return ApiResponse.ok(service.updateTemplate(id, req));
    }

    @DeleteMapping("/templates/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> deleteTemplate(@PathVariable Long id) {
        service.deleteTemplate(id);
        return ApiResponse.noContent();
    }

    // ── Targets ───────────────────────────────────────────────────────────

    @GetMapping("/targets")
    public ApiResponse<List<PhishingDto.TargetResponse>> listTargets() {
        return ApiResponse.ok(service.listTargets());
    }

    @PostMapping("/targets")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ApiResponse<PhishingDto.TargetResponse> createTarget(
            @Valid @RequestBody PhishingDto.TargetRequest req) {
        return ApiResponse.created(service.createTarget(req));
    }

    @PatchMapping("/targets/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ApiResponse<PhishingDto.TargetResponse> updateTarget(
            @PathVariable Long id,
            @Valid @RequestBody PhishingDto.TargetRequest req) {
        return ApiResponse.ok(service.updateTarget(id, req));
    }

    @PatchMapping("/targets/{id}/toggle")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ApiResponse<Void> toggleTarget(@PathVariable Long id) {
        service.toggleTargetActive(id);
        return ApiResponse.noContent();
    }

    @DeleteMapping("/targets/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> deleteTarget(@PathVariable Long id) {
        service.deleteTarget(id);
        return ApiResponse.noContent();
    }

    // ── Campaigns ─────────────────────────────────────────────────────────

    @GetMapping("/campaigns")
    public ApiResponse<List<PhishingDto.CampaignResponse>> listCampaigns() {
        return ApiResponse.ok(service.listCampaigns());
    }

    @GetMapping("/campaigns/{id}")
    public ApiResponse<PhishingDto.CampaignDetail> getCampaign(@PathVariable Long id) {
        return ApiResponse.ok(service.getCampaign(id));
    }

    @PostMapping("/campaigns")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ApiResponse<PhishingDto.CampaignResponse> createCampaign(
            @Valid @RequestBody PhishingDto.CampaignRequest req,
            @AuthenticationPrincipal User user) {
        return ApiResponse.created(service.createCampaign(req, user));
    }

    @PostMapping("/campaigns/{id}/launch")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ApiResponse<PhishingDto.CampaignResponse> launch(
            @PathVariable Long id,
            @AuthenticationPrincipal User user) {
        return ApiResponse.ok(service.launchCampaign(id, user));
    }

    @PostMapping("/campaigns/{id}/complete")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ApiResponse<PhishingDto.CampaignResponse> complete(@PathVariable Long id) {
        return ApiResponse.ok(service.completeCampaign(id));
    }

    @PostMapping("/campaigns/{id}/cancel")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ApiResponse<Void> cancel(@PathVariable Long id) {
        service.cancelCampaign(id);
        return ApiResponse.noContent();
    }

    @DeleteMapping("/campaigns/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> deleteCampaign(@PathVariable Long id) {
        service.deleteCampaign(id);
        return ApiResponse.noContent();
    }

    // ── Send logs (발송 처리 결과) ─────────────────────────────────────────

    @GetMapping("/send-logs")
    public ApiResponse<List<PhishingDto.SendLogEntry>> listSendLogs() {
        return ApiResponse.ok(service.listSendLogs());
    }

    // ── Tracking (public — no auth required) ──────────────────────────────

    @GetMapping("/track/{token}/open")
    public void trackOpen(@PathVariable String token, HttpServletResponse res) throws IOException {
        service.trackOpen(token);
        // Return a 1×1 transparent GIF
        byte[] gif = Base64.getDecoder().decode(
            "R0lGODlhAQABAIAAAP///wAAACH5BAEAAAAALAAAAAABAAEAAAICRAEAOw==");
        res.setContentType("image/gif");
        res.getOutputStream().write(gif);
    }

    @GetMapping("/track/{token}/click")
    public void trackClick(@PathVariable String token, HttpServletResponse res) throws IOException {
        service.trackClick(token);
        // Redirect to a phishing awareness page
        res.sendRedirect("/phishing-awareness.html");
    }
}
