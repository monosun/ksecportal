package com.monosun.secportal.privacy.service;

import com.monosun.secportal.audit.service.AuditLogService;
import com.monosun.secportal.auth.entity.User;
import com.monosun.secportal.common.exception.BusinessException;
import com.monosun.secportal.common.exception.ResourceNotFoundException;
import com.monosun.secportal.common.service.FileStorageService;
import com.monosun.secportal.privacy.dto.PrivacyConsentDto;
import com.monosun.secportal.privacy.dto.PrivacyConsentVersionDto;
import com.monosun.secportal.privacy.entity.PrivacyConsent;
import com.monosun.secportal.privacy.entity.PrivacyConsentVersion;
import com.monosun.secportal.privacy.repository.PrivacyConsentRepository;
import com.monosun.secportal.privacy.repository.PrivacyConsentVersionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PrivacyConsentService {

    private final PrivacyConsentRepository repo;
    private final PrivacyConsentVersionRepository versionRepo;
    private final FileStorageService fileStorageService;
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
                // 버전은 수동 입력하지 않고 항상 1.0부터 시작하며, 이후 버전이력으로만 관리한다.
                .version("1.0")
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
        // 최초 버전 이력 자동 생성
        versionRepo.save(PrivacyConsentVersion.builder()
                .consent(saved)
                .version(saved.getVersion())
                .effectiveDate(saved.getEffectiveDate())
                .changeNote("최초 등록")
                .build());
        auditLogService.log("CREATE", "PrivacyConsent", saved.getId(),
                saved.getTitle() + " v" + saved.getVersion());
        return PrivacyConsentDto.Response.from(saved);
    }

    @Transactional
    public PrivacyConsentDto.Response update(Long id, PrivacyConsentDto.Request req) {
        PrivacyConsent c = find(id);
        if (req.getTitle() != null && !req.getTitle().isBlank()) c.setTitle(req.getTitle().trim());
        // 버전은 세부항목에서 수동 수정 불가 — 버전이력(추가/삭제)으로만 변경된다.
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

    // ── 버전 이력 관리 ─────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<PrivacyConsentVersionDto.Response> listVersions(Long consentId) {
        find(consentId); // 존재 검증
        return versionRepo.findByConsentIdOrderByIdDesc(consentId).stream()
                .map(PrivacyConsentVersionDto.Response::from)
                .toList();
    }

    /** 새 버전 이력 등록 — 첨부파일을 함께 저장하고, 동의서의 현재 버전을 갱신한다. */
    @Transactional
    public PrivacyConsentVersionDto.Response addVersion(
            Long consentId, PrivacyConsentVersionDto.CreateRequest req, MultipartFile file, User user) throws IOException {
        PrivacyConsent consent = find(consentId);

        // 버전은 수동 지정 없이 현재 버전에서 자동 증가한다.
        String version = incrementVersion(consent.getVersion());

        PrivacyConsentVersion v = PrivacyConsentVersion.builder()
                .consent(consent)
                .version(version)
                .effectiveDate(req.getEffectiveDate())
                .changeNote(req.getChangeNote())
                .author(user)
                .build();
        PrivacyConsentVersion saved = versionRepo.save(v);

        if (file != null && !file.isEmpty()) {
            String path = fileStorageService.store(file, "privacy-consent/" + consentId);
            saved.setFilePath(path);
            saved.setFileName(file.getOriginalFilename());
            saved.setFileSize(file.getSize());
        }

        // 동의서의 현재 버전/시행일을 최신 버전으로 갱신
        consent.setVersion(version);
        if (req.getEffectiveDate() != null) consent.setEffectiveDate(req.getEffectiveDate());

        auditLogService.log("CREATE", "PrivacyConsentVersion", saved.getId(),
                consent.getTitle() + " v" + version);
        return PrivacyConsentVersionDto.Response.from(saved);
    }

    @Transactional
    public void deleteVersion(Long versionId) throws IOException {
        PrivacyConsentVersion v = findVersion(versionId);
        PrivacyConsent consent = v.getConsent();
        String removedVersion = v.getVersion();
        if (v.getFilePath() != null) fileStorageService.delete(v.getFilePath());
        versionRepo.delete(v);
        versionRepo.flush();

        // 버전이력이 삭제되면 현재 버전을 남아있는 최신(직전) 버전으로 롤백한다. 없으면 최초 1.0.
        List<PrivacyConsentVersion> remaining = versionRepo.findByConsentIdOrderByIdDesc(consent.getId());
        String rolledBack;
        if (remaining.isEmpty()) {
            rolledBack = "1.0";
            consent.setVersion(rolledBack);
        } else {
            PrivacyConsentVersion latest = remaining.get(0);
            rolledBack = latest.getVersion();
            consent.setVersion(rolledBack);
            if (latest.getEffectiveDate() != null) consent.setEffectiveDate(latest.getEffectiveDate());
        }

        auditLogService.log("DELETE", "PrivacyConsentVersion", versionId,
                consent.getTitle() + " v" + removedVersion + " → v" + rolledBack);
    }

    @Transactional(readOnly = true)
    public PrivacyConsentVersion getVersion(Long versionId) {
        return findVersion(versionId);
    }

    public Resource downloadVersionFile(Long versionId) {
        PrivacyConsentVersion v = findVersion(versionId);
        if (v.getFilePath() == null) throw new BusinessException("첨부파일이 없습니다.");
        return fileStorageService.load(v.getFilePath());
    }

    @Transactional
    public void delete(Long id) throws IOException {
        PrivacyConsent c = find(id);
        for (PrivacyConsentVersion v : versionRepo.findByConsentIdOrderByIdDesc(id)) {
            if (v.getFilePath() != null) fileStorageService.delete(v.getFilePath());
        }
        versionRepo.deleteByConsentId(id);
        repo.delete(c);
        auditLogService.log("DELETE", "PrivacyConsent", id, c.getTitle() + " v" + c.getVersion());
    }

    private PrivacyConsentVersion findVersion(Long versionId) {
        return versionRepo.findById(versionId)
                .orElseThrow(() -> new ResourceNotFoundException("PrivacyConsentVersion", versionId));
    }

    private String incrementVersion(String version) {
        if (version == null || version.isBlank()) return "1.0";
        try {
            String[] parts = version.split("\\.");
            if (parts.length >= 2) {
                int major = Integer.parseInt(parts[0]);
                int minor = Integer.parseInt(parts[1]) + 1;
                return major + "." + minor;
            }
        } catch (NumberFormatException ignored) {}
        return version + ".1";
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
