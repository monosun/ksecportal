package com.monosun.secportal.privacy.service;

import com.monosun.secportal.audit.service.AuditLogService;
import com.monosun.secportal.common.exception.ResourceNotFoundException;
import com.monosun.secportal.privacy.dto.PrivacyBreachDto;
import com.monosun.secportal.privacy.entity.PrivacyBreach;
import com.monosun.secportal.privacy.repository.PrivacyBreachRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PrivacyBreachService {

    /** 개인정보 유출 신고기한 — 인지 시점부터 72시간 (개인정보 보호법 제34조) */
    private static final int REPORT_DEADLINE_HOURS = 72;

    private final PrivacyBreachRepository repo;
    private final AuditLogService auditLogService;

    @Transactional(readOnly = true)
    public List<PrivacyBreachDto.Response> listAll() {
        return repo.findAllByOrderByDetectedAtDesc().stream()
                .map(PrivacyBreachDto.Response::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public PrivacyBreachDto.Response get(Long id) {
        return PrivacyBreachDto.Response.from(find(id));
    }

    @Transactional
    public PrivacyBreachDto.Response create(PrivacyBreachDto.Request req) {
        PrivacyBreach b = PrivacyBreach.builder()
                .title(req.getTitle().trim())
                .occurredAt(req.getOccurredAt())
                .detectedAt(req.getDetectedAt())
                .reportDueAt(resolveReportDueAt(req))
                .affectedCount(req.getAffectedCount())
                .infoItems(req.getInfoItems())
                .cause(req.getCause())
                .subjectNotified(req.getSubjectNotified() != null ? req.getSubjectNotified() : false)
                .notifiedDate(req.getNotifiedDate())
                .notifyMethod(req.getNotifyMethod())
                .authorityReported(req.getAuthorityReported() != null ? req.getAuthorityReported() : false)
                .authorityName(req.getAuthorityName())
                .reportedDate(req.getReportedDate())
                .investigation(req.getInvestigation())
                .preventionPlan(req.getPreventionPlan())
                .closedDate(req.getClosedDate())
                .status(parseStatus(req.getStatus()))
                .notes(req.getNotes())
                .build();
        PrivacyBreach saved = repo.save(b);
        auditLogService.log("CREATE", "PrivacyBreach", saved.getId(), saved.getTitle());
        return PrivacyBreachDto.Response.from(saved);
    }

    @Transactional
    public PrivacyBreachDto.Response update(Long id, PrivacyBreachDto.Request req) {
        PrivacyBreach b = find(id);
        if (req.getTitle() != null && !req.getTitle().isBlank()) b.setTitle(req.getTitle().trim());
        if (req.getOccurredAt() != null) b.setOccurredAt(req.getOccurredAt());
        if (req.getDetectedAt() != null) {
            b.setDetectedAt(req.getDetectedAt());
            // 인지일시가 바뀌면 신고기한을 다시 계산한다 (명시적으로 준 값이 없을 때만)
            if (req.getReportDueAt() == null) {
                b.setReportDueAt(req.getDetectedAt().plusHours(REPORT_DEADLINE_HOURS));
            }
        }
        if (req.getReportDueAt() != null) b.setReportDueAt(req.getReportDueAt());
        if (req.getAffectedCount() != null) b.setAffectedCount(req.getAffectedCount());
        if (req.getInfoItems() != null) b.setInfoItems(req.getInfoItems());
        if (req.getCause() != null) b.setCause(req.getCause());
        if (req.getSubjectNotified() != null) b.setSubjectNotified(req.getSubjectNotified());
        if (req.getNotifiedDate() != null) b.setNotifiedDate(req.getNotifiedDate());
        if (req.getNotifyMethod() != null) b.setNotifyMethod(req.getNotifyMethod());
        if (req.getAuthorityReported() != null) b.setAuthorityReported(req.getAuthorityReported());
        if (req.getAuthorityName() != null) b.setAuthorityName(req.getAuthorityName());
        if (req.getReportedDate() != null) b.setReportedDate(req.getReportedDate());
        if (req.getInvestigation() != null) b.setInvestigation(req.getInvestigation());
        if (req.getPreventionPlan() != null) b.setPreventionPlan(req.getPreventionPlan());
        if (req.getClosedDate() != null) b.setClosedDate(req.getClosedDate());
        if (req.getStatus() != null) b.setStatus(parseStatus(req.getStatus()));
        if (req.getNotes() != null) b.setNotes(req.getNotes());
        auditLogService.log("UPDATE", "PrivacyBreach", b.getId(), b.getTitle());
        return PrivacyBreachDto.Response.from(b);
    }

    @Transactional
    public void delete(Long id) {
        PrivacyBreach b = find(id);
        repo.delete(b);
        auditLogService.log("DELETE", "PrivacyBreach", id, b.getTitle());
    }

    /** 신고기한을 명시하지 않으면 인지일시 + 72시간으로 자동 산정한다. */
    private java.time.LocalDateTime resolveReportDueAt(PrivacyBreachDto.Request req) {
        if (req.getReportDueAt() != null) return req.getReportDueAt();
        if (req.getDetectedAt() != null) return req.getDetectedAt().plusHours(REPORT_DEADLINE_HOURS);
        return null;
    }

    private PrivacyBreach find(Long id) {
        return repo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("PrivacyBreach", id));
    }

    private PrivacyBreach.Status parseStatus(String s) {
        if (s == null || s.isBlank()) return PrivacyBreach.Status.DETECTED;
        try {
            return PrivacyBreach.Status.valueOf(s);
        } catch (IllegalArgumentException e) {
            return PrivacyBreach.Status.DETECTED;
        }
    }
}
