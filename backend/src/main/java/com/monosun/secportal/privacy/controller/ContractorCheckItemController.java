package com.monosun.secportal.privacy.controller;

import com.monosun.secportal.common.response.ApiResponse;
import com.monosun.secportal.privacy.dto.ContractorCheckItemDto;
import com.monosun.secportal.privacy.service.ContractorCheckItemService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/privacy/contractor-check-items")
@RequiredArgsConstructor
public class ContractorCheckItemController {

    private final ContractorCheckItemService service;

    @GetMapping
    public ApiResponse<List<ContractorCheckItemDto.ItemResponse>> list() {
        return ApiResponse.ok(service.list());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ApiResponse<ContractorCheckItemDto.ItemResponse> create(
            @Valid @RequestBody ContractorCheckItemDto.ItemRequest request) {
        return ApiResponse.created(service.create(request));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ApiResponse<ContractorCheckItemDto.ItemResponse> update(
            @PathVariable Long id,
            @RequestBody ContractorCheckItemDto.ItemRequest request) {
        return ApiResponse.ok(service.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ApiResponse.noContent();
    }

    @GetMapping("/defaults/check")
    public ApiResponse<ContractorCheckItemDto.DefaultCheckResponse> checkDefaults() {
        return ApiResponse.ok(service.checkDefaults());
    }

    @PostMapping("/defaults")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ApiResponse<Map<String, Integer>> loadDefaults() {
        int loaded = service.loadDefaults();
        return ApiResponse.ok(Map.of("loaded", loaded));
    }

    @GetMapping("/defaults")
    public ApiResponse<List<ContractorCheckItemDto.DefaultItemResponse>> listDefaults() {
        return ApiResponse.ok(service.listDefaults());
    }

    @PostMapping("/defaults/item")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ApiResponse<ContractorCheckItemDto.DefaultItemResponse> createDefault(
            @Valid @RequestBody ContractorCheckItemDto.DefaultItemRequest request) {
        return ApiResponse.created(service.createDefault(request));
    }

    @PatchMapping("/defaults/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ApiResponse<ContractorCheckItemDto.DefaultItemResponse> updateDefault(
            @PathVariable Long id,
            @RequestBody ContractorCheckItemDto.DefaultItemRequest request) {
        return ApiResponse.ok(service.updateDefault(id, request));
    }

    @DeleteMapping("/defaults/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ApiResponse<Void> deleteDefault(@PathVariable Long id) {
        service.deleteDefault(id);
        return ApiResponse.noContent();
    }

    @PostMapping("/reset")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ApiResponse<Map<String, Integer>> resetToDefaults() {
        int loaded = service.resetToDefaults();
        return ApiResponse.ok(Map.of("loaded", loaded));
    }
}
