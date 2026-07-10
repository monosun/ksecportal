package com.monosun.secportal.asset.controller;

import com.monosun.secportal.asset.dto.AssetSnapshotDto;
import com.monosun.secportal.asset.service.AssetSnapshotService;
import com.monosun.secportal.auth.entity.User;
import com.monosun.secportal.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/assets/snapshots")
@RequiredArgsConstructor
public class AssetSnapshotController {

    private final AssetSnapshotService snapshotService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ApiResponse<List<AssetSnapshotDto.SnapshotResponse>> list() {
        return ApiResponse.ok(snapshotService.list());
    }

    @GetMapping("/{id}/items")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ApiResponse<List<AssetSnapshotDto.ItemResponse>> items(@PathVariable Long id) {
        return ApiResponse.ok(snapshotService.items(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ApiResponse<AssetSnapshotDto.SnapshotResponse> create(
            @RequestBody AssetSnapshotDto.CreateRequest request,
            @AuthenticationPrincipal User currentUser) {
        String createdBy = currentUser != null ? currentUser.getName() : null;
        return ApiResponse.created(snapshotService.create(request.getTitle(), request.getMemo(), createdBy));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        snapshotService.delete(id);
        return ApiResponse.noContent();
    }
}
