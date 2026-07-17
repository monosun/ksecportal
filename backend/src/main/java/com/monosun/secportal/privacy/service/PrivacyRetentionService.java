package com.monosun.secportal.privacy.service;

import com.monosun.secportal.audit.service.AuditLogService;
import com.monosun.secportal.common.exception.ResourceNotFoundException;
import com.monosun.secportal.privacy.dto.PrivacyRetentionDto;
import com.monosun.secportal.privacy.entity.PrivacyRetention;
import com.monosun.secportal.privacy.repository.PrivacyRetentionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PrivacyRetentionService {

    private final PrivacyRetentionRepository repo;
    private final AuditLogService auditLogService;

    @Transactional(readOnly = true)
    public List<PrivacyRetentionDto.Response> listAll() {
        return repo.findAllByOrderByExpiryDateAsc().stream()
                .map(PrivacyRetentionDto.Response::from)
                .toList();
    }

    /** 만료예정 목록 — days 일 이내 만료되며 아직 파기되지 않은 항목 */
    @Transactional(readOnly = true)
    public List<PrivacyRetentionDto.Response> listExpiring(int days) {
        return repo.findByExpiryDateLessThanEqualAndStatusNotOrderByExpiryDateAsc(
                        LocalDate.now().plusDays(days), PrivacyRetention.Status.DISPOSED).stream()
                .map(PrivacyRetentionDto.Response::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public PrivacyRetentionDto.Response get(Long id) {
        return PrivacyRetentionDto.Response.from(find(id));
    }

    @Transactional
    public PrivacyRetentionDto.Response create(PrivacyRetentionDto.Request req) {
        PrivacyRetention r = PrivacyRetention.builder()
                .targetName(req.getTargetName().trim())
                .privacyFileId(req.getPrivacyFileId())
                .department(req.getDepartment())
                .infoItems(req.getInfoItems())
                .retentionPeriod(req.getRetentionPeriod())
                .legalBasis(req.getLegalBasis())
                .startDate(req.getStartDate())
                .expiryDate(req.getExpiryDate())
                .disposalDueDate(req.getDisposalDueDate())
                .extensionReason(req.getExtensionReason())
                .notifyEnabled(req.getNotifyEnabled() != null ? req.getNotifyEnabled() : true)
                .notifyDaysBefore(req.getNotifyDaysBefore() != null ? req.getNotifyDaysBefore() : 30)
                .status(parseStatus(req.getStatus()))
                .notes(req.getNotes())
                .build();
        PrivacyRetention saved = repo.save(r);
        auditLogService.log("CREATE", "PrivacyRetention", saved.getId(), saved.getTargetName());
        return PrivacyRetentionDto.Response.from(saved);
    }

    @Transactional
    public PrivacyRetentionDto.Response update(Long id, PrivacyRetentionDto.Request req) {
        PrivacyRetention r = find(id);
        if (req.getTargetName() != null && !req.getTargetName().isBlank()) r.setTargetName(req.getTargetName().trim());
        if (req.getPrivacyFileId() != null) r.setPrivacyFileId(req.getPrivacyFileId());
        if (req.getDepartment() != null) r.setDepartment(req.getDepartment());
        if (req.getInfoItems() != null) r.setInfoItems(req.getInfoItems());
        if (req.getRetentionPeriod() != null) r.setRetentionPeriod(req.getRetentionPeriod());
        if (req.getLegalBasis() != null) r.setLegalBasis(req.getLegalBasis());
        if (req.getStartDate() != null) r.setStartDate(req.getStartDate());
        if (req.getExpiryDate() != null) r.setExpiryDate(req.getExpiryDate());
        if (req.getDisposalDueDate() != null) r.setDisposalDueDate(req.getDisposalDueDate());
        if (req.getExtensionReason() != null) r.setExtensionReason(req.getExtensionReason());
        if (req.getNotifyEnabled() != null) r.setNotifyEnabled(req.getNotifyEnabled());
        if (req.getNotifyDaysBefore() != null) r.setNotifyDaysBefore(req.getNotifyDaysBefore());
        if (req.getStatus() != null) r.setStatus(parseStatus(req.getStatus()));
        if (req.getNotes() != null) r.setNotes(req.getNotes());
        auditLogService.log("UPDATE", "PrivacyRetention", r.getId(), r.getTargetName());
        return PrivacyRetentionDto.Response.from(r);
    }

    @Transactional
    public void delete(Long id) {
        PrivacyRetention r = find(id);
        repo.delete(r);
        auditLogService.log("DELETE", "PrivacyRetention", id, r.getTargetName());
    }

    private PrivacyRetention find(Long id) {
        return repo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("PrivacyRetention", id));
    }

    private PrivacyRetention.Status parseStatus(String s) {
        if (s == null || s.isBlank()) return PrivacyRetention.Status.ACTIVE;
        try {
            return PrivacyRetention.Status.valueOf(s);
        } catch (IllegalArgumentException e) {
            return PrivacyRetention.Status.ACTIVE;
        }
    }
}
