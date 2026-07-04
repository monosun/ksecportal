package com.monosun.secportal.secfinding.service;

import com.monosun.secportal.auth.entity.User;
import com.monosun.secportal.common.exception.BusinessException;
import com.monosun.secportal.common.exception.ResourceNotFoundException;
import com.monosun.secportal.common.service.FileStorageService;
import com.monosun.secportal.secfinding.dto.SecurityFindingDto;
import com.monosun.secportal.secfinding.entity.SecurityFinding;
import com.monosun.secportal.secfinding.repository.SecurityFindingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SecurityFindingService {

    private final SecurityFindingRepository repo;
    private final FileStorageService fileStorageService;

    @Transactional(readOnly = true)
    public List<Integer> getYears() {
        return repo.findDistinctYears();
    }

    @Transactional(readOnly = true)
    public Page<SecurityFindingDto.Response> list(Integer year, String status, String riskLevel,
                                                   String auditType, String keyword, int page, int size) {
        PageRequest pr = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return repo.findWithFilters(
                year,
                parseStatus(status),
                parseRiskLevel(riskLevel),
                parseAuditType(auditType),
                (keyword == null || keyword.isBlank()) ? null : keyword.trim(),
                pr
        ).map(SecurityFindingDto.Response::from);
    }

    @Transactional(readOnly = true)
    public SecurityFindingDto.Response get(Long id) {
        return SecurityFindingDto.Response.from(find(id));
    }

    @Transactional
    public SecurityFindingDto.Response create(SecurityFindingDto.Request req, MultipartFile file, User user)
            throws IOException {
        if (req.getFindingSummary() == null || req.getFindingSummary().isBlank())
            throw new BusinessException("결함 요약을 입력하세요.");

        SecurityFinding f = SecurityFinding.builder()
                .year(req.getYear() > 0 ? req.getYear() : java.time.Year.now().getValue())
                .auditType(parseAuditTypeOrDefault(req.getAuditType()))
                .domain(req.getDomain())
                .requirementCode(req.getRequirementCode())
                .requirementName(req.getRequirementName())
                .findingSummary(req.getFindingSummary().trim())
                .findingDetail(req.getFindingDetail())
                .riskLevel(parseRiskLevelOrDefault(req.getRiskLevel()))
                .correctiveAction(req.getCorrectiveAction())
                .actionDeadline(req.getActionDeadline())
                .status(parseStatusOrDefault(req.getStatus()))
                .resolvedAt(req.getResolvedAt())
                .resolver(req.getResolver())
                .createdBy(user)
                .build();
        f = repo.save(f);

        if (file != null && !file.isEmpty()) {
            String path = fileStorageService.store(file, "sec-finding/" + f.getId());
            f.setFilePath(path);
            f.setFileName(file.getOriginalFilename());
            f.setFileSize(file.getSize());
        }
        return SecurityFindingDto.Response.from(f);
    }

    @Transactional
    public SecurityFindingDto.Response update(Long id, SecurityFindingDto.Request req, MultipartFile file)
            throws IOException {
        SecurityFinding f = find(id);
        if (req.getFindingSummary() != null && !req.getFindingSummary().isBlank())
            f.setFindingSummary(req.getFindingSummary().trim());
        if (req.getDomain() != null) f.setDomain(req.getDomain());
        if (req.getRequirementCode() != null) f.setRequirementCode(req.getRequirementCode());
        if (req.getRequirementName() != null) f.setRequirementName(req.getRequirementName());
        if (req.getFindingDetail() != null) f.setFindingDetail(req.getFindingDetail());
        if (req.getRiskLevel() != null) f.setRiskLevel(parseRiskLevelOrDefault(req.getRiskLevel()));
        if (req.getAuditType() != null) f.setAuditType(parseAuditTypeOrDefault(req.getAuditType()));
        if (req.getCorrectiveAction() != null) f.setCorrectiveAction(req.getCorrectiveAction());
        if (req.getActionDeadline() != null) f.setActionDeadline(req.getActionDeadline());
        if (req.getStatus() != null) f.setStatus(parseStatusOrDefault(req.getStatus()));
        if (req.getResolvedAt() != null) f.setResolvedAt(req.getResolvedAt());
        if (req.getResolver() != null) f.setResolver(req.getResolver());

        if (file != null && !file.isEmpty()) {
            if (f.getFilePath() != null) fileStorageService.delete(f.getFilePath());
            String path = fileStorageService.store(file, "sec-finding/" + f.getId());
            f.setFilePath(path);
            f.setFileName(file.getOriginalFilename());
            f.setFileSize(file.getSize());
        }
        return SecurityFindingDto.Response.from(f);
    }

    @Transactional
    public void delete(Long id) throws IOException {
        SecurityFinding f = find(id);
        if (f.getFilePath() != null) fileStorageService.delete(f.getFilePath());
        repo.delete(f);
    }

    public org.springframework.core.io.Resource download(Long id) {
        SecurityFinding f = find(id);
        if (f.getFilePath() == null) throw new BusinessException("첨부파일이 없습니다.");
        return fileStorageService.load(f.getFilePath());
    }

    private SecurityFinding find(Long id) {
        return repo.findById(id).orElseThrow(() -> new ResourceNotFoundException("SecurityFinding", id));
    }

    private SecurityFinding.Status parseStatus(String s) {
        if (s == null || s.isBlank()) return null;
        try { return SecurityFinding.Status.valueOf(s.toUpperCase()); } catch (IllegalArgumentException e) { return null; }
    }

    private SecurityFinding.Status parseStatusOrDefault(String s) {
        if (s == null || s.isBlank()) return SecurityFinding.Status.OPEN;
        try { return SecurityFinding.Status.valueOf(s.toUpperCase()); } catch (IllegalArgumentException e) { return SecurityFinding.Status.OPEN; }
    }

    private SecurityFinding.RiskLevel parseRiskLevel(String s) {
        if (s == null || s.isBlank()) return null;
        try { return SecurityFinding.RiskLevel.valueOf(s.toUpperCase()); } catch (IllegalArgumentException e) { return null; }
    }

    private SecurityFinding.RiskLevel parseRiskLevelOrDefault(String s) {
        if (s == null || s.isBlank()) return SecurityFinding.RiskLevel.MEDIUM;
        try { return SecurityFinding.RiskLevel.valueOf(s.toUpperCase()); } catch (IllegalArgumentException e) { return SecurityFinding.RiskLevel.MEDIUM; }
    }

    private SecurityFinding.AuditType parseAuditType(String s) {
        if (s == null || s.isBlank()) return null;
        try { return SecurityFinding.AuditType.valueOf(s.toUpperCase()); } catch (IllegalArgumentException e) { return null; }
    }

    private SecurityFinding.AuditType parseAuditTypeOrDefault(String s) {
        if (s == null || s.isBlank()) return SecurityFinding.AuditType.ISMS_P;
        try { return SecurityFinding.AuditType.valueOf(s.toUpperCase()); } catch (IllegalArgumentException e) { return SecurityFinding.AuditType.ISMS_P; }
    }
}
