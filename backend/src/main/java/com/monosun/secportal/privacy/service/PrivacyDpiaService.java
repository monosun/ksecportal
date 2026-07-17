package com.monosun.secportal.privacy.service;

import com.monosun.secportal.audit.service.AuditLogService;
import com.monosun.secportal.common.exception.ResourceNotFoundException;
import com.monosun.secportal.privacy.dto.PrivacyDpiaDto;
import com.monosun.secportal.privacy.entity.PrivacyDpia;
import com.monosun.secportal.privacy.repository.PrivacyDpiaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PrivacyDpiaService {

    private final PrivacyDpiaRepository repo;
    private final AuditLogService auditLogService;

    @Transactional(readOnly = true)
    public List<PrivacyDpiaDto.Response> listAll() {
        return repo.findAllByOrderByAssessmentDateDesc().stream()
                .map(PrivacyDpiaDto.Response::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public PrivacyDpiaDto.Response get(Long id) {
        return PrivacyDpiaDto.Response.from(find(id));
    }

    @Transactional
    public PrivacyDpiaDto.Response create(PrivacyDpiaDto.Request req) {
        PrivacyDpia d = PrivacyDpia.builder()
                .title(req.getTitle().trim())
                .targetSystem(req.getTargetSystem())
                .department(req.getDepartment())
                .infoItems(req.getInfoItems())
                .subjectCount(req.getSubjectCount())
                .assessmentDate(req.getAssessmentDate())
                .assessor(req.getAssessor())
                .checklist(req.getChecklist())
                .riskLevel(parseRisk(req.getRiskLevel()))
                .improvementPlan(req.getImprovementPlan())
                .improvementDueDate(req.getImprovementDueDate())
                .completionReport(req.getCompletionReport())
                .completedDate(req.getCompletedDate())
                .status(parseStatus(req.getStatus()))
                .notes(req.getNotes())
                .build();
        PrivacyDpia saved = repo.save(d);
        auditLogService.log("CREATE", "PrivacyDpia", saved.getId(), saved.getTitle());
        return PrivacyDpiaDto.Response.from(saved);
    }

    @Transactional
    public PrivacyDpiaDto.Response update(Long id, PrivacyDpiaDto.Request req) {
        PrivacyDpia d = find(id);
        if (req.getTitle() != null && !req.getTitle().isBlank()) d.setTitle(req.getTitle().trim());
        if (req.getTargetSystem() != null) d.setTargetSystem(req.getTargetSystem());
        if (req.getDepartment() != null) d.setDepartment(req.getDepartment());
        if (req.getInfoItems() != null) d.setInfoItems(req.getInfoItems());
        if (req.getSubjectCount() != null) d.setSubjectCount(req.getSubjectCount());
        if (req.getAssessmentDate() != null) d.setAssessmentDate(req.getAssessmentDate());
        if (req.getAssessor() != null) d.setAssessor(req.getAssessor());
        if (req.getChecklist() != null) d.setChecklist(req.getChecklist());
        if (req.getRiskLevel() != null) d.setRiskLevel(parseRisk(req.getRiskLevel()));
        if (req.getImprovementPlan() != null) d.setImprovementPlan(req.getImprovementPlan());
        if (req.getImprovementDueDate() != null) d.setImprovementDueDate(req.getImprovementDueDate());
        if (req.getCompletionReport() != null) d.setCompletionReport(req.getCompletionReport());
        if (req.getCompletedDate() != null) d.setCompletedDate(req.getCompletedDate());
        if (req.getStatus() != null) d.setStatus(parseStatus(req.getStatus()));
        if (req.getNotes() != null) d.setNotes(req.getNotes());
        auditLogService.log("UPDATE", "PrivacyDpia", d.getId(), d.getTitle());
        return PrivacyDpiaDto.Response.from(d);
    }

    @Transactional
    public void delete(Long id) {
        PrivacyDpia d = find(id);
        repo.delete(d);
        auditLogService.log("DELETE", "PrivacyDpia", id, d.getTitle());
    }

    private PrivacyDpia find(Long id) {
        return repo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("PrivacyDpia", id));
    }

    private PrivacyDpia.RiskLevel parseRisk(String s) {
        if (s == null || s.isBlank()) return PrivacyDpia.RiskLevel.MEDIUM;
        try {
            return PrivacyDpia.RiskLevel.valueOf(s);
        } catch (IllegalArgumentException e) {
            return PrivacyDpia.RiskLevel.MEDIUM;
        }
    }

    private PrivacyDpia.Status parseStatus(String s) {
        if (s == null || s.isBlank()) return PrivacyDpia.Status.PLANNED;
        try {
            return PrivacyDpia.Status.valueOf(s);
        } catch (IllegalArgumentException e) {
            return PrivacyDpia.Status.PLANNED;
        }
    }
}
