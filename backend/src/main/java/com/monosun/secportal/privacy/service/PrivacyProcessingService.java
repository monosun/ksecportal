package com.monosun.secportal.privacy.service;

import com.monosun.secportal.audit.service.AuditLogService;
import com.monosun.secportal.common.exception.ResourceNotFoundException;
import com.monosun.secportal.privacy.dto.PrivacyProcessingDto;
import com.monosun.secportal.privacy.entity.PrivacyProcessing;
import com.monosun.secportal.privacy.repository.PrivacyProcessingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PrivacyProcessingService {

    private final PrivacyProcessingRepository repo;
    private final AuditLogService auditLogService;

    @Transactional(readOnly = true)
    public List<PrivacyProcessingDto.Response> listAll() {
        return repo.findAllByOrderByNameAsc().stream()
                .map(PrivacyProcessingDto.Response::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public PrivacyProcessingDto.Response get(Long id) {
        return PrivacyProcessingDto.Response.from(find(id));
    }

    @Transactional
    public PrivacyProcessingDto.Response create(PrivacyProcessingDto.Request req) {
        PrivacyProcessing p = PrivacyProcessing.builder()
                .name(req.getName().trim())
                .department(req.getDepartment())
                .purpose(req.getPurpose())
                .infoItems(req.getInfoItems())
                .retentionPeriod(req.getRetentionPeriod())
                .legalBasis(req.getLegalBasis())
                .systemName(req.getSystemName())
                .workflow(req.getWorkflow())
                .lifecycle(req.getLifecycle())
                .status(parseStatus(req.getStatus()))
                .notes(req.getNotes())
                .build();
        PrivacyProcessing saved = repo.save(p);
        auditLogService.log("CREATE", "PrivacyProcessing", saved.getId(), saved.getName());
        return PrivacyProcessingDto.Response.from(saved);
    }

    @Transactional
    public PrivacyProcessingDto.Response update(Long id, PrivacyProcessingDto.Request req) {
        PrivacyProcessing p = find(id);
        if (req.getName() != null && !req.getName().isBlank()) p.setName(req.getName().trim());
        if (req.getDepartment() != null) p.setDepartment(req.getDepartment());
        if (req.getPurpose() != null) p.setPurpose(req.getPurpose());
        if (req.getInfoItems() != null) p.setInfoItems(req.getInfoItems());
        if (req.getRetentionPeriod() != null) p.setRetentionPeriod(req.getRetentionPeriod());
        if (req.getLegalBasis() != null) p.setLegalBasis(req.getLegalBasis());
        if (req.getSystemName() != null) p.setSystemName(req.getSystemName());
        if (req.getWorkflow() != null) p.setWorkflow(req.getWorkflow());
        if (req.getLifecycle() != null) p.setLifecycle(req.getLifecycle());
        if (req.getStatus() != null) p.setStatus(parseStatus(req.getStatus()));
        if (req.getNotes() != null) p.setNotes(req.getNotes());
        auditLogService.log("UPDATE", "PrivacyProcessing", p.getId(), p.getName());
        return PrivacyProcessingDto.Response.from(p);
    }

    @Transactional
    public void delete(Long id) {
        PrivacyProcessing p = find(id);
        repo.delete(p);
        auditLogService.log("DELETE", "PrivacyProcessing", id, p.getName());
    }

    private PrivacyProcessing find(Long id) {
        return repo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("PrivacyProcessing", id));
    }

    private PrivacyProcessing.Status parseStatus(String s) {
        if (s == null || s.isBlank()) return PrivacyProcessing.Status.ACTIVE;
        try {
            return PrivacyProcessing.Status.valueOf(s);
        } catch (IllegalArgumentException e) {
            return PrivacyProcessing.Status.ACTIVE;
        }
    }
}
