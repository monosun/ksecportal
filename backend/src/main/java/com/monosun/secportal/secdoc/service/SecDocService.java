package com.monosun.secportal.secdoc.service;

import com.monosun.secportal.auth.entity.User;
import com.monosun.secportal.common.exception.BusinessException;
import com.monosun.secportal.common.exception.ResourceNotFoundException;
import com.monosun.secportal.common.service.FileStorageService;
import com.monosun.secportal.secdoc.dto.SecDocDto;
import com.monosun.secportal.secdoc.entity.SecDoc;
import com.monosun.secportal.secdoc.repository.SecDocRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SecDocService {

    private final SecDocRepository repository;
    private final FileStorageService fileStorageService;

    private static final java.util.Set<String> SEARCH_FIELDS =
            java.util.Set.of("ALL", "TITLE", "DESCRIPTION", "FILENAME", "VERSION", "ORG");

    @Transactional(readOnly = true)
    public Page<SecDocDto.Response> list(String category, String keyword, String searchField, int page, int size) {
        SecDoc.Category cat = parseCategory(category);
        String kw = (keyword == null || keyword.isBlank()) ? null : keyword.trim();
        String field = (searchField != null && SEARCH_FIELDS.contains(searchField.toUpperCase()))
                ? searchField.toUpperCase() : "ALL";
        PageRequest pr = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<SecDoc> docs = repository.findLatest(cat, kw, field, pr);

        return docs.map(doc -> {
            int versionCount = repository.findByDocumentKeyOrderByCreatedAtDesc(doc.getDocumentKey()).size();
            return SecDocDto.Response.from(doc, versionCount);
        });
    }

    @Transactional(readOnly = true)
    public SecDocDto.Response get(Long id) {
        SecDoc doc = find(id);
        int versionCount = repository.findByDocumentKeyOrderByCreatedAtDesc(doc.getDocumentKey()).size();
        return SecDocDto.Response.from(doc, versionCount);
    }

    @Transactional(readOnly = true)
    public List<SecDocDto.Response> getVersions(Long id) {
        SecDoc doc = find(id);
        return repository.findByDocumentKeyOrderByCreatedAtDesc(doc.getDocumentKey())
                .stream()
                .map(SecDocDto.Response::from)
                .collect(Collectors.toList());
    }

    @Transactional
    public SecDocDto.Response create(SecDocDto.CreateRequest req, MultipartFile file, User user) throws IOException {
        SecDoc.Category category = parseCategory(req.getCategory());
        if (category == null) throw new BusinessException("카테고리를 선택해주세요.");
        if (req.getTitle() == null || req.getTitle().isBlank()) throw new BusinessException("제목을 입력해주세요.");

        String docKey = UUID.randomUUID().toString();
        String version = (req.getVersion() == null || req.getVersion().isBlank()) ? "1.0" : req.getVersion();

        SecDoc doc = SecDoc.builder()
                .documentKey(docKey)
                .title(req.getTitle().trim())
                .description(req.getDescription())
                .category(category)
                .version(version)
                .producingOrg(trimToNull(req.getProducingOrg()))
                .latest(true)
                .uploader(user)
                .build();
        doc = repository.save(doc);

        if (file != null && !file.isEmpty()) {
            String path = fileStorageService.store(file, "secdoc/" + doc.getId());
            doc.setFilePath(path);
            doc.setFileName(file.getOriginalFilename());
            doc.setFileSize(file.getSize());
        }

        return SecDocDto.Response.from(doc, 1);
    }

    @Transactional
    public SecDocDto.Response addVersion(Long id, SecDocDto.NewVersionRequest req, MultipartFile file, User user) throws IOException {
        SecDoc latest = find(id);
        if (!latest.isLatest()) throw new BusinessException("최신 버전이 아닙니다. 최신 버전에서만 새 버전을 추가할 수 있습니다.");

        String newVersion = (req.getVersion() == null || req.getVersion().isBlank())
                ? incrementVersion(latest.getVersion())
                : req.getVersion();

        // mark old version as not latest
        latest.setLatest(false);

        SecDoc newDoc = SecDoc.builder()
                .documentKey(latest.getDocumentKey())
                .title(latest.getTitle())
                .description(req.getDescription() != null ? req.getDescription() : latest.getDescription())
                .category(latest.getCategory())
                .version(newVersion)
                .producingOrg(latest.getProducingOrg())
                .latest(true)
                .uploader(user)
                .build();
        newDoc = repository.save(newDoc);

        if (file != null && !file.isEmpty()) {
            String path = fileStorageService.store(file, "secdoc/" + newDoc.getId());
            newDoc.setFilePath(path);
            newDoc.setFileName(file.getOriginalFilename());
            newDoc.setFileSize(file.getSize());
        }

        int versionCount = repository.findByDocumentKeyOrderByCreatedAtDesc(newDoc.getDocumentKey()).size();
        return SecDocDto.Response.from(newDoc, versionCount);
    }

    @Transactional
    public SecDocDto.Response updateMeta(Long id, String title, String description, String producingOrg) {
        SecDoc doc = find(id);
        if (title != null && !title.isBlank()) doc.setTitle(title.trim());
        if (description != null) doc.setDescription(description);
        if (producingOrg != null) doc.setProducingOrg(trimToNull(producingOrg));
        int versionCount = repository.findByDocumentKeyOrderByCreatedAtDesc(doc.getDocumentKey()).size();
        return SecDocDto.Response.from(doc, versionCount);
    }

    private String trimToNull(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }

    @Transactional
    public void delete(Long id) throws IOException {
        SecDoc doc = find(id);
        List<SecDoc> versions = repository.findByDocumentKeyOrderByCreatedAtDesc(doc.getDocumentKey());
        for (SecDoc v : versions) {
            if (v.getFilePath() != null) fileStorageService.delete(v.getFilePath());
        }
        repository.deleteByDocumentKey(doc.getDocumentKey());
    }

    @Transactional
    public void deleteVersion(Long id) throws IOException {
        SecDoc doc = find(id);
        if (doc.getFilePath() != null) fileStorageService.delete(doc.getFilePath());

        List<SecDoc> remaining = repository.findByDocumentKeyOrderByCreatedAtDesc(doc.getDocumentKey())
                .stream().filter(d -> !d.getId().equals(id)).collect(Collectors.toList());

        if (doc.isLatest() && !remaining.isEmpty()) {
            remaining.get(0).setLatest(true);
        }
        repository.delete(doc);
    }

    public org.springframework.core.io.Resource download(Long id) {
        SecDoc doc = find(id);
        if (doc.getFilePath() == null) throw new BusinessException("첨부파일이 없습니다.");
        return fileStorageService.load(doc.getFilePath());
    }

    private SecDoc find(Long id) {
        return repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("SecDoc", id));
    }

    private SecDoc.Category parseCategory(String category) {
        if (category == null || category.isBlank()) return null;
        try { return SecDoc.Category.valueOf(category.toUpperCase()); }
        catch (IllegalArgumentException e) { return null; }
    }

    private String incrementVersion(String version) {
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
}
