package com.monosun.secportal.security.controller;

import com.monosun.secportal.common.response.ApiResponse;
import com.monosun.secportal.security.dto.SecurityDto;
import com.monosun.secportal.security.service.SecurityEventService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/security-integrations")
@RequiredArgsConstructor
public class SecurityEventController {

    private final SecurityEventService service;

    @GetMapping
    public ApiResponse<List<SecurityDto.IntegrationResponse>> list() {
        return ApiResponse.ok(service.listIntegrations());
    }

    @GetMapping("/{id}")
    public ApiResponse<SecurityDto.IntegrationResponse> get(@PathVariable Long id) {
        return ApiResponse.ok(service.getIntegration(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ApiResponse<SecurityDto.IntegrationResponse> create(
            @Valid @RequestBody SecurityDto.IntegrationCreateRequest req) {
        return ApiResponse.created(service.createIntegration(req));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ApiResponse<SecurityDto.IntegrationResponse> update(
            @PathVariable Long id,
            @RequestBody SecurityDto.IntegrationUpdateRequest req) {
        return ApiResponse.ok(service.updateIntegration(id, req));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        service.deleteIntegration(id);
        return ApiResponse.noContent();
    }

    @GetMapping("/{id}/events")
    public ApiResponse<Page<SecurityDto.EventResponse>> listEvents(
            @PathVariable Long id,
            @PageableDefault(size = 50) Pageable pageable) {
        return ApiResponse.ok(service.listEvents(id, pageable));
    }

    @PostMapping("/{id}/events")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ApiResponse<SecurityDto.EventResponse> createEvent(
            @PathVariable Long id,
            @Valid @RequestBody SecurityDto.EventCreateRequest req) {
        return ApiResponse.created(service.createEvent(id, req));
    }

    @DeleteMapping("/events/{eventId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ApiResponse<Void> deleteEvent(@PathVariable Long eventId) {
        service.deleteEvent(eventId);
        return ApiResponse.noContent();
    }
}
