package com.monosun.secportal.nvd.controller;

import com.monosun.secportal.common.response.ApiResponse;
import com.monosun.secportal.nvd.dto.NvdCveDto;
import com.monosun.secportal.nvd.service.NvdService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/nvd")
@RequiredArgsConstructor
public class NvdController {

    private final NvdService nvdService;

    @GetMapping("/cve/{cveId}")
    public ApiResponse<NvdCveDto> lookup(@PathVariable String cveId) {
        return ApiResponse.ok(nvdService.lookup(cveId));
    }
}
