package com.monosun.secportal.privacy.service;

import com.monosun.secportal.audit.service.AuditLogService;
import com.monosun.secportal.common.exception.ResourceNotFoundException;
import com.monosun.secportal.privacy.dto.PrivacyFileDto;
import com.monosun.secportal.privacy.entity.PrivacyFile;
import com.monosun.secportal.privacy.repository.PrivacyFileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PrivacyFileService {

    private final PrivacyFileRepository repo;
    private final AuditLogService auditLogService;

    @Transactional(readOnly = true)
    public List<PrivacyFileDto.Response> listAll() {
        return repo.findAllByOrderByNameAsc().stream()
                .map(PrivacyFileDto.Response::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public PrivacyFileDto.Response get(Long id) {
        return PrivacyFileDto.Response.from(find(id));
    }

    @Transactional
    public PrivacyFileDto.Response create(PrivacyFileDto.Request req) {
        PrivacyFile f = PrivacyFile.builder()
                .name(req.getName().trim())
                .department(req.getDepartment())
                .systemName(req.getSystemName())
                .dbTable(req.getDbTable())
                .infoItems(req.getInfoItems())
                .retentionPeriod(req.getRetentionPeriod())
                .sensitiveInfo(req.getSensitiveInfo() != null ? req.getSensitiveInfo() : false)
                .uniqueIdentifier(req.getUniqueIdentifier() != null ? req.getUniqueIdentifier() : false)
                .recordCount(req.getRecordCount())
                .status(parseStatus(req.getStatus()))
                .notes(req.getNotes())
                .build();
        PrivacyFile saved = repo.save(f);
        auditLogService.log("CREATE", "PrivacyFile", saved.getId(), saved.getName());
        return PrivacyFileDto.Response.from(saved);
    }

    @Transactional
    public PrivacyFileDto.Response update(Long id, PrivacyFileDto.Request req) {
        PrivacyFile f = find(id);
        if (req.getName() != null && !req.getName().isBlank()) f.setName(req.getName().trim());
        if (req.getDepartment() != null) f.setDepartment(req.getDepartment());
        if (req.getSystemName() != null) f.setSystemName(req.getSystemName());
        if (req.getDbTable() != null) f.setDbTable(req.getDbTable());
        if (req.getInfoItems() != null) f.setInfoItems(req.getInfoItems());
        if (req.getRetentionPeriod() != null) f.setRetentionPeriod(req.getRetentionPeriod());
        if (req.getSensitiveInfo() != null) f.setSensitiveInfo(req.getSensitiveInfo());
        if (req.getUniqueIdentifier() != null) f.setUniqueIdentifier(req.getUniqueIdentifier());
        if (req.getRecordCount() != null) f.setRecordCount(req.getRecordCount());
        if (req.getStatus() != null) f.setStatus(parseStatus(req.getStatus()));
        if (req.getNotes() != null) f.setNotes(req.getNotes());
        auditLogService.log("UPDATE", "PrivacyFile", f.getId(), f.getName());
        return PrivacyFileDto.Response.from(f);
    }

    @Transactional
    public void delete(Long id) {
        PrivacyFile f = find(id);
        repo.delete(f);
        auditLogService.log("DELETE", "PrivacyFile", id, f.getName());
    }

    private PrivacyFile find(Long id) {
        return repo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("PrivacyFile", id));
    }

    private PrivacyFile.Status parseStatus(String s) {
        if (s == null || s.isBlank()) return PrivacyFile.Status.ACTIVE;
        try {
            return PrivacyFile.Status.valueOf(s);
        } catch (IllegalArgumentException e) {
            return PrivacyFile.Status.ACTIVE;
        }
    }
}
