package com.monosun.secportal.admin.service;

import com.monosun.secportal.admin.dto.UserAdminDto;
import com.monosun.secportal.admin.entity.PendingAdminAction;
import com.monosun.secportal.admin.repository.PendingAdminActionRepository;
import com.monosun.secportal.audit.service.AuditLogService;
import com.monosun.secportal.auth.entity.User;
import com.monosun.secportal.auth.repository.UserRepository;
import com.monosun.secportal.common.exception.BusinessException;
import com.monosun.secportal.common.exception.ResourceNotFoundException;
import com.monosun.secportal.inbox.entity.InboxMessage;
import com.monosun.secportal.inbox.repository.InboxMessageRepository;
import com.monosun.secportal.notification.service.ApprovalNotificationService;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserAdminService {

    private final UserRepository userRepository;
    private final PendingAdminActionRepository pendingActionRepository;
    private final AuditLogService auditLogService;
    private final ApprovalNotificationService notificationService;
    private final InboxMessageRepository inboxMessageRepository;
    private final PasswordEncoder passwordEncoder;
    private final EntityManager em;
    private final com.monosun.secportal.setting.service.AppSettingService appSettingService;

    @Value("${app.base-url:http://localhost:8080/api}")
    private String defaultBaseUrl;

    /** 이메일 링크 기준 URL — 설정관리 app.base_url(도메인 주소) 우선, 없으면 환경변수 APP_BASE_URL */
    private String baseUrl() {
        return appSettingService.resolveBaseUrl(defaultBaseUrl);
    }

    @Transactional(readOnly = true)
    public boolean isAlreadyAdmin(Long id) {
        return userRepository.findById(id)
                .map(u -> u.getRole() == User.Role.ADMIN)
                .orElse(false);
    }

    @Transactional(readOnly = true)
    public Page<UserAdminDto.Response> list(Pageable pageable) {
        return userRepository.findAll(pageable).map(UserAdminDto.Response::from);
    }

    @Transactional(readOnly = true)
    public List<UserAdminDto.SimpleResponse> listSimple() {
        return userRepository.findAllByActiveTrueOrderByNameAsc()
                .stream()
                .map(UserAdminDto.SimpleResponse::from)
                .collect(Collectors.toList());
    }

    @Transactional
    public UserAdminDto.Response update(Long id, UserAdminDto.UpdateRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", id));
        if (request.getName() != null && !request.getName().isBlank()) user.setName(request.getName());
        if (request.getRole() != null) user.setRole(request.getRole());
        if (request.getActive() != null) user.setActive(request.getActive());
        if (request.getDepartment() != null) user.setDepartment(request.getDepartment());
        if (request.getNewPassword() != null && !request.getNewPassword().isBlank()) {
            user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        }
        auditLogService.log("USER_UPDATED", "USER", id,
                "name=" + user.getName() + ", role=" + user.getRole() + ", active=" + user.isActive());
        return UserAdminDto.Response.from(user);
    }

    /** 비밀번호 오류 횟수 초기화 + 계정 잠금 해제 */
    @Transactional
    public UserAdminDto.Response resetFailedAttempts(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", id));
        user.setFailedLoginAttempts(0);
        user.setLockedUntil(null);
        auditLogService.log("USER_UNLOCKED", "USER", id,
                "email=" + user.getEmail() + " — 비밀번호 오류 횟수 초기화 및 잠금 해제");
        return UserAdminDto.Response.from(user);
    }

    @Transactional
    public UserAdminDto.Response create(UserAdminDto.CreateRequest request, User requester) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException("이미 사용 중인 이메일입니다.");
        }
        User newUser = User.builder()
                .email(request.getEmail())
                .name(request.getName())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(request.getRole() != null ? request.getRole() : User.Role.USER)
                .department(request.getDepartment())
                .active(true)
                .build();
        userRepository.save(newUser);
        auditLogService.log("USER_CREATED", "USER", newUser.getId(),
                "email=" + newUser.getEmail() + ", role=" + newUser.getRole());
        notificationService.send(
                "[SecPortal] 신규 계정 생성 알림",
                buildCreatedNotificationHtml(newUser, requester),
                "[SecPortal] 신규 계정 생성\n이름: " + newUser.getName() + "\n이메일: " + newUser.getEmail() + "\n역할: " + newUser.getRole() + "\n등록자: " + requester.getName());
        return UserAdminDto.Response.from(newUser);
    }

    @Transactional
    public UserAdminDto.PendingResponse requestDelete(Long targetId, User requester) {
        User target = userRepository.findById(targetId)
                .orElseThrow(() -> new ResourceNotFoundException("User", targetId));
        String token = UUID.randomUUID().toString();
        PendingAdminAction action = PendingAdminAction.builder()
                .token(token)
                .actionType(PendingAdminAction.ActionType.DELETE_USER)
                .targetUser(target)
                .requester(requester)
                .createdAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusHours(24))
                .build();
        pendingActionRepository.save(action);
        auditLogService.log("USER_DELETE_REQUESTED", "USER", targetId,
                "requester=" + requester.getEmail());
        notificationService.send(
                "[SecPortal] 계정 삭제 승인 요청",
                buildDeleteApprovalHtml(target, requester, token),
                "[SecPortal] 계정 삭제 승인 요청\n대상: " + target.getName() + " (" + target.getEmail() + ")\n요청자: " + requester.getName()
                + "\n승인: " + baseUrl() + "/admin/approve/" + token
                + "\n거부: " + baseUrl() + "/admin/reject/" + token);
        sendInboxToAdmins(
                "[승인 요청] 계정 삭제 — " + target.getName(),
                requester.getName() + "님이 " + target.getName() + " (" + target.getEmail() + ") 계정 삭제를 요청했습니다.",
                token, requester);
        return UserAdminDto.PendingResponse.builder()
                .pending(true)
                .actionType("DELETE_USER")
                .message("승인 알림이 발송되었습니다.")
                .build();
    }

    @Transactional
    public UserAdminDto.PendingResponse requestPromoteAdmin(Long targetId, User requester) {
        User target = userRepository.findById(targetId)
                .orElseThrow(() -> new ResourceNotFoundException("User", targetId));
        String token = UUID.randomUUID().toString();
        PendingAdminAction action = PendingAdminAction.builder()
                .token(token)
                .actionType(PendingAdminAction.ActionType.PROMOTE_ADMIN)
                .targetUser(target)
                .requester(requester)
                .createdAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusHours(24))
                .build();
        pendingActionRepository.save(action);
        auditLogService.log("USER_PROMOTE_ADMIN_REQUESTED", "USER", targetId,
                "requester=" + requester.getEmail());
        notificationService.send(
                "[SecPortal] ADMIN 권한 부여 승인 요청",
                buildPromoteAdminApprovalHtml(target, requester, token),
                "[SecPortal] ADMIN 권한 부여 승인 요청\n대상: " + target.getName() + " (" + target.getEmail() + ")\n요청자: " + requester.getName()
                + "\n승인: " + baseUrl() + "/admin/approve/" + token
                + "\n거부: " + baseUrl() + "/admin/reject/" + token);
        sendInboxToAdmins(
                "[승인 요청] ADMIN 권한 부여 — " + target.getName(),
                requester.getName() + "님이 " + target.getName() + " (" + target.getEmail() + ") 계정에 ADMIN 권한 부여를 요청했습니다.",
                token, requester);
        return UserAdminDto.PendingResponse.builder()
                .pending(true)
                .actionType("PROMOTE_ADMIN")
                .message("승인 알림이 발송되었습니다.")
                .build();
    }

    @Transactional
    public String approveAction(String token) {
        PendingAdminAction action = pendingActionRepository.findByToken(token)
                .orElseThrow(() -> new IllegalArgumentException("유효하지 않은 토큰입니다."));
        if (action.getStatus() == PendingAdminAction.Status.APPROVED) {
            return "이미 승인된 요청입니다.";
        }
        if (action.getStatus() == PendingAdminAction.Status.REJECTED) {
            return "이미 거부된 요청입니다.";
        }
        if (action.getExpiresAt().isBefore(LocalDateTime.now())) {
            action.setStatus(PendingAdminAction.Status.EXPIRED);
            return "만료된 요청입니다. (24시간 초과)";
        }
        action.setStatus(PendingAdminAction.Status.APPROVED);
        User target = action.getTargetUser();
        if (action.getActionType() == PendingAdminAction.ActionType.DELETE_USER) {
            target.setActive(false);
            auditLogService.log("USER_DEACTIVATED", "USER", target.getId(), "approved via inbox");
            return target.getName() + " (" + target.getEmail() + ") 계정이 비활성화되었습니다.";
        } else if (action.getActionType() == PendingAdminAction.ActionType.PROMOTE_ADMIN) {
            target.setRole(User.Role.ADMIN);
            auditLogService.log("USER_PROMOTED_ADMIN", "USER", target.getId(), "approved via inbox");
            return target.getName() + " (" + target.getEmail() + ") 계정이 ADMIN으로 승격되었습니다.";
        } else if (action.getActionType() == PendingAdminAction.ActionType.HARD_DELETE_USER) {
            String name = target.getName();
            String email = target.getEmail();
            doHardDelete(target.getId());
            return name + " (" + email + ") 계정이 완전 삭제되었습니다.";
        }
        return "처리 완료되었습니다.";
    }

    @Transactional
    public String rejectAction(String token) {
        PendingAdminAction action = pendingActionRepository.findByToken(token)
                .orElseThrow(() -> new IllegalArgumentException("유효하지 않은 토큰입니다."));
        if (action.getStatus() != PendingAdminAction.Status.PENDING) {
            return action.getStatus() == PendingAdminAction.Status.APPROVED
                    ? "이미 승인된 요청입니다." : "이미 거부된 요청입니다.";
        }
        action.setStatus(PendingAdminAction.Status.REJECTED);
        User target = action.getTargetUser();
        auditLogService.log("ADMIN_ACTION_REJECTED", "USER", target.getId(),
                "type=" + action.getActionType() + ", requester=" + action.getRequester().getEmail());
        return "요청이 거부되었습니다. (" + action.getActionType() + " for " + target.getName() + ")";
    }

    @Transactional
    public UserAdminDto.PendingResponse requestHardDelete(Long targetId, User requester) {
        User target = userRepository.findById(targetId)
                .orElseThrow(() -> new ResourceNotFoundException("User", targetId));
        if (target.isActive()) {
            throw new BusinessException("비활성화된 계정만 완전 삭제 요청이 가능합니다.");
        }
        String token = UUID.randomUUID().toString();
        PendingAdminAction action = PendingAdminAction.builder()
                .token(token)
                .actionType(PendingAdminAction.ActionType.HARD_DELETE_USER)
                .targetUser(target)
                .requester(requester)
                .createdAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusHours(24))
                .build();
        pendingActionRepository.save(action);
        auditLogService.log("USER_HARD_DELETE_REQUESTED", "USER", targetId,
                "requester=" + requester.getEmail());
        notificationService.send(
                "[SecPortal] 계정 영구 삭제 승인 요청",
                buildHardDeleteApprovalHtml(target, requester, token),
                "[SecPortal] 계정 영구 삭제 승인 요청\n대상: " + target.getName() + " (" + target.getEmail() + ")\n요청자: " + requester.getName()
                + "\n승인: " + baseUrl() + "/admin/approve/" + token
                + "\n거부: " + baseUrl() + "/admin/reject/" + token);
        sendInboxToAdmins(
                "[승인 요청] 계정 완전 삭제 — " + target.getName(),
                requester.getName() + "님이 " + target.getName() + " (" + target.getEmail() + ") 계정 완전 삭제를 요청했습니다.",
                token, requester);
        return UserAdminDto.PendingResponse.builder()
                .pending(true)
                .actionType("HARD_DELETE_USER")
                .message("완전 삭제 승인 요청이 전송되었습니다.")
                .build();
    }

    private void doHardDelete(Long userId) {
        auditLogService.log("USER_HARD_DELETED", "USER", userId, "permanently deleted");
        // 참조 컬럼 NULL 처리 (비즈니스 레코드 보존)
        em.createNativeQuery("UPDATE audit_logs SET user_id = NULL WHERE user_id = :id").setParameter("id", userId).executeUpdate();
        em.createNativeQuery("UPDATE incidents SET reporter_id = NULL WHERE reporter_id = :id").setParameter("id", userId).executeUpdate();
        em.createNativeQuery("UPDATE incidents SET assignee_id = NULL WHERE assignee_id = :id").setParameter("id", userId).executeUpdate();
        em.createNativeQuery("UPDATE vulnerabilities SET reporter_id = NULL WHERE reporter_id = :id").setParameter("id", userId).executeUpdate();
        em.createNativeQuery("UPDATE vulnerabilities SET assignee_id = NULL WHERE assignee_id = :id").setParameter("id", userId).executeUpdate();
        em.createNativeQuery("UPDATE vulnerability_comments SET user_id = NULL WHERE user_id = :id").setParameter("id", userId).executeUpdate();
        em.createNativeQuery("UPDATE policies SET author_id = NULL WHERE author_id = :id").setParameter("id", userId).executeUpdate();
        em.createNativeQuery("UPDATE training_courses SET created_by = NULL WHERE created_by = :id").setParameter("id", userId).executeUpdate();
        em.createNativeQuery("UPDATE isms_evidences SET registrant_id = NULL WHERE registrant_id = :id").setParameter("id", userId).executeUpdate();
        em.createNativeQuery("UPDATE notices SET created_by_id = NULL WHERE created_by_id = :id").setParameter("id", userId).executeUpdate();
        // 사용자 소유 레코드 삭제
        em.createNativeQuery("DELETE FROM inbox_messages WHERE recipient_id = :id").setParameter("id", userId).executeUpdate();
        em.createNativeQuery("DELETE FROM pending_admin_actions WHERE target_user_id = :id OR requester_id = :id").setParameter("id", userId).executeUpdate();
        em.createNativeQuery("DELETE FROM policy_acknowledgments WHERE user_id = :id").setParameter("id", userId).executeUpdate();
        em.createNativeQuery("DELETE FROM training_completions WHERE user_id = :id").setParameter("id", userId).executeUpdate();
        em.createNativeQuery("DELETE FROM users WHERE id = :id").setParameter("id", userId).executeUpdate();
    }

    // ── Email Templates ──────────────────────────────────────────────────────

    private String buildCreatedNotificationHtml(User newUser, User requester) {
        return "<h2 style='color:#1e40af'>신규 계정 생성 알림</h2>"
                + "<p><b>" + h(requester.getName()) + "</b>이(가) 새 계정을 생성했습니다.</p>"
                + "<table style='border-collapse:collapse'>"
                + "<tr><td style='padding:4px 12px 4px 0;color:#6b7280'>이름</td><td><b>" + h(newUser.getName()) + "</b></td></tr>"
                + "<tr><td style='padding:4px 12px 4px 0;color:#6b7280'>이메일</td><td>" + h(newUser.getEmail()) + "</td></tr>"
                + "<tr><td style='padding:4px 12px 4px 0;color:#6b7280'>역할</td><td>" + h(newUser.getRole().name()) + "</td></tr>"
                + "</table>";
    }

    private String buildDeleteApprovalHtml(User target, User requester, String token) {
        String approveUrl = baseUrl() + "/admin/approve/" + token;
        String rejectUrl = baseUrl() + "/admin/reject/" + token;
        return "<h2 style='color:#dc2626'>계정 삭제 승인 요청</h2>"
                + "<p><b>" + h(requester.getName()) + "</b>이(가) 계정 삭제를 요청했습니다.</p>"
                + "<table style='border-collapse:collapse'>"
                + "<tr><td style='padding:4px 12px 4px 0;color:#6b7280'>대상</td><td><b>" + h(target.getName()) + "</b> (" + h(target.getEmail()) + ")</td></tr>"
                + "<tr><td style='padding:4px 12px 4px 0;color:#6b7280'>현재 역할</td><td>" + h(target.getRole().name()) + "</td></tr>"
                + "</table>"
                + "<p style='color:#6b7280;font-size:0.875rem'>24시간 내 승인하지 않으면 자동 만료됩니다.</p>"
                + "<div style='margin-top:16px'>"
                + "<a href='" + approveUrl + "' style='background:#2563eb;color:white;padding:10px 24px;text-decoration:none;border-radius:6px;margin-right:12px;font-weight:600'>승인</a>"
                + "<a href='" + rejectUrl + "' style='background:#dc2626;color:white;padding:10px 24px;text-decoration:none;border-radius:6px;font-weight:600'>거부</a>"
                + "</div>";
    }

    private String buildHardDeleteApprovalHtml(User target, User requester, String token) {
        String approveUrl = baseUrl() + "/admin/approve/" + token;
        String rejectUrl = baseUrl() + "/admin/reject/" + token;
        return "<h2 style='color:#dc2626'>계정 영구 삭제 승인 요청</h2>"
                + "<p><b>" + h(requester.getName()) + "</b>이(가) 계정 <b>영구 삭제</b>를 요청했습니다.</p>"
                + "<p style='color:#dc2626;font-weight:bold'>⚠ 이 작업은 되돌릴 수 없습니다.</p>"
                + "<table style='border-collapse:collapse'>"
                + "<tr><td style='padding:4px 12px 4px 0;color:#6b7280'>대상</td><td><b>" + h(target.getName()) + "</b> (" + h(target.getEmail()) + ")</td></tr>"
                + "<tr><td style='padding:4px 12px 4px 0;color:#6b7280'>현재 역할</td><td>" + h(target.getRole().name()) + "</td></tr>"
                + "</table>"
                + "<p style='color:#6b7280;font-size:0.875rem'>24시간 내 승인하지 않으면 자동 만료됩니다.</p>"
                + "<div style='margin-top:16px'>"
                + "<a href='" + approveUrl + "' style='background:#dc2626;color:white;padding:10px 24px;text-decoration:none;border-radius:6px;margin-right:12px;font-weight:600'>영구 삭제 승인</a>"
                + "<a href='" + rejectUrl + "' style='background:#6b7280;color:white;padding:10px 24px;text-decoration:none;border-radius:6px;font-weight:600'>거부</a>"
                + "</div>";
    }

    private String buildPromoteAdminApprovalHtml(User target, User requester, String token) {
        String approveUrl = baseUrl() + "/admin/approve/" + token;
        String rejectUrl = baseUrl() + "/admin/reject/" + token;
        return "<h2 style='color:#d97706'>ADMIN 권한 부여 승인 요청</h2>"
                + "<p><b>" + h(requester.getName()) + "</b>이(가) ADMIN 권한 부여를 요청했습니다.</p>"
                + "<table style='border-collapse:collapse'>"
                + "<tr><td style='padding:4px 12px 4px 0;color:#6b7280'>대상</td><td><b>" + h(target.getName()) + "</b> (" + h(target.getEmail()) + ")</td></tr>"
                + "<tr><td style='padding:4px 12px 4px 0;color:#6b7280'>현재 역할</td><td>" + h(target.getRole().name()) + " → <b>ADMIN</b></td></tr>"
                + "</table>"
                + "<p style='color:#6b7280;font-size:0.875rem'>24시간 내 승인하지 않으면 자동 만료됩니다.</p>"
                + "<div style='margin-top:16px'>"
                + "<a href='" + approveUrl + "' style='background:#2563eb;color:white;padding:10px 24px;text-decoration:none;border-radius:6px;margin-right:12px;font-weight:600'>승인</a>"
                + "<a href='" + rejectUrl + "' style='background:#dc2626;color:white;padding:10px 24px;text-decoration:none;border-radius:6px;font-weight:600'>거부</a>"
                + "</div>";
    }

    private void sendInboxToAdmins(String title, String content, String token, User requester) {
        userRepository.findAllByRoleAndActiveTrue(User.Role.ADMIN).forEach(admin ->
            inboxMessageRepository.save(InboxMessage.builder()
                    .recipient(admin)
                    .type(InboxMessage.MessageType.APPROVAL_REQUEST)
                    .title(title)
                    .content(content)
                    .actionToken(token)
                    .build())
        );
    }

    private static String h(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#x27;");
    }
}
