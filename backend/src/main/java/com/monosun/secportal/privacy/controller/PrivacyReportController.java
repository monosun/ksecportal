package com.monosun.secportal.privacy.controller;

import com.monosun.secportal.common.response.ApiResponse;
import com.monosun.secportal.privacy.dto.PrivacyReportDto;
import com.monosun.secportal.privacy.service.PrivacyReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/privacy/report")
@RequiredArgsConstructor
public class PrivacyReportController {

    private final PrivacyReportService service;

    /** 개인정보 현황보고서 — 전 영역 집계 */
    @GetMapping
    public ApiResponse<PrivacyReportDto.Summary> summary() {
        return ApiResponse.ok(service.generate());
    }
}
