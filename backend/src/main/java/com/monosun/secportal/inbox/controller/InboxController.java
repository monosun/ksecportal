package com.monosun.secportal.inbox.controller;

import com.monosun.secportal.auth.entity.User;
import com.monosun.secportal.common.response.ApiResponse;
import com.monosun.secportal.inbox.dto.InboxMessageDto;
import com.monosun.secportal.inbox.service.InboxService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/inbox")
@RequiredArgsConstructor
public class InboxController {

    private final InboxService inboxService;

    @GetMapping
    public ApiResponse<List<InboxMessageDto.Response>> list(@AuthenticationPrincipal User user) {
        return ApiResponse.ok(inboxService.list(user));
    }

    @GetMapping("/unread-count")
    public ApiResponse<Map<String, Long>> unreadCount(@AuthenticationPrincipal User user) {
        return ApiResponse.ok(Map.of("count", inboxService.unreadCount(user)));
    }

    @PatchMapping("/{id}/read")
    public ApiResponse<Void> markRead(@PathVariable Long id, @AuthenticationPrincipal User user) {
        inboxService.markRead(id, user);
        return ApiResponse.ok(null);
    }

    @PatchMapping("/read-all")
    public ApiResponse<Void> markAllRead(@AuthenticationPrincipal User user) {
        inboxService.markAllRead(user);
        return ApiResponse.ok(null);
    }

    @DeleteMapping
    public ApiResponse<Void> deleteAll(@AuthenticationPrincipal User user) {
        inboxService.deleteAll(user);
        return ApiResponse.ok(null);
    }

    @PostMapping("/{id}/approve")
    public ApiResponse<Void> approve(@PathVariable Long id, @AuthenticationPrincipal User user) {
        return ApiResponse.ok(inboxService.approve(id, user));
    }

    @PostMapping("/{id}/reject")
    public ApiResponse<Void> reject(@PathVariable Long id, @AuthenticationPrincipal User user) {
        return ApiResponse.ok(inboxService.reject(id, user));
    }
}
