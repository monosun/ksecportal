package com.monosun.secportal.notification.controller;

import com.monosun.secportal.notification.service.NotificationConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/admin/notification-config")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class NotificationConfigController {

    private final NotificationConfigService configService;

    @GetMapping
    public ResponseEntity<Map<String, String>> get() {
        return ResponseEntity.ok(configService.getAll());
    }

    @PutMapping
    public ResponseEntity<Map<String, String>> update(@RequestBody Map<String, String> body) {
        configService.save(body);
        return ResponseEntity.ok(configService.getAll());
    }
}
