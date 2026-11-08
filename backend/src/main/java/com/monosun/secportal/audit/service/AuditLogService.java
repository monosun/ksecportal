package com.monosun.secportal.audit.service;

import com.monosun.secportal.audit.dto.AuditLogDto;
import com.monosun.secportal.audit.entity.AuditLog;
import com.monosun.secportal.audit.repository.AuditLogRepository;
import com.monosun.secportal.auth.entity.User;
import com.monosun.secportal.common.net.ClientIp;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void log(String action, String resourceType, Long resourceId, String detail) {
        try {
            User user = currentUser();
            auditLogRepository.save(AuditLog.builder()
                    .user(user)
                    .action(action)
                    .resourceType(resourceType)
                    .resourceId(resourceId)
                    .detail(detail)
                    .ipAddress(extractIp())
                    .build());
        } catch (Exception e) {
            log.warn("Failed to save audit log: {}", e.getMessage());
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void log(String action, String resourceType, Long resourceId, String detail, User explicitUser) {
        try {
            auditLogRepository.save(AuditLog.builder()
                    .user(explicitUser)
                    .action(action)
                    .resourceType(resourceType)
                    .resourceId(resourceId)
                    .detail(detail)
                    .ipAddress(extractIp())
                    .build());
        } catch (Exception e) {
            log.warn("Failed to save audit log: {}", e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public Page<AuditLogDto.Response> list(String resourceType, String action, Long userId,
                                           LocalDateTime dateFrom, LocalDateTime dateTo, Pageable pageable) {
        return auditLogRepository.search(resourceType, action, userId, dateFrom, dateTo, pageable)
                .map(AuditLogDto.Response::from);
    }

    private User currentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof User user) {
            return user;
        }
        return null;
    }

    private String extractIp() {
        // X-Forwarded-For 첫 값은 클라이언트가 위조할 수 있으므로 nginx 가 덮어쓰는 X-Real-IP 를 쓴다
        return ClientIp.current();
    }
}
