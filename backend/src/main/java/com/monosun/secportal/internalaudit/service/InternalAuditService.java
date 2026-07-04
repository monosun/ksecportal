package com.monosun.secportal.internalaudit.service;

import com.monosun.secportal.auth.entity.User;
import com.monosun.secportal.common.exception.BusinessException;
import com.monosun.secportal.common.exception.ResourceNotFoundException;
import com.monosun.secportal.common.service.FileStorageService;
import com.monosun.secportal.internalaudit.dto.InternalAuditDto;
import com.monosun.secportal.internalaudit.entity.*;
import com.monosun.secportal.internalaudit.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InternalAuditService {

    private final InternalAuditRepository auditRepo;
    private final AuditTargetRepository targetRepo;
    private final AuditItemRepository itemRepo;
    private final AuditFileRepository fileRepo;
    private final FileStorageService fileStorageService;

    @Transactional(readOnly = true)
    public List<Integer> getYears() {
        return auditRepo.findDistinctYears();
    }

    @Transactional(readOnly = true)
    public List<InternalAuditDto.AuditResponse> listByYear(int year) {
        return auditRepo.findByYearOrderByCreatedAtDesc(year)
                .stream().map(InternalAuditDto.AuditResponse::summary).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public InternalAuditDto.AuditResponse get(Long id) {
        return InternalAuditDto.AuditResponse.from(findAudit(id));
    }

    @Transactional
    public InternalAuditDto.AuditResponse create(InternalAuditDto.AuditRequest req, User user) {
        if (req.getTitle() == null || req.getTitle().isBlank()) throw new BusinessException("감사명을 입력하세요.");
        InternalAudit audit = InternalAudit.builder()
                .year(req.getYear() > 0 ? req.getYear() : java.time.Year.now().getValue())
                .title(req.getTitle().trim())
                .auditStart(req.getAuditStart())
                .auditEnd(req.getAuditEnd())
                .auditor(req.getAuditor())
                .status(parseStatus(req.getStatus()))
                .description(req.getDescription())
                .createdBy(user)
                .build();
        return InternalAuditDto.AuditResponse.summary(auditRepo.save(audit));
    }

    @Transactional
    public InternalAuditDto.AuditResponse update(Long id, InternalAuditDto.AuditRequest req) {
        InternalAudit audit = findAudit(id);
        if (req.getTitle() != null && !req.getTitle().isBlank()) audit.setTitle(req.getTitle().trim());
        if (req.getAuditStart() != null) audit.setAuditStart(req.getAuditStart());
        if (req.getAuditEnd() != null) audit.setAuditEnd(req.getAuditEnd());
        if (req.getAuditor() != null) audit.setAuditor(req.getAuditor());
        if (req.getStatus() != null) audit.setStatus(parseStatus(req.getStatus()));
        if (req.getDescription() != null) audit.setDescription(req.getDescription());
        return InternalAuditDto.AuditResponse.from(audit);
    }

    @Transactional
    public void delete(Long id) throws IOException {
        InternalAudit audit = findAudit(id);
        for (AuditFile f : audit.getFiles()) {
            if (f.getFilePath() != null) fileStorageService.delete(f.getFilePath());
        }
        auditRepo.delete(audit);
    }

    // ── Targets ──

    @Transactional
    public InternalAuditDto.TargetResponse addTarget(Long auditId, InternalAuditDto.TargetRequest req) {
        InternalAudit audit = findAudit(auditId);
        if (req.getTargetName() == null || req.getTargetName().isBlank())
            throw new BusinessException("점검대상명을 입력하세요.");
        AuditTarget t = AuditTarget.builder()
                .audit(audit)
                .targetName(req.getTargetName().trim())
                .targetType(req.getTargetType())
                .description(req.getDescription())
                .sortOrder(req.getSortOrder())
                .build();
        return InternalAuditDto.TargetResponse.from(targetRepo.save(t));
    }

    @Transactional
    public InternalAuditDto.TargetResponse updateTarget(Long targetId, InternalAuditDto.TargetRequest req) {
        AuditTarget t = targetRepo.findById(targetId)
                .orElseThrow(() -> new ResourceNotFoundException("AuditTarget", targetId));
        if (req.getTargetName() != null && !req.getTargetName().isBlank()) t.setTargetName(req.getTargetName().trim());
        if (req.getTargetType() != null) t.setTargetType(req.getTargetType());
        if (req.getDescription() != null) t.setDescription(req.getDescription());
        t.setSortOrder(req.getSortOrder());
        return InternalAuditDto.TargetResponse.from(t);
    }

    @Transactional
    public void deleteTarget(Long targetId) {
        AuditTarget t = targetRepo.findById(targetId)
                .orElseThrow(() -> new ResourceNotFoundException("AuditTarget", targetId));
        targetRepo.delete(t);
    }

    // ── Items ──

    @Transactional
    public InternalAuditDto.ItemResponse addItem(Long auditId, InternalAuditDto.ItemRequest req) {
        InternalAudit audit = findAudit(auditId);
        if (req.getItemName() == null || req.getItemName().isBlank())
            throw new BusinessException("점검항목명을 입력하세요.");
        AuditTarget target = null;
        if (req.getTargetId() != null) {
            target = targetRepo.findById(req.getTargetId())
                    .orElseThrow(() -> new ResourceNotFoundException("AuditTarget", req.getTargetId()));
        }
        AuditItem item = AuditItem.builder()
                .audit(audit)
                .target(target)
                .itemName(req.getItemName().trim())
                .checkMethod(req.getCheckMethod())
                .result(parseResult(req.getResult()))
                .finding(req.getFinding())
                .actionRequired(req.getActionRequired())
                .sortOrder(req.getSortOrder())
                .build();
        return InternalAuditDto.ItemResponse.from(itemRepo.save(item));
    }

    @Transactional
    public InternalAuditDto.ItemResponse updateItem(Long itemId, InternalAuditDto.ItemRequest req) {
        AuditItem item = itemRepo.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("AuditItem", itemId));
        if (req.getItemName() != null && !req.getItemName().isBlank()) item.setItemName(req.getItemName().trim());
        if (req.getCheckMethod() != null) item.setCheckMethod(req.getCheckMethod());
        if (req.getResult() != null) item.setResult(parseResult(req.getResult()));
        if (req.getFinding() != null) item.setFinding(req.getFinding());
        if (req.getActionRequired() != null) item.setActionRequired(req.getActionRequired());
        item.setSortOrder(req.getSortOrder());
        if (req.getTargetId() != null) {
            AuditTarget target = targetRepo.findById(req.getTargetId())
                    .orElseThrow(() -> new ResourceNotFoundException("AuditTarget", req.getTargetId()));
            item.setTarget(target);
        }
        return InternalAuditDto.ItemResponse.from(item);
    }

    @Transactional
    public void deleteItem(Long itemId) {
        AuditItem item = itemRepo.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("AuditItem", itemId));
        itemRepo.delete(item);
    }

    // ── Files ──

    @Transactional
    public InternalAuditDto.FileResponse addFile(Long auditId, String title,
                                                   MultipartFile file, User user) throws IOException {
        InternalAudit audit = findAudit(auditId);
        if (title == null || title.isBlank()) throw new BusinessException("파일 제목을 입력하세요.");
        AuditFile af = AuditFile.builder()
                .audit(audit)
                .title(title.trim())
                .uploader(user)
                .build();
        af = fileRepo.save(af);
        if (file != null && !file.isEmpty()) {
            String path = fileStorageService.store(file, "internal-audit/" + af.getId());
            af.setFilePath(path);
            af.setFileName(file.getOriginalFilename());
            af.setFileSize(file.getSize());
        }
        return InternalAuditDto.FileResponse.from(af);
    }

    @Transactional
    public void deleteFile(Long fileId) throws IOException {
        AuditFile af = fileRepo.findById(fileId)
                .orElseThrow(() -> new ResourceNotFoundException("AuditFile", fileId));
        if (af.getFilePath() != null) fileStorageService.delete(af.getFilePath());
        fileRepo.delete(af);
    }

    public org.springframework.core.io.Resource downloadFile(Long fileId) {
        AuditFile af = fileRepo.findById(fileId)
                .orElseThrow(() -> new ResourceNotFoundException("AuditFile", fileId));
        if (af.getFilePath() == null) throw new BusinessException("첨부파일이 없습니다.");
        return fileStorageService.load(af.getFilePath());
    }

    public AuditFile getFile(Long fileId) {
        return fileRepo.findById(fileId)
                .orElseThrow(() -> new ResourceNotFoundException("AuditFile", fileId));
    }

    private InternalAudit findAudit(Long id) {
        return auditRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("InternalAudit", id));
    }

    private InternalAudit.Status parseStatus(String s) {
        if (s == null) return InternalAudit.Status.PLANNED;
        try { return InternalAudit.Status.valueOf(s.toUpperCase()); }
        catch (IllegalArgumentException e) { return InternalAudit.Status.PLANNED; }
    }

    private AuditItem.Result parseResult(String r) {
        if (r == null || r.isBlank()) return null;
        try { return AuditItem.Result.valueOf(r.toUpperCase()); }
        catch (IllegalArgumentException e) { return null; }
    }
}
