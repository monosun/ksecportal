package com.monosun.secportal.metrics.controller;

import com.monosun.secportal.common.response.ApiResponse;
import com.monosun.secportal.metrics.dto.MetricsDto;
import com.monosun.secportal.metrics.service.MetricsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/metrics")
@RequiredArgsConstructor
public class MetricsController {

    private final MetricsService metricsService;

    @GetMapping("/summary")
    public ApiResponse<MetricsDto.Summary> summary() {
        return ApiResponse.ok(metricsService.summary());
    }
}
