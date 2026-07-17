package com.monosun.secportal.privacy.service;

import com.monosun.secportal.audit.service.AuditLogService;
import com.monosun.secportal.common.exception.ResourceNotFoundException;
import com.monosun.secportal.privacy.dto.PrivacySafeguardDto;
import com.monosun.secportal.privacy.entity.PrivacySafeguard;
import com.monosun.secportal.privacy.repository.PrivacySafeguardRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PrivacySafeguardService {

    private final PrivacySafeguardRepository repo;
    private final AuditLogService auditLogService;

    @Transactional(readOnly = true)
    public List<PrivacySafeguardDto.Response> listAll() {
        return repo.findAllByOrderByCheckDateDesc().stream()
                .map(PrivacySafeguardDto.Response::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public PrivacySafeguardDto.Response get(Long id) {
        return PrivacySafeguardDto.Response.from(find(id));
    }

    @Transactional
    public PrivacySafeguardDto.Response create(PrivacySafeguardDto.Request req) {
        PrivacySafeguard s = PrivacySafeguard.builder()
                .safeguardType(parseType(req.getSafeguardType()))
                .title(req.getTitle().trim())
                .targetSystem(req.getTargetSystem())
                .department(req.getDepartment())
                .checkDate(req.getCheckDate())
                .performer(req.getPerformer())
                .targetCount(req.getTargetCount())
                .actionCount(req.getActionCount())
                .result(req.getResult())
                .findings(req.getFindings())
                .improvement(req.getImprovement())
                .nextCheckDate(req.getNextCheckDate())
                .status(parseStatus(req.getStatus()))
                .notes(req.getNotes())
                .build();
        PrivacySafeguard saved = repo.save(s);
        auditLogService.log("CREATE", "PrivacySafeguard", saved.getId(), saved.getTitle());
        return PrivacySafeguardDto.Response.from(saved);
    }

    @Transactional
    public PrivacySafeguardDto.Response update(Long id, PrivacySafeguardDto.Request req) {
        PrivacySafeguard s = find(id);
        if (req.getSafeguardType() != null) s.setSafeguardType(parseType(req.getSafeguardType()));
        if (req.getTitle() != null && !req.getTitle().isBlank()) s.setTitle(req.getTitle().trim());
        if (req.getTargetSystem() != null) s.setTargetSystem(req.getTargetSystem());
        if (req.getDepartment() != null) s.setDepartment(req.getDepartment());
        if (req.getCheckDate() != null) s.setCheckDate(req.getCheckDate());
        if (req.getPerformer() != null) s.setPerformer(req.getPerformer());
        if (req.getTargetCount() != null) s.setTargetCount(req.getTargetCount());
        if (req.getActionCount() != null) s.setActionCount(req.getActionCount());
        if (req.getResult() != null) s.setResult(req.getResult());
        if (req.getFindings() != null) s.setFindings(req.getFindings());
        if (req.getImprovement() != null) s.setImprovement(req.getImprovement());
        if (req.getNextCheckDate() != null) s.setNextCheckDate(req.getNextCheckDate());
        if (req.getStatus() != null) s.setStatus(parseStatus(req.getStatus()));
        if (req.getNotes() != null) s.setNotes(req.getNotes());
        auditLogService.log("UPDATE", "PrivacySafeguard", s.getId(), s.getTitle());
        return PrivacySafeguardDto.Response.from(s);
    }

    @Transactional
    public void delete(Long id) {
        PrivacySafeguard s = find(id);
        repo.delete(s);
        auditLogService.log("DELETE", "PrivacySafeguard", id, s.getTitle());
    }

    private PrivacySafeguard find(Long id) {
        return repo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("PrivacySafeguard", id));
    }

    private PrivacySafeguard.SafeguardType parseType(String s) {
        if (s == null || s.isBlank()) return PrivacySafeguard.SafeguardType.ACCESS_REVIEW;
        try {
            return PrivacySafeguard.SafeguardType.valueOf(s);
        } catch (IllegalArgumentException e) {
            return PrivacySafeguard.SafeguardType.ACCESS_REVIEW;
        }
    }

    private PrivacySafeguard.Status parseStatus(String s) {
        if (s == null || s.isBlank()) return PrivacySafeguard.Status.PLANNED;
        try {
            return PrivacySafeguard.Status.valueOf(s);
        } catch (IllegalArgumentException e) {
            return PrivacySafeguard.Status.PLANNED;
        }
    }
}
