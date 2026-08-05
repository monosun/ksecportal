package com.monosun.secportal.emergency.controller;

import com.monosun.secportal.common.response.ApiResponse;
import com.monosun.secportal.emergency.dto.EmergencyDto;
import com.monosun.secportal.emergency.service.EmergencyContactService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/emergency-contacts")
@RequiredArgsConstructor
public class EmergencyContactController {

    private final EmergencyContactService service;

    // ── 연락망 그룹 ────────────────────────────────────────────────────────

    @GetMapping("/groups")
    public ApiResponse<List<EmergencyDto.GroupResponse>> listGroups() {
        return ApiResponse.ok(service.listGroups());
    }

    @GetMapping("/groups/{id}")
    public ApiResponse<EmergencyDto.GroupResponse> getGroup(@PathVariable Long id) {
        return ApiResponse.ok(service.getGroup(id));
    }

    @PostMapping("/groups")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ApiResponse<EmergencyDto.GroupResponse> createGroup(
            @Valid @RequestBody EmergencyDto.GroupRequest req) {
        return ApiResponse.created(service.createGroup(req));
    }

    @PatchMapping("/groups/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ApiResponse<EmergencyDto.GroupResponse> updateGroup(
            @PathVariable Long id,
            @Valid @RequestBody EmergencyDto.GroupRequest req) {
        return ApiResponse.ok(service.updateGroup(id, req));
    }

    @PatchMapping("/groups/{id}/toggle")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ApiResponse<Void> toggleGroup(@PathVariable Long id) {
        service.toggleGroupActive(id);
        return ApiResponse.noContent();
    }

    @DeleteMapping("/groups/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> deleteGroup(@PathVariable Long id) {
        service.deleteGroup(id);
        return ApiResponse.noContent();
    }

    // ── 연락처 ────────────────────────────────────────────────────────────

    @PostMapping("/contacts")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ApiResponse<EmergencyDto.ContactResponse> createContact(
            @Valid @RequestBody EmergencyDto.ContactRequest req) {
        return ApiResponse.created(service.createContact(req));
    }

    @PatchMapping("/contacts/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ApiResponse<EmergencyDto.ContactResponse> updateContact(
            @PathVariable Long id,
            @Valid @RequestBody EmergencyDto.ContactRequest req) {
        return ApiResponse.ok(service.updateContact(id, req));
    }

    @PatchMapping("/contacts/{id}/toggle")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ApiResponse<Void> toggleContact(@PathVariable Long id) {
        service.toggleContactActive(id);
        return ApiResponse.noContent();
    }

    @DeleteMapping("/contacts/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ApiResponse<Void> deleteContact(@PathVariable Long id) {
        service.deleteContact(id);
        return ApiResponse.noContent();
    }
}
