package com.monosun.secportal.privacy.service;

import com.monosun.secportal.audit.service.AuditLogService;
import com.monosun.secportal.common.exception.ResourceNotFoundException;
import com.monosun.secportal.privacy.dto.PrivacyDisposalDto;
import com.monosun.secportal.privacy.entity.PrivacyDisposal;
import com.monosun.secportal.privacy.entity.PrivacyRetention;
import com.monosun.secportal.privacy.repository.PrivacyDisposalRepository;
import com.monosun.secportal.privacy.repository.PrivacyRetentionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PrivacyDisposalService {

    private final PrivacyDisposalRepository repo;
    private final PrivacyRetentionRepository retentionRepo;
    private final AuditLogService auditLogService;

    @Transactional(readOnly = true)
    public List<PrivacyDisposalDto.Response> listAll() {
        return repo.findAllByOrderByPlannedDateDesc().stream()
                .map(PrivacyDisposalDto.Response::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public PrivacyDisposalDto.Response get(Long id) {
        return PrivacyDisposalDto.Response.from(find(id));
    }

    @Transactional
    public PrivacyDisposalDto.Response create(PrivacyDisposalDto.Request req) {
        PrivacyDisposal d = PrivacyDisposal.builder()
                .title(req.getTitle().trim())
                .targetName(req.getTargetName().trim())
                .retentionId(req.getRetentionId())
                .department(req.getDepartment())
                .infoItems(req.getInfoItems())
                .recordCount(req.getRecordCount())
                .method(req.getMethod())
                .plannedDate(req.getPlannedDate())
                .disposalDate(req.getDisposalDate())
                .performer(req.getPerformer())
                .approver(req.getApprover())
                .approvalStatus(parseApproval(req.getApprovalStatus()))
                .approvedDate(req.getApprovedDate())
                .evidence(req.getEvidence())
                .status(parseStatus(req.getStatus()))
                .notes(req.getNotes())
                .build();
        PrivacyDisposal saved = repo.save(d);
        syncRetention(saved);
        auditLogService.log("CREATE", "PrivacyDisposal", saved.getId(), saved.getTitle());
        return PrivacyDisposalDto.Response.from(saved);
    }

    @Transactional
    public PrivacyDisposalDto.Response update(Long id, PrivacyDisposalDto.Request req) {
        PrivacyDisposal d = find(id);
        if (req.getTitle() != null && !req.getTitle().isBlank()) d.setTitle(req.getTitle().trim());
        if (req.getTargetName() != null && !req.getTargetName().isBlank()) d.setTargetName(req.getTargetName().trim());
        if (req.getRetentionId() != null) d.setRetentionId(req.getRetentionId());
        if (req.getDepartment() != null) d.setDepartment(req.getDepartment());
        if (req.getInfoItems() != null) d.setInfoItems(req.getInfoItems());
        if (req.getRecordCount() != null) d.setRecordCount(req.getRecordCount());
        if (req.getMethod() != null) d.setMethod(req.getMethod());
        if (req.getPlannedDate() != null) d.setPlannedDate(req.getPlannedDate());
        if (req.getDisposalDate() != null) d.setDisposalDate(req.getDisposalDate());
        if (req.getPerformer() != null) d.setPerformer(req.getPerformer());
        if (req.getApprover() != null) d.setApprover(req.getApprover());
        if (req.getApprovalStatus() != null) d.setApprovalStatus(parseApproval(req.getApprovalStatus()));
        if (req.getApprovedDate() != null) d.setApprovedDate(req.getApprovedDate());
        if (req.getEvidence() != null) d.setEvidence(req.getEvidence());
        if (req.getStatus() != null) d.setStatus(parseStatus(req.getStatus()));
        if (req.getNotes() != null) d.setNotes(req.getNotes());
        syncRetention(d);
        auditLogService.log("UPDATE", "PrivacyDisposal", d.getId(), d.getTitle());
        return PrivacyDisposalDto.Response.from(d);
    }

    @Transactional
    public void delete(Long id) {
        PrivacyDisposal d = find(id);
        repo.delete(d);
        auditLogService.log("DELETE", "PrivacyDisposal", id, d.getTitle());
    }

    /**
     * 파기가 완료되면 연계된 보유기간 항목을 파기완료(DISPOSED)로 전이시킨다.
     * 보유기간 관리와 파기관리가 따로 놀지 않도록 하는 연계 지점.
     */
    private void syncRetention(PrivacyDisposal d) {
        if (d.getRetentionId() == null) return;
        if (d.getStatus() != PrivacyDisposal.Status.COMPLETED) return;
        retentionRepo.findById(d.getRetentionId()).ifPresent(r -> {
            if (r.getStatus() != PrivacyRetention.Status.DISPOSED) {
                r.setStatus(PrivacyRetention.Status.DISPOSED);
                auditLogService.log("UPDATE", "PrivacyRetention", r.getId(),
                        "파기 완료로 보유기간 상태 전이 (파기계획 #" + d.getId() + ")");
            }
        });
    }

    private PrivacyDisposal find(Long id) {
        return repo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("PrivacyDisposal", id));
    }

    private PrivacyDisposal.ApprovalStatus parseApproval(String s) {
        if (s == null || s.isBlank()) return PrivacyDisposal.ApprovalStatus.PENDING;
        try {
            return PrivacyDisposal.ApprovalStatus.valueOf(s);
        } catch (IllegalArgumentException e) {
            return PrivacyDisposal.ApprovalStatus.PENDING;
        }
    }

    private PrivacyDisposal.Status parseStatus(String s) {
        if (s == null || s.isBlank()) return PrivacyDisposal.Status.PLANNED;
        try {
            return PrivacyDisposal.Status.valueOf(s);
        } catch (IllegalArgumentException e) {
            return PrivacyDisposal.Status.PLANNED;
        }
    }
}
