package com.monosun.secportal.privacy.service;

import com.monosun.secportal.audit.service.AuditLogService;
import com.monosun.secportal.common.exception.ResourceNotFoundException;
import com.monosun.secportal.privacy.dto.PrivacyProvisionDto;
import com.monosun.secportal.privacy.entity.PrivacyProvision;
import com.monosun.secportal.privacy.repository.PrivacyProvisionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PrivacyProvisionService {

    private final PrivacyProvisionRepository repo;
    private final AuditLogService auditLogService;

    @Transactional(readOnly = true)
    public List<PrivacyProvisionDto.Response> listAll() {
        return repo.findAllByOrderByRecipientAsc().stream()
                .map(PrivacyProvisionDto.Response::from)
                .toList();
    }

    /** 처리업무에 연계된 제공 목록 */
    @Transactional(readOnly = true)
    public List<PrivacyProvisionDto.Response> listByProcessing(Long processingId) {
        return repo.findByProcessingIdOrderByRecipientAsc(processingId).stream()
                .map(PrivacyProvisionDto.Response::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public PrivacyProvisionDto.Response get(Long id) {
        return PrivacyProvisionDto.Response.from(find(id));
    }

    @Transactional
    public PrivacyProvisionDto.Response create(PrivacyProvisionDto.Request req) {
        PrivacyProvision p = PrivacyProvision.builder()
                .provisionType(parseType(req.getProvisionType()))
                .recipient(req.getRecipient().trim())
                .processingId(req.getProcessingId())
                .country(req.getCountry())
                .infoItems(req.getInfoItems())
                .purpose(req.getPurpose())
                .legalBasis(req.getLegalBasis())
                .retentionPeriod(req.getRetentionPeriod())
                .method(req.getMethod())
                .contractInfo(req.getContractInfo())
                .contractStart(req.getContractStart())
                .contractEnd(req.getContractEnd())
                .provisionDate(req.getProvisionDate())
                .status(parseStatus(req.getStatus()))
                .notes(req.getNotes())
                .build();
        PrivacyProvision saved = repo.save(p);
        auditLogService.log("CREATE", "PrivacyProvision", saved.getId(), saved.getRecipient());
        return PrivacyProvisionDto.Response.from(saved);
    }

    @Transactional
    public PrivacyProvisionDto.Response update(Long id, PrivacyProvisionDto.Request req) {
        PrivacyProvision p = find(id);
        if (req.getProvisionType() != null) p.setProvisionType(parseType(req.getProvisionType()));
        if (req.getRecipient() != null && !req.getRecipient().isBlank()) p.setRecipient(req.getRecipient().trim());
        if (req.getProcessingId() != null) p.setProcessingId(req.getProcessingId());
        if (req.getCountry() != null) p.setCountry(req.getCountry());
        if (req.getInfoItems() != null) p.setInfoItems(req.getInfoItems());
        if (req.getPurpose() != null) p.setPurpose(req.getPurpose());
        if (req.getLegalBasis() != null) p.setLegalBasis(req.getLegalBasis());
        if (req.getRetentionPeriod() != null) p.setRetentionPeriod(req.getRetentionPeriod());
        if (req.getMethod() != null) p.setMethod(req.getMethod());
        if (req.getContractInfo() != null) p.setContractInfo(req.getContractInfo());
        if (req.getContractStart() != null) p.setContractStart(req.getContractStart());
        if (req.getContractEnd() != null) p.setContractEnd(req.getContractEnd());
        if (req.getProvisionDate() != null) p.setProvisionDate(req.getProvisionDate());
        if (req.getStatus() != null) p.setStatus(parseStatus(req.getStatus()));
        if (req.getNotes() != null) p.setNotes(req.getNotes());
        auditLogService.log("UPDATE", "PrivacyProvision", p.getId(), p.getRecipient());
        return PrivacyProvisionDto.Response.from(p);
    }

    @Transactional
    public void delete(Long id) {
        PrivacyProvision p = find(id);
        repo.delete(p);
        auditLogService.log("DELETE", "PrivacyProvision", id, p.getRecipient());
    }

    private PrivacyProvision find(Long id) {
        return repo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("PrivacyProvision", id));
    }

    private PrivacyProvision.ProvisionType parseType(String s) {
        if (s == null || s.isBlank()) return PrivacyProvision.ProvisionType.THIRD_PARTY;
        try {
            return PrivacyProvision.ProvisionType.valueOf(s);
        } catch (IllegalArgumentException e) {
            return PrivacyProvision.ProvisionType.THIRD_PARTY;
        }
    }

    private PrivacyProvision.Status parseStatus(String s) {
        if (s == null || s.isBlank()) return PrivacyProvision.Status.ACTIVE;
        try {
            return PrivacyProvision.Status.valueOf(s);
        } catch (IllegalArgumentException e) {
            return PrivacyProvision.Status.ACTIVE;
        }
    }
}
