package com.monosun.secportal.detection.controller;

import com.monosun.secportal.audit.service.AuditLogService;
import com.monosun.secportal.auth.entity.User;
import com.monosun.secportal.common.net.ClientIp;
import com.monosun.secportal.common.response.ApiResponse;
import com.monosun.secportal.detection.dto.DetectionDto;
import com.monosun.secportal.detection.entity.DetectionEvent;
import com.monosun.secportal.detection.service.BehaviorAnalyzer;
import com.monosun.secportal.detection.service.DetectionEventService;
import com.monosun.secportal.detection.service.DetectionSettingsService;
import com.monosun.secportal.detection.service.IpBlockService;
import com.monosun.secportal.detection.service.RequestTracker;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/** 자동화·AI 공격 탐지 관리 API — 조회는 관리자·담당자, 설정 변경은 관리자만 */
@RestController
@RequestMapping("/detection")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
public class DetectionController {

    private final DetectionEventService eventService;
    private final DetectionSettingsService settingsService;
    private final IpBlockService ipBlockService;
    private final BehaviorAnalyzer analyzer;
    private final RequestTracker tracker;
    private final AuditLogService auditLogService;

    @GetMapping("/summary")
    public ApiResponse<DetectionDto.Summary> summary() {
        return ApiResponse.ok(eventService.summary());
    }

    @GetMapping("/events")
    public ApiResponse<Page<DetectionDto.EventResponse>> events(
            @RequestParam(required = false) DetectionEvent.Category category,
            @RequestParam(required = false) DetectionEvent.Severity severity,
            @RequestParam(required = false) DetectionEvent.Status status,
            @RequestParam(required = false) String ip,
            @RequestParam(required = false) Integer days,
            @PageableDefault(size = 30) Pageable pageable) {
        return ApiResponse.ok(eventService.search(category, severity, status, ip, days, pageable));
    }

    @GetMapping("/events/{id}")
    public ApiResponse<DetectionDto.EventResponse> event(@PathVariable Long id) {
        return ApiResponse.ok(eventService.get(id));
    }

    @PatchMapping("/events/{id}/status")
    public ApiResponse<DetectionDto.EventResponse> updateStatus(@PathVariable Long id,
                                                                @RequestBody DetectionDto.StatusRequest req,
                                                                @AuthenticationPrincipal User user) {
        return ApiResponse.ok(eventService.updateStatus(id, req.getStatus(), req.getNote(), user));
    }

    @PostMapping("/events/{id}/escalate")
    public ApiResponse<DetectionDto.EventResponse> escalate(@PathVariable Long id, @AuthenticationPrincipal User user) {
        return ApiResponse.ok(eventService.escalate(id, user));
    }

    @DeleteMapping("/events/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> deleteEvent(@PathVariable Long id) {
        eventService.delete(id);
        return ApiResponse.noContent();
    }

    @PostMapping("/events/test")
    public ApiResponse<DetectionDto.EventResponse> test(@AuthenticationPrincipal User user, HttpServletRequest req) {
        return ApiResponse.created(eventService.createTest(user, ClientIp.resolve(req)));
    }

    /** 실시간 의심 IP — 마지막 행위 분석 결과 */
    @GetMapping("/live")
    public ApiResponse<DetectionDto.LiveResponse> live() {
        DetectionSettingsService.Settings s = settingsService.get();
        return ApiResponse.ok(DetectionDto.LiveResponse.builder()
                .analyzedAt(analyzer.lastRunAt())
                .windowMinutes(s.windowMinutes())
                .scoreThreshold(s.scoreThreshold())
                .trackedIps(analyzer.trackedIps())
                .ips(analyzer.lastScores())
                .build());
    }

    @GetMapping("/blocks")
    public ApiResponse<List<DetectionDto.BlockResponse>> blocks() {
        return ApiResponse.ok(ipBlockService.list().stream().map(DetectionDto.BlockResponse::from).toList());
    }

    @PostMapping("/blocks")
    public ApiResponse<DetectionDto.BlockResponse> block(@RequestBody DetectionDto.BlockRequest req,
                                                         @AuthenticationPrincipal User user,
                                                         HttpServletRequest http) {
        if (req.getIp() != null && req.getIp().trim().equals(ClientIp.resolve(http))) {
            throw new com.monosun.secportal.common.exception.BusinessException("현재 접속 중인 본인 IP 는 차단할 수 없습니다.");
        }
        int minutes = req.getMinutes() == null ? 60 : req.getMinutes();
        var b = ipBlockService.block(req.getIp(), minutes, req.getReason(), user.getEmail());
        auditLogService.log("DETECTION_IP_BLOCK", "DETECTION_BLOCK", b.getId(),
                b.getIp() + " / " + (minutes == 0 ? "무기한" : minutes + "분") + " / " + req.getReason());
        return ApiResponse.created(DetectionDto.BlockResponse.from(b));
    }

    @DeleteMapping("/blocks/{id}")
    public ApiResponse<Void> release(@PathVariable Long id, @AuthenticationPrincipal User user) {
        ipBlockService.release(id, user.getEmail());
        auditLogService.log("DETECTION_IP_RELEASE", "DETECTION_BLOCK", id, "released by " + user.getEmail());
        return ApiResponse.noContent();
    }

    /** IP 로그인 일시 차단(크리덴셜 스터핑 대응) 해제 */
    @DeleteMapping("/login-blocks/{ip}")
    public ApiResponse<Void> releaseLoginBlock(@PathVariable String ip) {
        tracker.clearLoginBlock(ip);
        auditLogService.log("DETECTION_LOGIN_UNBLOCK", "DETECTION_BLOCK", null, ip);
        return ApiResponse.noContent();
    }

    @GetMapping("/settings")
    public ApiResponse<Map<String, Object>> settings(HttpServletRequest req) {
        Map<String, Object> m = settingsService.toMap();
        m.put("yourIp", ClientIp.resolve(req));   // 허용 목록 입력 도움말용
        return ApiResponse.ok(m);
    }

    @PutMapping("/settings")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Map<String, Object>> saveSettings(@RequestBody Map<String, Object> body, HttpServletRequest req) {
        settingsService.save(body);
        auditLogService.log("DETECTION_SETTINGS", "DETECTION_SETTINGS", null, "공격 탐지 설정 변경");
        Map<String, Object> m = settingsService.toMap();
        m.put("yourIp", ClientIp.resolve(req));
        return ApiResponse.ok(m);
    }

    @PostMapping("/settings/honey-token")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Map<String, Object>> regenerateHoneyToken(HttpServletRequest req) {
        settingsService.regenerateHoneyToken();
        auditLogService.log("DETECTION_SETTINGS", "DETECTION_SETTINGS", null, "허니토큰 재발급");
        Map<String, Object> m = settingsService.toMap();
        m.put("yourIp", ClientIp.resolve(req));
        return ApiResponse.ok(m);
    }
}
