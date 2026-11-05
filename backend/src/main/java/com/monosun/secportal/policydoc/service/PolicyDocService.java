package com.monosun.secportal.policydoc.service;

import com.monosun.secportal.audit.service.AuditLogService;
import com.monosun.secportal.auth.entity.User;
import com.monosun.secportal.common.exception.BusinessException;
import com.monosun.secportal.common.exception.ResourceNotFoundException;
import com.monosun.secportal.common.service.DocumentPreviewService;
import com.monosun.secportal.common.service.FileStorageService;
import com.monosun.secportal.policydoc.dto.PolicyDocDto;
import com.monosun.secportal.policydoc.entity.PolicyDoc;
import com.monosun.secportal.policydoc.entity.PolicyDocRevision;
import com.monosun.secportal.policydoc.repository.PolicyDocRepository;
import com.monosun.secportal.policydoc.repository.PolicyDocRevisionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PolicyDocService {

    private final PolicyDocRepository docRepository;
    private final PolicyDocRevisionRepository revisionRepository;
    private final FileStorageService fileStorageService;
    private final DocumentPreviewService documentPreviewService;
    private final AuditLogService auditLogService;

    /** 정책 문서로 받는 형식 — 뷰어로 열 수 있는 문서 위주 (HWP 는 변환 미지원이라 다운로드로 확인) */
    private static final Set<String> ALLOWED_EXTS = Set.of(
            "pdf", "doc", "docx", "odt", "rtf", "ppt", "pptx", "xls", "xlsx", "txt", "hwp", "hwpx");

    @Transactional(readOnly = true)
    public Page<PolicyDocDto.Response> list(String category, String status, String keyword, int page, int size) {
        String kw = (keyword == null || keyword.isBlank()) ? null : keyword.trim();
        PageRequest pr = PageRequest.of(page, size,
                Sort.by("category").ascending().and(Sort.by("docNo").ascending()).and(Sort.by("title").ascending()));
        Page<PolicyDoc> docs = docRepository.search(parseCategory(category), parseStatus(status), kw, pr);

        Map<Long, List<PolicyDocRevision>> byDoc = docs.isEmpty() ? Map.of()
                : revisionRepository.findByDocIds(docs.map(PolicyDoc::getId).toList()).stream()
                    .collect(Collectors.groupingBy(r -> r.getDocument().getId(), LinkedHashMap::new, Collectors.toList()));
        return docs.map(d -> PolicyDocDto.Response.from(d, byDoc.getOrDefault(d.getId(), List.of()), false));
    }

    /** 분류 탭에 표시할 분류별 문서 수 */
    @Transactional(readOnly = true)
    public Map<String, Long> summary(String status) {
        Map<String, Long> result = new LinkedHashMap<>();
        for (PolicyDoc.Category c : PolicyDoc.Category.values()) result.put(c.name(), 0L);
        for (Object[] row : docRepository.countByCategory(parseStatus(status))) {
            result.put(((PolicyDoc.Category) row[0]).name(), (Long) row[1]);
        }
        return result;
    }

    @Transactional(readOnly = true)
    public PolicyDocDto.Response get(Long id) {
        PolicyDoc doc = findDoc(id);
        return PolicyDocDto.Response.from(doc, revisionRepository.findByDocId(id), true);
    }

    @Transactional
    public PolicyDocDto.Response create(PolicyDocDto.CreateRequest req, MultipartFile file, User user) throws IOException {
        if (req == null || req.getDoc() == null) throw new BusinessException("문서 정보를 입력해주세요.");
        PolicyDoc doc = new PolicyDoc();
        applyDoc(doc, req.getDoc());
        doc.setStatus(PolicyDoc.Status.ACTIVE);
        doc.setRegistrant(user);
        doc = docRepository.save(doc);

        PolicyDocDto.RevisionRequest revReq = req.getRevision() != null ? req.getRevision() : new PolicyDocDto.RevisionRequest();
        if (revReq.getRevisionType() == null || revReq.getRevisionType().isBlank()) {
            revReq.setRevisionType(PolicyDocRevision.RevisionType.ENACTMENT.name());
        }
        if (revReq.getVersion() == null || revReq.getVersion().isBlank()) revReq.setVersion("1.0");
        saveNewRevision(doc, revReq, file, user);

        auditLogService.log("POLICY_DOC_CREATED", "POLICY_DOC", doc.getId(),
                doc.getTitle() + " (" + doc.getCategory() + " v" + revReq.getVersion() + ")");
        return get(doc.getId());
    }

    @Transactional
    public PolicyDocDto.Response update(Long id, PolicyDocDto.DocRequest req) {
        PolicyDoc doc = findDoc(id);
        applyDoc(doc, req);
        auditLogService.log("POLICY_DOC_UPDATED", "POLICY_DOC", id, doc.getTitle());
        return get(id);
    }

    @Transactional
    public void delete(Long id) throws IOException {
        PolicyDoc doc = findDoc(id);
        List<PolicyDocRevision> revisions = revisionRepository.findByDocId(id);
        for (PolicyDocRevision r : revisions) deleteFile(r.getFilePath());
        revisionRepository.deleteAll(revisions);
        // 정책 문서가 없어졌을 때 누가·언제 지웠는지 추적할 수 있도록 남긴다
        auditLogService.log("POLICY_DOC_DELETED", "POLICY_DOC", id,
                doc.getTitle() + " (" + doc.getCategory() + ", 전체 " + revisions.size() + "개 버전)");
        docRepository.delete(doc);
    }

    // ── 버전(개정본) ──────────────────────────────────────────────────────

    @Transactional
    public PolicyDocDto.Response addRevision(Long docId, PolicyDocDto.RevisionRequest req, MultipartFile file, User user) throws IOException {
        PolicyDoc doc = findDoc(docId);
        if (req.getVersion() == null || req.getVersion().isBlank()) {
            List<PolicyDocRevision> existing = revisionRepository.findByDocId(docId);
            req.setVersion(existing.isEmpty() ? "1.0" : incrementVersion(existing.get(0).getVersion()));
        }
        PolicyDocRevision rev = saveNewRevision(doc, req, file, user);
        auditLogService.log("POLICY_DOC_REVISION_ADDED", "POLICY_DOC", docId,
                doc.getTitle() + " v" + rev.getVersion() + " (" + rev.getRevisionType() + ")");
        return get(docId);
    }

    @Transactional
    public PolicyDocDto.Response updateRevision(Long revisionId, PolicyDocDto.RevisionRequest req) {
        PolicyDocRevision rev = findRevision(revisionId);
        String version = req.getVersion() == null ? null : req.getVersion().trim();
        if (version != null && !version.isEmpty() && !version.equals(rev.getVersion())) {
            if (revisionRepository.existsByDocumentIdAndVersion(rev.getDocument().getId(), version)) {
                throw new BusinessException("이미 등록된 버전입니다: v" + version);
            }
            rev.setVersion(version);
        }
        applyRevision(rev, req);
        refreshStatus(rev.getDocument());
        auditLogService.log("POLICY_DOC_REVISION_UPDATED", "POLICY_DOC", rev.getDocument().getId(),
                rev.getDocument().getTitle() + " v" + rev.getVersion());
        return get(rev.getDocument().getId());
    }

    /** 버전 파일 교체 — 잘못 올린 파일을 바로잡을 때 */
    @Transactional
    public PolicyDocDto.Response replaceRevisionFile(Long revisionId, MultipartFile file) throws IOException {
        PolicyDocRevision rev = findRevision(revisionId);
        if (file == null || file.isEmpty()) throw new BusinessException("교체할 파일을 선택해주세요.");
        validateFile(file);
        String oldPath = rev.getFilePath();
        storeFile(rev, file);
        deleteFile(oldPath);
        auditLogService.log("POLICY_DOC_FILE_REPLACED", "POLICY_DOC", rev.getDocument().getId(),
                rev.getDocument().getTitle() + " v" + rev.getVersion() + " → " + rev.getFileName());
        return get(rev.getDocument().getId());
    }

    @Transactional
    public PolicyDocDto.Response deleteRevision(Long revisionId) throws IOException {
        PolicyDocRevision rev = findRevision(revisionId);
        PolicyDoc doc = rev.getDocument();
        if (revisionRepository.findByDocId(doc.getId()).size() <= 1) {
            throw new BusinessException("마지막 남은 버전은 삭제할 수 없습니다. 문서 자체를 삭제해주세요.");
        }
        deleteFile(rev.getFilePath());
        auditLogService.log("POLICY_DOC_REVISION_DELETED", "POLICY_DOC", doc.getId(),
                doc.getTitle() + " v" + rev.getVersion());
        revisionRepository.delete(rev);
        revisionRepository.flush();
        refreshStatus(doc);
        return get(doc.getId());
    }

    public Resource download(Long revisionId) {
        PolicyDocRevision rev = findRevision(revisionId);
        if (rev.getFilePath() == null) throw new BusinessException("첨부파일이 없습니다.");
        return fileStorageService.load(rev.getFilePath());
    }

    @Transactional(readOnly = true)
    public String fileName(Long revisionId) {
        return findRevision(revisionId).getFileName();
    }

    /** 브라우저가 직접 열 수 없는 오피스 문서(DOC·PPT 등)를 PDF 로 변환해 뷰어에 보여준다 (결과는 캐시) */
    public Resource previewPdf(Long revisionId) {
        PolicyDocRevision rev = findRevision(revisionId);
        if (rev.getFilePath() == null) throw new BusinessException("첨부파일이 없습니다.");
        return documentPreviewService.toPdf(rev.getFilePath(), rev.getFileName());
    }

    // ── 내부 ─────────────────────────────────────────────────────────────

    private PolicyDocRevision saveNewRevision(PolicyDoc doc, PolicyDocDto.RevisionRequest req,
                                              MultipartFile file, User user) throws IOException {
        String version = req.getVersion().trim();
        if (doc.getId() != null && revisionRepository.existsByDocumentIdAndVersion(doc.getId(), version)) {
            throw new BusinessException("이미 등록된 버전입니다: v" + version);
        }
        if (file != null && !file.isEmpty()) validateFile(file);

        PolicyDocRevision rev = new PolicyDocRevision();
        rev.setDocument(doc);
        rev.setVersion(version);
        rev.setUploader(user);
        rev.setRevisionType(PolicyDocRevision.RevisionType.AMENDMENT);
        applyRevision(rev, req);
        if (rev.getRevisionDate() == null) rev.setRevisionDate(LocalDate.now());
        rev = revisionRepository.save(rev);

        if (file != null && !file.isEmpty()) storeFile(rev, file);
        revisionRepository.flush();
        refreshStatus(doc);
        return rev;
    }

    private void applyDoc(PolicyDoc doc, PolicyDocDto.DocRequest req) {
        if (req.getTitle() == null || req.getTitle().isBlank()) throw new BusinessException("문서명을 입력해주세요.");
        PolicyDoc.Category category = parseCategory(req.getCategory());
        if (category == null) throw new BusinessException("분류를 선택해주세요.");
        doc.setTitle(req.getTitle().trim());
        doc.setCategory(category);
        doc.setDocNo(resolveDocNo(doc, trimToNull(req.getDocNo())));
        doc.setDepartment(trimToNull(req.getDepartment()));
        doc.setOwner(trimToNull(req.getOwner()));
        doc.setDescription(trimToNull(req.getDescription()));
    }

    /**
     * 입력한 문서번호는 중복만 확인해 그대로 쓰고, 비워 두면 분류 키워드로 다음 번호를 부여한다.
     * 수정 시 분류를 바꿔도 이미 있는 번호는 유지한다(문서번호는 외부 문서·결재에 인용되므로 바뀌면 안 된다).
     */
    private String resolveDocNo(PolicyDoc doc, String requested) {
        if (requested == null) return nextDocNo(doc.getCategory());
        boolean duplicate = doc.getId() == null
                ? docRepository.existsByDocNo(requested)
                : docRepository.existsByDocNoAndIdNot(requested, doc.getId());
        if (duplicate) throw new BusinessException("이미 사용 중인 문서번호입니다: " + requested);
        return requested;
    }

    /** 분류 키워드별 다음 문서번호 — 키워드-001 형식, 직접 입력한 같은 형식의 번호도 이어서 센다 */
    @Transactional(readOnly = true)
    public String nextDocNo(PolicyDoc.Category category) {
        String prefix = category.getPrefix();
        java.util.regex.Pattern p = java.util.regex.Pattern.compile("^" + prefix + "-(\\d+)$");
        int max = 0;
        for (String no : docRepository.findDocNosByPrefix(prefix)) {
            java.util.regex.Matcher m = p.matcher(no);
            if (m.matches()) {
                try { max = Math.max(max, Integer.parseInt(m.group(1))); } catch (NumberFormatException ignored) {}
            }
        }
        return String.format("%s-%03d", prefix, max + 1);
    }

    /** 화면 안내용 — 문서번호를 비웠을 때 부여될 번호 */
    @Transactional(readOnly = true)
    public String nextDocNo(String category) {
        PolicyDoc.Category c = parseCategory(category);
        if (c == null) throw new BusinessException("분류를 선택해주세요.");
        return nextDocNo(c);
    }

    /** 문서번호 없이 등록된 기존 문서에 번호를 채운다 (기동 시 1회, 등록 순서대로) */
    @Transactional
    public int backfillDocNos() {
        List<PolicyDoc> targets = docRepository.findByDocNoIsNullOrderByIdAsc();
        for (PolicyDoc d : targets) {
            d.setDocNo(nextDocNo(d.getCategory()));
            docRepository.saveAndFlush(d);
        }
        return targets.size();
    }

    private void applyRevision(PolicyDocRevision rev, PolicyDocDto.RevisionRequest req) {
        if (req.getRevisionType() != null && !req.getRevisionType().isBlank()) {
            try {
                rev.setRevisionType(PolicyDocRevision.RevisionType.valueOf(req.getRevisionType().toUpperCase()));
            } catch (IllegalArgumentException e) {
                throw new BusinessException("알 수 없는 개정 구분입니다: " + req.getRevisionType());
            }
        }
        if (req.getRevisionDate() != null) rev.setRevisionDate(req.getRevisionDate());
        rev.setEffectiveDate(req.getEffectiveDate());
        rev.setApprover(trimToNull(req.getApprover()));
        rev.setChangeReason(trimToNull(req.getChangeReason()));
        rev.setChangeDetails(trimToNull(req.getChangeDetails()));
    }

    /** 현행본(최신 개정본)이 폐지이면 문서도 폐지 상태로 맞춘다 */
    private void refreshStatus(PolicyDoc doc) {
        List<PolicyDocRevision> revisions = revisionRepository.findByDocId(doc.getId());
        boolean abolished = !revisions.isEmpty()
                && revisions.get(0).getRevisionType() == PolicyDocRevision.RevisionType.ABOLITION;
        doc.setStatus(abolished ? PolicyDoc.Status.ABOLISHED : PolicyDoc.Status.ACTIVE);
    }

    private void validateFile(MultipartFile file) {
        String name = file.getOriginalFilename() != null ? file.getOriginalFilename() : "";
        int dot = name.lastIndexOf('.');
        String ext = dot > 0 ? name.substring(dot + 1).toLowerCase() : "";
        if (!ALLOWED_EXTS.contains(ext)) {
            throw new BusinessException("정책 문서로 등록할 수 없는 파일 형식입니다: " + (ext.isEmpty() ? "(확장자 없음)" : "." + ext)
                    + " — PDF·DOC(X)·HWP(X)·PPT(X)·XLS(X)·TXT 만 가능합니다.");
        }
    }

    private void storeFile(PolicyDocRevision rev, MultipartFile file) throws IOException {
        String path = fileStorageService.store(file, "policydoc/" + rev.getDocument().getId());
        rev.setFilePath(path);
        rev.setFileName(file.getOriginalFilename());
        rev.setFileSize(file.getSize());
    }

    private void deleteFile(String path) throws IOException {
        if (path == null) return;
        documentPreviewService.deleteCache(path);
        fileStorageService.delete(path);
    }

    private PolicyDoc findDoc(Long id) {
        return docRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("PolicyDoc", id));
    }

    private PolicyDocRevision findRevision(Long id) {
        return revisionRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("PolicyDocRevision", id));
    }

    private PolicyDoc.Category parseCategory(String category) {
        if (category == null || category.isBlank()) return null;
        try { return PolicyDoc.Category.valueOf(category.toUpperCase()); }
        catch (IllegalArgumentException e) { return null; }
    }

    private PolicyDoc.Status parseStatus(String status) {
        if (status == null || status.isBlank()) return null;
        try { return PolicyDoc.Status.valueOf(status.toUpperCase()); }
        catch (IllegalArgumentException e) { return null; }
    }

    private String trimToNull(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }

    private String incrementVersion(String version) {
        try {
            String[] parts = version.split("\\.");
            if (parts.length >= 2) {
                return Integer.parseInt(parts[0]) + "." + (Integer.parseInt(parts[1]) + 1);
            }
            return (Integer.parseInt(version) + 1) + ".0";
        } catch (NumberFormatException ignored) {}
        return version + ".1";
    }
}
