package com.monosun.secportal.privacy.service;

import com.monosun.secportal.audit.service.AuditLogService;
import com.monosun.secportal.common.exception.ResourceNotFoundException;
import com.monosun.secportal.privacy.dto.PrivacyConsentDto;
import com.monosun.secportal.privacy.entity.PrivacyConsent;
import com.monosun.secportal.privacy.repository.PrivacyConsentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PrivacyConsentService {

    private final PrivacyConsentRepository repo;
    private final AuditLogService auditLogService;

    @Transactional(readOnly = true)
    public List<PrivacyConsentDto.Response> listAll() {
        return repo.findAllByOrderByTitleAscVersionDesc().stream()
                .map(PrivacyConsentDto.Response::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public PrivacyConsentDto.Response get(Long id) {
        return PrivacyConsentDto.Response.from(find(id));
    }

    @Transactional
    public PrivacyConsentDto.Response create(PrivacyConsentDto.Request req) {
        PrivacyConsent c = PrivacyConsent.builder()
                .title(req.getTitle().trim())
                .version(req.getVersion() != null && !req.getVersion().isBlank() ? req.getVersion() : "1.0")
                .consentType(parseType(req.getConsentType()))
                .infoItems(req.getInfoItems())
                .purpose(req.getPurpose())
                .legalBasis(req.getLegalBasis())
                .retentionPeriod(req.getRetentionPeriod())
                .channel(req.getChannel())
                .effectiveDate(req.getEffectiveDate())
                .content(req.getContent())
                .status(parseStatus(req.getStatus()))
                .notes(req.getNotes())
                .build();
        PrivacyConsent saved = repo.save(c);
        auditLogService.log("CREATE", "PrivacyConsent", saved.getId(),
                saved.getTitle() + " v" + saved.getVersion());
        return PrivacyConsentDto.Response.from(saved);
    }

    @Transactional
    public PrivacyConsentDto.Response update(Long id, PrivacyConsentDto.Request req) {
        PrivacyConsent c = find(id);
        if (req.getTitle() != null && !req.getTitle().isBlank()) c.setTitle(req.getTitle().trim());
        if (req.getVersion() != null && !req.getVersion().isBlank()) c.setVersion(req.getVersion());
        if (req.getConsentType() != null) c.setConsentType(parseType(req.getConsentType()));
        if (req.getInfoItems() != null) c.setInfoItems(req.getInfoItems());
        if (req.getPurpose() != null) c.setPurpose(req.getPurpose());
        if (req.getLegalBasis() != null) c.setLegalBasis(req.getLegalBasis());
        if (req.getRetentionPeriod() != null) c.setRetentionPeriod(req.getRetentionPeriod());
        if (req.getChannel() != null) c.setChannel(req.getChannel());
        if (req.getEffectiveDate() != null) c.setEffectiveDate(req.getEffectiveDate());
        if (req.getContent() != null) c.setContent(req.getContent());
        if (req.getStatus() != null) c.setStatus(parseStatus(req.getStatus()));
        if (req.getNotes() != null) c.setNotes(req.getNotes());
        auditLogService.log("UPDATE", "PrivacyConsent", c.getId(),
                c.getTitle() + " v" + c.getVersion());
        return PrivacyConsentDto.Response.from(c);
    }

    /** 동의서 개정 — 기존 내용을 복사해 새 버전으로 등록한다. */
    @Transactional
    public PrivacyConsentDto.Response newVersion(Long id, String newVersion) {
        PrivacyConsent src = find(id);
        PrivacyConsent copy = PrivacyConsent.builder()
                .title(src.getTitle())
                .version(newVersion)
                .consentType(src.getConsentType())
                .infoItems(src.getInfoItems())
                .purpose(src.getPurpose())
                .legalBasis(src.getLegalBasis())
                .retentionPeriod(src.getRetentionPeriod())
                .channel(src.getChannel())
                .effectiveDate(src.getEffectiveDate())
                .content(src.getContent())
                .status(PrivacyConsent.Status.DRAFT)
                .notes(src.getNotes())
                .build();
        PrivacyConsent saved = repo.save(copy);
        auditLogService.log("CREATE", "PrivacyConsent", saved.getId(),
                "개정본 생성: " + saved.getTitle() + " v" + saved.getVersion());
        return PrivacyConsentDto.Response.from(saved);
    }

    @Transactional
    public void delete(Long id) {
        PrivacyConsent c = find(id);
        repo.delete(c);
        auditLogService.log("DELETE", "PrivacyConsent", id, c.getTitle() + " v" + c.getVersion());
    }

    private PrivacyConsent find(Long id) {
        return repo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("PrivacyConsent", id));
    }

    private PrivacyConsent.ConsentType parseType(String s) {
        if (s == null || s.isBlank()) return PrivacyConsent.ConsentType.REQUIRED;
        try {
            return PrivacyConsent.ConsentType.valueOf(s);
        } catch (IllegalArgumentException e) {
            return PrivacyConsent.ConsentType.REQUIRED;
        }
    }

    private PrivacyConsent.Status parseStatus(String s) {
        if (s == null || s.isBlank()) return PrivacyConsent.Status.DRAFT;
        try {
            return PrivacyConsent.Status.valueOf(s);
        } catch (IllegalArgumentException e) {
            return PrivacyConsent.Status.DRAFT;
        }
    }
}
