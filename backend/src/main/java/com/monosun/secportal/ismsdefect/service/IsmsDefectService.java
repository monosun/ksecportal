package com.monosun.secportal.ismsdefect.service;

import com.monosun.secportal.audit.service.AuditLogService;
import com.monosun.secportal.auth.entity.User;
import com.monosun.secportal.common.exception.BusinessException;
import com.monosun.secportal.common.exception.ResourceNotFoundException;
import com.monosun.secportal.common.service.FileStorageService;
import com.monosun.secportal.ismsdefect.dto.IsmsDefectDto;
import com.monosun.secportal.ismsdefect.entity.IsmsDefect;
import com.monosun.secportal.ismsdefect.entity.IsmsDefectReport;
import com.monosun.secportal.ismsdefect.repository.IsmsDefectReportRepository;
import com.monosun.secportal.ismsdefect.repository.IsmsDefectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class IsmsDefectService {

    private final IsmsDefectRepository defectRepository;
    private final IsmsDefectReportRepository reportRepository;
    private final FileStorageService fileStorageService;
    private final AuditLogService auditLogService;
    /** 등록·수정·삭제 결과를 보안 운영 > 보안 결함사항에 자동 반영한다 */
    private final IsmsDefectFindingSync findingSync;

    // ── 결함 ────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<IsmsDefectDto.DefectResponse> list(int year, String auditType, String defectType,
                                                   String severity, String status, String keyword) {
        return defectRepository.search(
                        year,
                        parse(IsmsDefect.AuditType.class, auditType),
                        parse(IsmsDefect.DefectType.class, defectType),
                        parse(IsmsDefect.Severity.class, severity),
                        parse(IsmsDefect.Status.class, status),
                        (keyword == null || keyword.isBlank()) ? null : keyword.trim())
                .stream().map(IsmsDefectDto.DefectResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public IsmsDefectDto.DefectResponse get(Long id) {
        return IsmsDefectDto.DefectResponse.from(findDefect(id));
    }

    @Transactional
    public IsmsDefectDto.DefectResponse create(IsmsDefectDto.DefectRequest req, MultipartFile file, User user)
            throws IOException {
        if (req.getTitle() == null || req.getTitle().isBlank())
            throw new BusinessException("결함 제목을 입력하세요.");

        int year = req.getYear() != null && req.getYear() > 0 ? req.getYear() : LocalDate.now().getYear();

        IsmsDefect d = IsmsDefect.builder()
                .year(year)
                .auditType(parseOr(IsmsDefect.AuditType.class, req.getAuditType(), IsmsDefect.AuditType.RENEWAL))
                .defectNo(trim(req.getDefectNo()))
                .defectType(parseOr(IsmsDefect.DefectType.class, req.getDefectType(), IsmsDefect.DefectType.DEFECT))
                .severity(parseOr(IsmsDefect.Severity.class, req.getSeverity(), IsmsDefect.Severity.MEDIUM))
                .domainName(trim(req.getDomainName()))
                .itemCode(trim(req.getItemCode()))
                .itemName(trim(req.getItemName()))
                .title(req.getTitle().trim())
                .content(req.getContent())
                .cause(req.getCause())
                .actionPlan(req.getActionPlan())
                .actionResult(req.getActionResult())
                .preventionPlan(req.getPreventionPlan())
                .dueDate(req.getDueDate())
                .completedDate(req.getCompletedDate())
                .assignee(trim(req.getAssignee()))
                .department(trim(req.getDepartment()))
                .status(parseOr(IsmsDefect.Status.class, req.getStatus(), IsmsDefect.Status.OPEN))
                .sortOrder(req.getSortOrder() != null ? req.getSortOrder() : defectRepository.maxSortOrder(year) + 1)
                .registrant(user)
                .build();
        d = defectRepository.save(d);

        if (file != null && !file.isEmpty()) storeFile(d, file);

        findingSync.sync(d);
        auditLogService.log("CREATE", "ISMS_DEFECT", d.getId(),
                year + "년 ISMS 결함 등록: " + d.getTitle());
        return IsmsDefectDto.DefectResponse.from(d);
    }

    @Transactional
    public IsmsDefectDto.DefectResponse update(Long id, IsmsDefectDto.DefectRequest req, MultipartFile file)
            throws IOException {
        IsmsDefect d = findDefect(id);

        if (req.getYear() != null && req.getYear() > 0) d.setYear(req.getYear());
        if (req.getTitle() != null && !req.getTitle().isBlank()) d.setTitle(req.getTitle().trim());
        if (req.getAuditType() != null) d.setAuditType(parseOr(IsmsDefect.AuditType.class, req.getAuditType(), d.getAuditType()));
        if (req.getDefectType() != null) d.setDefectType(parseOr(IsmsDefect.DefectType.class, req.getDefectType(), d.getDefectType()));
        if (req.getSeverity() != null) d.setSeverity(parseOr(IsmsDefect.Severity.class, req.getSeverity(), d.getSeverity()));
        if (req.getStatus() != null) d.setStatus(parseOr(IsmsDefect.Status.class, req.getStatus(), d.getStatus()));
        if (req.getDefectNo() != null) d.setDefectNo(trim(req.getDefectNo()));
        if (req.getDomainName() != null) d.setDomainName(trim(req.getDomainName()));
        if (req.getItemCode() != null) d.setItemCode(trim(req.getItemCode()));
        if (req.getItemName() != null) d.setItemName(trim(req.getItemName()));
        if (req.getContent() != null) d.setContent(req.getContent());
        if (req.getCause() != null) d.setCause(req.getCause());
        if (req.getActionPlan() != null) d.setActionPlan(req.getActionPlan());
        if (req.getActionResult() != null) d.setActionResult(req.getActionResult());
        if (req.getPreventionPlan() != null) d.setPreventionPlan(req.getPreventionPlan());
        if (req.getAssignee() != null) d.setAssignee(trim(req.getAssignee()));
        if (req.getDepartment() != null) d.setDepartment(trim(req.getDepartment()));
        if (req.getSortOrder() != null) d.setSortOrder(req.getSortOrder());
        // 날짜는 "비우기"도 의미가 있으므로 폼에서 온 값을 그대로 반영한다(수정 폼이 항상 전체 값을 보낸다)
        d.setDueDate(req.getDueDate());
        d.setCompletedDate(req.getCompletedDate());

        if (Boolean.TRUE.equals(req.getRemoveFile())) clearFile(d);
        if (file != null && !file.isEmpty()) {
            clearFile(d);
            storeFile(d, file);
        }

        findingSync.sync(d);
        auditLogService.log("UPDATE", "ISMS_DEFECT", d.getId(),
                d.getYear() + "년 ISMS 결함 수정: " + d.getTitle());
        return IsmsDefectDto.DefectResponse.from(d);
    }

    @Transactional
    public void delete(Long id) throws IOException {
        IsmsDefect d = findDefect(id);
        findingSync.remove(d.getId());
        if (d.getFilePath() != null) fileStorageService.delete(d.getFilePath());
        auditLogService.log("DELETE", "ISMS_DEFECT", d.getId(),
                d.getYear() + "년 ISMS 결함 삭제: " + d.getTitle());
        defectRepository.delete(d);
    }

    @Transactional(readOnly = true)
    public IsmsDefect getDefectEntity(Long id) {
        return findDefect(id);
    }

    public Resource downloadDefectFile(Long id) {
        IsmsDefect d = findDefect(id);
        if (d.getFilePath() == null) throw new BusinessException("첨부파일이 없습니다.");
        return fileStorageService.load(d.getFilePath());
    }

    // ── 연도별 보고서 ────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public IsmsDefectDto.ReportResponse getReport(int year) {
        return reportRepository.findByYear(year)
                .map(IsmsDefectDto.ReportResponse::from)
                .orElseGet(() -> IsmsDefectDto.ReportResponse.empty(year));
    }

    @Transactional
    public IsmsDefectDto.ReportResponse saveReport(int year, IsmsDefectDto.ReportRequest req,
                                                   MultipartFile file, User user) throws IOException {
        IsmsDefectReport r = reportRepository.findByYear(year).orElseGet(() ->
                IsmsDefectReport.builder().year(year).build());

        if (req.getTitle() != null) r.setTitle(trim(req.getTitle()));
        if (r.getTitle() == null || r.getTitle().isBlank()) r.setTitle(year + "년 ISMS-P 결함 조치 보고서");
        if (req.getAuditType() != null)
            r.setAuditType(parseOr(IsmsDefect.AuditType.class, req.getAuditType(),
                    r.getAuditType() != null ? r.getAuditType() : IsmsDefect.AuditType.RENEWAL));
        if (req.getAuditOrg() != null) r.setAuditOrg(trim(req.getAuditOrg()));
        if (req.getAuditors() != null) r.setAuditors(trim(req.getAuditors()));
        if (req.getAuditScope() != null) r.setAuditScope(trim(req.getAuditScope()));
        if (req.getSummary() != null) r.setSummary(req.getSummary());
        if (req.getContent() != null) r.setContent(req.getContent());
        if (req.getConclusion() != null) r.setConclusion(req.getConclusion());
        if (req.getReporter() != null) r.setReporter(trim(req.getReporter()));
        r.setAuditStartDate(req.getAuditStartDate());
        r.setAuditEndDate(req.getAuditEndDate());
        r.setReportedAt(req.getReportedAt());
        if (r.getRegistrant() == null) r.setRegistrant(user);

        r = reportRepository.save(r);

        if (Boolean.TRUE.equals(req.getRemoveFile())) clearReportFile(r);
        if (file != null && !file.isEmpty()) {
            clearReportFile(r);
            String path = fileStorageService.store(file, "isms-defect/report/" + r.getId());
            r.setFilePath(path);
            r.setFileName(file.getOriginalFilename());
            r.setFileSize(file.getSize());
        }

        auditLogService.log("UPDATE", "ISMS_DEFECT_REPORT", r.getId(),
                year + "년 ISMS 결함 보고서 저장");
        return IsmsDefectDto.ReportResponse.from(r);
    }

    @Transactional
    public void deleteReport(int year) throws IOException {
        IsmsDefectReport r = reportRepository.findByYear(year)
                .orElseThrow(() -> new ResourceNotFoundException("IsmsDefectReport", (long) year));
        if (r.getFilePath() != null) fileStorageService.delete(r.getFilePath());
        auditLogService.log("DELETE", "ISMS_DEFECT_REPORT", r.getId(), year + "년 ISMS 결함 보고서 삭제");
        reportRepository.delete(r);
    }

    public Resource downloadReportFile(int year) {
        IsmsDefectReport r = reportRepository.findByYear(year)
                .orElseThrow(() -> new ResourceNotFoundException("IsmsDefectReport", (long) year));
        if (r.getFilePath() == null) throw new BusinessException("첨부파일이 없습니다.");
        return fileStorageService.load(r.getFilePath());
    }

    @Transactional(readOnly = true)
    public IsmsDefectReport getReportEntity(int year) {
        return reportRepository.findByYear(year)
                .orElseThrow(() -> new ResourceNotFoundException("IsmsDefectReport", (long) year));
    }

    // ── 요약 / 내보내기 ──────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<Integer> years() {
        return Stream.concat(defectRepository.findDistinctYears().stream(),
                        reportRepository.findDistinctYears().stream())
                .distinct().sorted((a, b) -> b - a).toList();
    }

    @Transactional(readOnly = true)
    public IsmsDefectDto.SummaryResponse summary(int year) {
        List<IsmsDefect> all = defectRepository.findByYearOrderBySortOrderAscIdAsc(year);
        LocalDate today = LocalDate.now();

        long completed = all.stream().filter(d -> d.getStatus() == IsmsDefect.Status.COMPLETED).count();
        long overdue = all.stream()
                .filter(d -> d.getStatus() != IsmsDefect.Status.COMPLETED)
                .filter(d -> d.getDueDate() != null && d.getDueDate().isBefore(today))
                .count();

        return IsmsDefectDto.SummaryResponse.builder()
                .year(year)
                .total(all.size())
                .open(countStatus(all, IsmsDefect.Status.OPEN))
                .inProgress(countStatus(all, IsmsDefect.Status.IN_PROGRESS))
                .completed(completed)
                .hold(countStatus(all, IsmsDefect.Status.HOLD))
                .defect(countType(all, IsmsDefect.DefectType.DEFECT))
                .recommendation(countType(all, IsmsDefect.DefectType.RECOMMENDATION))
                .improvement(countType(all, IsmsDefect.DefectType.IMPROVEMENT))
                .overdue(overdue)
                .completionRate(all.isEmpty() ? 0 : (int) Math.round(completed * 100.0 / all.size()))
                .reportWritten(reportRepository.findByYear(year).isPresent())
                .years(years())
                .build();
    }

    @Transactional(readOnly = true)
    public byte[] exportCsv(int year) {
        List<IsmsDefect> defects = defectRepository.findByYearOrderBySortOrderAscIdAsc(year);

        List<String> lines = new ArrayList<>();
        lines.add(String.join(",", "연도", "심사구분", "결함번호", "결함구분", "중요도", "분야",
                "인증기준코드", "인증기준명", "결함제목", "결함내용", "원인",
                "조치계획", "조치내용", "재발방지대책", "조치기한", "완료일",
                "담당자", "부서", "상태", "첨부파일", "등록자"));

        for (IsmsDefect d : defects) {
            lines.add(String.join(",",
                    String.valueOf(d.getYear()),
                    csv(auditTypeLabel(d.getAuditType())),
                    csv(d.getDefectNo()),
                    csv(defectTypeLabel(d.getDefectType())),
                    csv(severityLabel(d.getSeverity())),
                    csv(d.getDomainName()),
                    csv(d.getItemCode()),
                    csv(d.getItemName()),
                    csv(d.getTitle()),
                    csv(d.getContent()),
                    csv(d.getCause()),
                    csv(d.getActionPlan()),
                    csv(d.getActionResult()),
                    csv(d.getPreventionPlan()),
                    csv(d.getDueDate() != null ? d.getDueDate().toString() : ""),
                    csv(d.getCompletedDate() != null ? d.getCompletedDate().toString() : ""),
                    csv(d.getAssignee()),
                    csv(d.getDepartment()),
                    csv(statusLabel(d.getStatus())),
                    csv(d.getFileName()),
                    csv(d.getRegistrant() != null ? d.getRegistrant().getName() : "")));
        }

        String body = String.join("\n", lines) + "\n";
        // UTF-8 BOM — Excel 에서 한글이 깨지지 않도록 붙인다 (증적 CSV 와 같은 방식)
        byte[] bom = {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};
        byte[] content = body.getBytes(StandardCharsets.UTF_8);
        byte[] result = new byte[bom.length + content.length];
        System.arraycopy(bom, 0, result, 0, bom.length);
        System.arraycopy(content, 0, result, bom.length, content.length);
        return result;
    }

    // ── 내부 유틸 ────────────────────────────────────────────────────────────

    private IsmsDefect findDefect(Long id) {
        return defectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("IsmsDefect", id));
    }

    private void storeFile(IsmsDefect d, MultipartFile file) throws IOException {
        String path = fileStorageService.store(file, "isms-defect/" + d.getId());
        d.setFilePath(path);
        d.setFileName(file.getOriginalFilename());
        d.setFileSize(file.getSize());
    }

    private void clearFile(IsmsDefect d) throws IOException {
        if (d.getFilePath() != null) fileStorageService.delete(d.getFilePath());
        d.setFilePath(null);
        d.setFileName(null);
        d.setFileSize(null);
    }

    private void clearReportFile(IsmsDefectReport r) throws IOException {
        if (r.getFilePath() != null) fileStorageService.delete(r.getFilePath());
        r.setFilePath(null);
        r.setFileName(null);
        r.setFileSize(null);
    }

    private static long countStatus(List<IsmsDefect> list, IsmsDefect.Status s) {
        return list.stream().filter(d -> d.getStatus() == s).count();
    }

    private static long countType(List<IsmsDefect> list, IsmsDefect.DefectType t) {
        return list.stream().filter(d -> d.getDefectType() == t).count();
    }

    private static String trim(String s) {
        return s == null ? null : s.trim();
    }

    private static <E extends Enum<E>> E parse(Class<E> type, String v) {
        if (v == null || v.isBlank()) return null;
        try {
            return Enum.valueOf(type, v.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static <E extends Enum<E>> E parseOr(Class<E> type, String v, E fallback) {
        E parsed = parse(type, v);
        return parsed != null ? parsed : fallback;
    }

    private static String csv(String v) {
        if (v == null) return "\"\"";
        String cleaned = v.replace("\r\n", " ").replace('\n', ' ').replace('\r', ' ');
        return "\"" + cleaned.replace("\"", "\"\"") + "\"";
    }

    private static String auditTypeLabel(IsmsDefect.AuditType t) {
        return switch (t) {
            case INITIAL -> "최초심사";
            case FOLLOWUP -> "사후심사";
            case RENEWAL -> "갱신심사";
            case INTERNAL -> "내부심사";
            case OTHER -> "기타";
        };
    }

    private static String defectTypeLabel(IsmsDefect.DefectType t) {
        return switch (t) {
            case DEFECT -> "결함";
            case RECOMMENDATION -> "권고";
            case IMPROVEMENT -> "개선사항";
        };
    }

    private static String severityLabel(IsmsDefect.Severity s) {
        return switch (s) {
            case CRITICAL -> "매우높음";
            case HIGH -> "높음";
            case MEDIUM -> "보통";
            case LOW -> "낮음";
        };
    }

    private static String statusLabel(IsmsDefect.Status s) {
        return switch (s) {
            case OPEN -> "미조치";
            case IN_PROGRESS -> "조치중";
            case COMPLETED -> "조치완료";
            case HOLD -> "보류";
        };
    }
}
