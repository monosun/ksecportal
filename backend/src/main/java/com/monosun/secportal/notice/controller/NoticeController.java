package com.monosun.secportal.notice.controller;

import com.monosun.secportal.auth.entity.User;
import com.monosun.secportal.common.response.ApiResponse;
import com.monosun.secportal.notice.dto.NoticeDto;
import com.monosun.secportal.notice.service.NoticeService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/notices")
@RequiredArgsConstructor
public class NoticeController {

    private final NoticeService noticeService;

    @GetMapping
    public ApiResponse<List<NoticeDto.Response>> listActive() {
        return ApiResponse.ok(noticeService.listActive());
    }

    @GetMapping("/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<List<NoticeDto.Response>> listAll() {
        return ApiResponse.ok(noticeService.listAll());
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<NoticeDto.Response> create(
            @RequestBody NoticeDto.CreateRequest req,
            @AuthenticationPrincipal User user) {
        return ApiResponse.ok(noticeService.create(req, user));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<NoticeDto.Response> update(
            @PathVariable Long id,
            @RequestBody NoticeDto.UpdateRequest req) {
        return ApiResponse.ok(noticeService.update(id, req));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        noticeService.delete(id);
        return ApiResponse.ok(null);
    }
}
