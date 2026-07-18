package com.monosun.secportal.privacy.service;

import com.monosun.secportal.audit.service.AuditLogService;
import com.monosun.secportal.auth.entity.User;
import com.monosun.secportal.common.exception.BusinessException;
import com.monosun.secportal.common.exception.ResourceNotFoundException;
import com.monosun.secportal.common.service.FileStorageService;
import com.monosun.secportal.privacy.dto.PrivacyDpiaDto;
import com.monosun.secportal.privacy.dto.PrivacyDpiaFileDto;
import com.monosun.secportal.privacy.entity.PrivacyDpia;
import com.monosun.secportal.privacy.entity.PrivacyDpiaFile;
import com.monosun.secportal.privacy.repository.PrivacyDpiaFileRepository;
import com.monosun.secportal.privacy.repository.PrivacyDpiaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class PrivacyDpiaService {

    private static final Set<String> CATEGORIES = Set.of("REPORT", "IMPROVEMENT", "CHECKLIST", "OTHER");

    private final PrivacyDpiaRepository repo;
    private final PrivacyDpiaFileRepository fileRepo;
    private final FileStorageService fileStorageService;
    private final AuditLogService auditLogService;

    @Transactional(readOnly = true)
    public List<PrivacyDpiaDto.Response> listAll() {
        Map<Long, Long> counts = new HashMap<>();
        for (Object[] row : fileRepo.countGroupByDpia()) {
            counts.put((Long) row[0], (Long) row[1]);
        }
        return repo.findAllByOrderByAssessmentDateDesc().stream()
                .map(d -> PrivacyDpiaDto.Response.from(d, counts.getOrDefault(d.getId(), 0L)))
                .toList();
    }

    @Transactional(readOnly = true)
    public PrivacyDpiaDto.Response get(Long id) {
        PrivacyDpia d = find(id);
        return PrivacyDpiaDto.Response.from(d, fileRepo.findByDpiaIdOrderByIdDesc(id).size());
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
    public void delete(Long id) throws IOException {
        PrivacyDpia d = find(id);
        // 첨부파일은 별도 테이블이므로 실제 파일과 레코드를 먼저 정리한다.
        List<PrivacyDpiaFile> files = fileRepo.findByDpiaIdOrderByIdDesc(id);
        for (PrivacyDpiaFile f : files) {
            if (f.getFilePath() != null) fileStorageService.delete(f.getFilePath());
        }
        fileRepo.deleteAll(files);
        repo.delete(d);
        auditLogService.log("DELETE", "PrivacyDpia", id, d.getTitle());
    }

    // ── 첨부파일 (보고서 등) ─────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<PrivacyDpiaFileDto.Response> listFiles(Long dpiaId) {
        find(dpiaId);
        return fileRepo.findByDpiaIdOrderByIdDesc(dpiaId).stream()
                .map(PrivacyDpiaFileDto.Response::from)
                .toList();
    }

    @Transactional
    public PrivacyDpiaFileDto.Response addFile(
            Long dpiaId, PrivacyDpiaFileDto.CreateRequest req, MultipartFile file, User user) throws IOException {
        PrivacyDpia dpia = find(dpiaId);
        if (file == null || file.isEmpty()) throw new BusinessException("첨부할 파일을 선택하세요.");

        String path = fileStorageService.store(file, "privacy-dpia/" + dpiaId);
        String title = req != null && req.getTitle() != null && !req.getTitle().isBlank()
                ? req.getTitle().trim()
                : file.getOriginalFilename();

        PrivacyDpiaFile saved = fileRepo.save(PrivacyDpiaFile.builder()
                .dpia(dpia)
                .category(parseCategory(req != null ? req.getCategory() : null))
                .title(title)
                .fileName(file.getOriginalFilename())
                .filePath(path)
                .fileSize(file.getSize())
                .uploader(user)
                .build());

        auditLogService.log("CREATE", "PrivacyDpiaFile", saved.getId(),
                dpia.getTitle() + " — " + title);
        return PrivacyDpiaFileDto.Response.from(saved);
    }

    @Transactional
    public void deleteFile(Long fileId) throws IOException {
        PrivacyDpiaFile f = findFile(fileId);
        if (f.getFilePath() != null) fileStorageService.delete(f.getFilePath());
        String title = f.getTitle();
        Long dpiaId = f.getDpia().getId();
        fileRepo.delete(f);
        auditLogService.log("DELETE", "PrivacyDpiaFile", fileId, "DPIA #" + dpiaId + " — " + title);
    }

    @Transactional(readOnly = true)
    public PrivacyDpiaFile getFile(Long fileId) {
        return findFile(fileId);
    }

    @Transactional(readOnly = true)
    public Resource downloadFile(Long fileId) {
        PrivacyDpiaFile f = findFile(fileId);
        if (f.getFilePath() == null) throw new BusinessException("첨부파일이 없습니다.");
        return fileStorageService.load(f.getFilePath());
    }

    private PrivacyDpiaFile findFile(Long fileId) {
        return fileRepo.findById(fileId)
                .orElseThrow(() -> new ResourceNotFoundException("PrivacyDpiaFile", fileId));
    }

    private String parseCategory(String s) {
        if (s == null || s.isBlank()) return "REPORT";
        String v = s.trim().toUpperCase();
        return CATEGORIES.contains(v) ? v : "OTHER";
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
