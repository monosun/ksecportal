package com.monosun.secportal.monthlycheck.controller;

import com.monosun.secportal.common.response.ApiResponse;
import com.monosun.secportal.monthlycheck.dto.MonthlyCheckDefaultDto;
import com.monosun.secportal.monthlycheck.service.MonthlyCheckDefaultService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/monthly-checks/default-items")
@RequiredArgsConstructor
public class MonthlyCheckDefaultController {

    private final MonthlyCheckDefaultService defaultService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<List<MonthlyCheckDefaultDto.Response>> list() {
        return ApiResponse.ok(defaultService.list());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<MonthlyCheckDefaultDto.Response> create(
            @Valid @RequestBody MonthlyCheckDefaultDto.Request request) {
        return ApiResponse.created(defaultService.create(request));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<MonthlyCheckDefaultDto.Response> update(
            @PathVariable Long id,
            @Valid @RequestBody MonthlyCheckDefaultDto.Request request) {
        return ApiResponse.ok(defaultService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        defaultService.delete(id);
        return ApiResponse.noContent();
    }
}
