package com.monosun.secportal.privacy.controller;

import com.monosun.secportal.common.response.ApiResponse;
import com.monosun.secportal.privacy.dto.ContractorCheckDto;
import com.monosun.secportal.privacy.service.ContractorCheckService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/privacy/contractor-checks")
@RequiredArgsConstructor
public class ContractorCheckController {

    private final ContractorCheckService service;

    @GetMapping("/years")
    public ApiResponse<List<Integer>> years() {
        return ApiResponse.ok(service.listYears());
    }

    @GetMapping("/contractor/{contractorId}")
    public ApiResponse<List<ContractorCheckDto.CheckResponse>> listByContractor(
            @PathVariable Long contractorId) {
        return ApiResponse.ok(service.listByContractor(contractorId));
    }

    @GetMapping
    public ApiResponse<List<ContractorCheckDto.CheckResponse>> listByYear(
            @RequestParam int year) {
        return ApiResponse.ok(service.listByYear(year));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ApiResponse<ContractorCheckDto.CheckResponse> create(
            @Valid @RequestBody ContractorCheckDto.CheckRequest request) {
        return ApiResponse.ok(service.create(request));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ApiResponse<ContractorCheckDto.CheckResponse> update(
            @PathVariable Long id,
            @RequestBody ContractorCheckDto.CheckRequest request) {
        return ApiResponse.ok(service.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ApiResponse.noContent();
    }

    @GetMapping("/{id}")
    public ApiResponse<ContractorCheckDto.CheckDetailResponse> getDetail(@PathVariable Long id) {
        return ApiResponse.ok(service.getDetail(id));
    }

    @PutMapping("/{id}/results")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ApiResponse<ContractorCheckDto.ResultResponse> saveResult(
            @PathVariable Long id,
            @Valid @RequestBody ContractorCheckDto.ResultRequest request) {
        return ApiResponse.ok(service.saveResult(id, request));
    }

    @PostMapping("/{id}/results/bulk")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ApiResponse<Void> setAllResults(
            @PathVariable Long id,
            @Valid @RequestBody ContractorCheckDto.BulkResultRequest req) {
        service.setAllResults(id, req.getResult());
        return ApiResponse.noContent();
    }

    @PostMapping("/{id}/sync")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ApiResponse<Void> syncResults(@PathVariable Long id) {
        service.syncResults(id);
        return ApiResponse.noContent();
    }
}
