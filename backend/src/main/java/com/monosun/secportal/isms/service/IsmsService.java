package com.monosun.secportal.isms.service;

import com.monosun.secportal.auth.entity.User;
import com.monosun.secportal.auth.repository.UserRepository;
import com.monosun.secportal.common.exception.ResourceNotFoundException;
import com.monosun.secportal.common.service.FileStorageService;
import com.monosun.secportal.isms.dto.IsmsDto;
import com.monosun.secportal.isms.entity.IsmsEvidence;
import com.monosun.secportal.isms.entity.IsmsItem;
import com.monosun.secportal.isms.entity.IsmsItemNote;
import com.monosun.secportal.isms.entity.IsmsPolicyMapping;
import com.monosun.secportal.isms.repository.IsmsEvidenceRepository;
import com.monosun.secportal.isms.repository.IsmsItemNoteRepository;
import com.monosun.secportal.isms.repository.IsmsItemRepository;
import com.monosun.secportal.isms.repository.IsmsPolicyMappingRepository;
import com.monosun.secportal.policy.entity.Policy;
import com.monosun.secportal.policy.repository.PolicyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.xssf.usermodel.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class IsmsService {

    private final IsmsItemRepository itemRepository;
    private final IsmsEvidenceRepository evidenceRepository;
    private final UserRepository userRepository;
    private final FileStorageService fileStorageService;
    private final IsmsPolicyMappingRepository policyMappingRepository;
    private final PolicyRepository policyRepository;
    private final IsmsItemNoteRepository itemNoteRepository;

    @Transactional(readOnly = true)
    public List<IsmsDto.ItemResponse> listItems(Integer year, String domainCode) {
        List<IsmsItem> items = domainCode != null && !domainCode.isBlank()
                ? itemRepository.findByDomainCodeOrderBySortOrderAsc(domainCode)
                : itemRepository.findAllByOrderBySortOrderAsc();

        List<Long> itemIds = items.stream().map(IsmsItem::getId).collect(Collectors.toList());
        Map<Long, List<IsmsDto.PolicyRef>> mappingsMap = buildMappingsMap(itemIds);

        if (year == null) {
            return items.stream().map(item ->
                IsmsDto.ItemResponse.from(item, 0L, null, mappingsMap.get(item.getId()))
            ).collect(Collectors.toList());
        }

        List<Object[]> rawStats = evidenceRepository.countByItemAndStatusForYear(year);
        Map<Long, Map<String, Long>> statsMap = new HashMap<>();
        for (Object[] row : rawStats) {
            Long itemId = (Long) row[0];
            String status = ((IsmsEvidence.Status) row[1]).name();
            Long count = (Long) row[2];
            statsMap.computeIfAbsent(itemId, k -> new HashMap<>()).put(status, count);
        }

        return items.stream().map(item -> {
            Map<String, Long> stats = statsMap.getOrDefault(item.getId(), Collections.emptyMap());
            long evidenceCount = stats.values().stream().mapToLong(Long::longValue).sum();
            String latestStatus = deriveStatus(stats);
            return IsmsDto.ItemResponse.from(item, evidenceCount, latestStatus, mappingsMap.get(item.getId()));
        }).collect(Collectors.toList());
    }

    @Transactional
    public void mapPolicy(Long itemId, Long policyId) {
        if (policyMappingRepository.existsByIsmsItemIdAndPolicyId(itemId, policyId)) return;
        IsmsItem item = itemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("IsmsItem", itemId));
        Policy policy = policyRepository.findById(policyId)
                .orElseThrow(() -> new ResourceNotFoundException("Policy", policyId));
        policyMappingRepository.save(IsmsPolicyMapping.builder()
                .ismsItem(item).policy(policy).build());
    }

    @Transactional
    public void unmapPolicy(Long itemId, Long policyId) {
        policyMappingRepository.deleteByIsmsItemIdAndPolicyId(itemId, policyId);
    }

    private Map<Long, List<IsmsDto.PolicyRef>> buildMappingsMap(List<Long> itemIds) {
        if (itemIds.isEmpty()) return Collections.emptyMap();
        return policyMappingRepository.findByIsmsItemIdIn(itemIds).stream()
                .collect(Collectors.groupingBy(
                        m -> m.getIsmsItem().getId(),
                        Collectors.mapping(m -> IsmsDto.PolicyRef.builder()
                                .id(m.getPolicy().getId())
                                .title(m.getPolicy().getTitle())
                                .status(m.getPolicy().getStatus().name())
                                .category(m.getPolicy().getCategory().name())
                                .build(),
                        Collectors.toList())));
    }

    @Transactional(readOnly = true)
    public IsmsDto.ItemResponse getItem(Long id) {
        IsmsItem item = itemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("IsmsItem", id));
        return IsmsDto.ItemResponse.from(item);
    }

    // ── 항목별 의견·현재 상태 (연도별) / 이행 가이드 (연도 무관) ────────

    @Transactional(readOnly = true)
    public IsmsDto.ItemNoteResponse getItemNote(Long itemId, int year) {
        return itemNoteRepository.findByItemIdAndYear(itemId, year)
                .map(IsmsDto.ItemNoteResponse::from)
                .orElseGet(() -> IsmsDto.ItemNoteResponse.empty(itemId, year));
    }

    /** 항목·연도 조합당 1건이므로 없으면 만들고 있으면 갱신한다. */
    @Transactional
    public IsmsDto.ItemNoteResponse saveItemNote(Long itemId, int year, IsmsDto.ItemNoteRequest req, User user) {
        IsmsItem item = itemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("IsmsItem", itemId));

        IsmsItemNote note = itemNoteRepository.findByItemIdAndYear(itemId, year)
                .orElseGet(() -> IsmsItemNote.builder().item(item).year(year).build());

        note.setStatusNote(req.getStatusNote());
        note.setOpinion(req.getOpinion());
        note.setUpdater(user);

        return IsmsDto.ItemNoteResponse.from(itemNoteRepository.save(note));
    }

    @Transactional
    public IsmsDto.ItemResponse updateItemGuide(Long itemId, IsmsDto.ItemGuideRequest req) {
        IsmsItem item = itemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("IsmsItem", itemId));
        item.setGuide(req.getGuide());
        return IsmsDto.ItemResponse.from(itemRepository.save(item));
    }

    @Transactional(readOnly = true)
    public List<IsmsDto.EvidenceResponse> listEvidences(Long itemId, Integer year) {
        List<IsmsEvidence> evidences = year != null
                ? evidenceRepository.findByItemIdAndYearOrderByCreatedAtDesc(itemId, year)
                : evidenceRepository.findByItemIdOrderByYearDescCreatedAtDesc(itemId);
        return evidences.stream().map(IsmsDto.EvidenceResponse::from).collect(Collectors.toList());
    }

    @Transactional
    public IsmsDto.EvidenceResponse createEvidence(Long itemId, IsmsDto.EvidenceCreateRequest request,
                                                   MultipartFile file, User user) throws IOException {
        IsmsItem item = itemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("IsmsItem", itemId));
        IsmsEvidence.Status status = parseStatus(request.getStatus(), IsmsEvidence.Status.COMPLIANT);
        IsmsEvidence evidence = IsmsEvidence.builder()
                .item(item)
                .year(request.getYear())
                .title(request.getTitle())
                .content(request.getContent())
                .status(status)
                .registrant(user)
                .build();
        evidence = evidenceRepository.save(evidence);
        if (file != null && !file.isEmpty()) {
            String path = fileStorageService.store(file, "isms/" + evidence.getId());
            evidence.setFilePath(path);
            evidence.setFileName(file.getOriginalFilename());
        }
        return IsmsDto.EvidenceResponse.from(evidence);
    }

    @Transactional
    public IsmsDto.EvidenceResponse updateEvidence(Long evidenceId, IsmsDto.EvidenceUpdateRequest request,
                                                   MultipartFile file) throws IOException {
        IsmsEvidence evidence = evidenceRepository.findById(evidenceId)
                .orElseThrow(() -> new ResourceNotFoundException("IsmsEvidence", evidenceId));
        if (request.getTitle() != null) evidence.setTitle(request.getTitle());
        if (request.getContent() != null) evidence.setContent(request.getContent());
        if (request.getStatus() != null) evidence.setStatus(parseStatus(request.getStatus(), evidence.getStatus()));
        if (file != null && !file.isEmpty()) {
            fileStorageService.delete(evidence.getFilePath());
            String path = fileStorageService.store(file, "isms/" + evidence.getId());
            evidence.setFilePath(path);
            evidence.setFileName(file.getOriginalFilename());
        }
        return IsmsDto.EvidenceResponse.from(evidence);
    }

    @Transactional
    public void deleteEvidence(Long evidenceId) throws IOException {
        IsmsEvidence evidence = evidenceRepository.findById(evidenceId)
                .orElseThrow(() -> new ResourceNotFoundException("IsmsEvidence", evidenceId));
        if (evidence.getFilePath() != null && evidence.getSourceEvidence() == null) {
            fileStorageService.delete(evidence.getFilePath());
        }
        evidenceRepository.delete(evidence);
    }

    @Transactional
    public IsmsDto.EvidenceResponse createEvidenceRef(Long itemId, IsmsDto.EvidenceRefRequest request, User user) {
        IsmsItem item = itemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("IsmsItem", itemId));
        IsmsEvidence source = evidenceRepository.findById(request.getSourceEvidenceId())
                .orElseThrow(() -> new ResourceNotFoundException("IsmsEvidence", request.getSourceEvidenceId()));
        if (source.getFilePath() == null)
            throw new IllegalArgumentException("참조 대상 증적에 파일이 없습니다.");
        IsmsEvidence.Status status = parseStatus(request.getStatus(), IsmsEvidence.Status.COMPLIANT);
        IsmsEvidence ref = IsmsEvidence.builder()
                .item(item)
                .year(request.getYear())
                .title(request.getTitle())
                .status(status)
                .sourceEvidence(source)
                .registrant(user)
                .build();
        return IsmsDto.EvidenceResponse.from(evidenceRepository.save(ref));
    }

    @Transactional(readOnly = true)
    public List<IsmsDto.EvidenceSearchResult> searchEvidences(Long excludeItemId, int year, String keyword) {
        String kw = keyword == null ? "" : keyword.trim();
        return evidenceRepository.searchForRef(year, excludeItemId, kw).stream()
                .map(e -> IsmsDto.EvidenceSearchResult.builder()
                        .id(e.getId())
                        .itemCode(e.getItem().getItemCode())
                        .itemName(e.getItem().getItemName())
                        .title(e.getTitle())
                        .fileName(e.getFileName())
                        .status(e.getStatus().name())
                        .build())
                .collect(Collectors.toList());
    }

    @Transactional
    public IsmsDto.EvidenceResponse removeEvidenceFile(Long evidenceId) throws IOException {
        IsmsEvidence evidence = evidenceRepository.findById(evidenceId)
                .orElseThrow(() -> new ResourceNotFoundException("IsmsEvidence", evidenceId));
        fileStorageService.delete(evidence.getFilePath());
        evidence.setFilePath(null);
        evidence.setFileName(null);
        return IsmsDto.EvidenceResponse.from(evidence);
    }

    @Transactional(readOnly = true)
    public Resource downloadEvidenceFile(Long evidenceId) {
        IsmsEvidence evidence = evidenceRepository.findById(evidenceId)
                .orElseThrow(() -> new ResourceNotFoundException("IsmsEvidence", evidenceId));
        if (evidence.getFilePath() == null && evidence.getSourceEvidence() != null) {
            evidence = evidence.getSourceEvidence();
        }
        if (evidence.getFilePath() == null) throw new ResourceNotFoundException("IsmsEvidence file", evidenceId);
        return fileStorageService.load(evidence.getFilePath());
    }

    @Transactional(readOnly = true)
    public IsmsEvidence getEvidence(Long evidenceId) {
        return evidenceRepository.findById(evidenceId)
                .orElseThrow(() -> new ResourceNotFoundException("IsmsEvidence", evidenceId));
    }

    @Transactional(readOnly = true)
    public IsmsDto.SummaryResponse summary(int year) {
        List<IsmsItem> allItems = itemRepository.findAllByOrderBySortOrderAsc();
        List<Object[]> rawStats = evidenceRepository.countByItemAndStatusForYear(year);

        Map<Long, Map<String, Long>> statsMap = new HashMap<>();
        for (Object[] row : rawStats) {
            Long itemId = (Long) row[0];
            String status = ((IsmsEvidence.Status) row[1]).name();
            Long count = (Long) row[2];
            statsMap.computeIfAbsent(itemId, k -> new HashMap<>()).put(status, count);
        }

        // Group items by domain
        Map<String, List<IsmsItem>> byDomain = allItems.stream()
                .collect(Collectors.groupingBy(IsmsItem::getDomainCode, LinkedHashMap::new, Collectors.toList()));

        int totalCompliant = 0, totalPartial = 0, totalNonCompliant = 0, totalNa = 0, totalNoEvidence = 0;
        List<IsmsDto.DomainSummary> domainSummaries = new ArrayList<>();

        for (Map.Entry<String, List<IsmsItem>> entry : byDomain.entrySet()) {
            String dc = entry.getKey();
            List<IsmsItem> domainItems = entry.getValue();
            int compliant = 0, partial = 0, nonCompliant = 0, na = 0, noEvidence = 0;

            for (IsmsItem item : domainItems) {
                Map<String, Long> stats = statsMap.getOrDefault(item.getId(), Collections.emptyMap());
                if (stats.isEmpty()) {
                    noEvidence++;
                } else {
                    String status = deriveStatus(stats);
                    switch (status != null ? status : "NO_EVIDENCE") {
                        case "COMPLIANT" -> compliant++;
                        case "PARTIAL" -> partial++;
                        case "NON_COMPLIANT" -> nonCompliant++;
                        case "NA" -> na++;
                        default -> noEvidence++;
                    }
                }
            }

            totalCompliant += compliant;
            totalPartial += partial;
            totalNonCompliant += nonCompliant;
            totalNa += na;
            totalNoEvidence += noEvidence;

            domainSummaries.add(IsmsDto.DomainSummary.builder()
                    .domainCode(dc)
                    .domainName(domainItems.get(0).getDomainName())
                    .sectionNum(domainItems.get(0).getSectionNum())
                    .total(domainItems.size())
                    .compliant(compliant)
                    .partial(partial)
                    .nonCompliant(nonCompliant)
                    .na(na)
                    .noEvidence(noEvidence)
                    .build());
        }

        return IsmsDto.SummaryResponse.builder()
                .year(year)
                .totalItems(allItems.size())
                .compliant(totalCompliant)
                .partial(totalPartial)
                .nonCompliant(totalNonCompliant)
                .na(totalNa)
                .noEvidence(totalNoEvidence)
                .byDomain(domainSummaries)
                .build();
    }

    // ── CSV 내보내기 ─────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public byte[] exportCsv(int year) {
        List<IsmsItem> allItems = itemRepository.findAllByOrderBySortOrderAsc();
        List<IsmsEvidence> evidences = evidenceRepository.findByYearOrderByItemSortOrder(year);

        Map<Long, List<IsmsEvidence>> byItem = evidences.stream()
                .collect(Collectors.groupingBy(e -> e.getItem().getId()));

        StringBuilder sb = new StringBuilder();
        sb.append("항목코드,항목명,섹션,도메인,증적제목,증적내용,파일명/경로,준수상태,등록자\n");

        for (IsmsItem item : allItems) {
            List<IsmsEvidence> evList = byItem.getOrDefault(item.getId(), Collections.emptyList());
            if (evList.isEmpty()) {
                sb.append(String.join(",",
                        csvField(item.getItemCode()), csvField(item.getItemName()),
                        String.valueOf(item.getSectionNum()), csvField(item.getDomainName()),
                        "", "", "", "", "")).append('\n');
            } else {
                for (IsmsEvidence ev : evList) {
                    sb.append(String.join(",",
                            csvField(item.getItemCode()), csvField(item.getItemName()),
                            String.valueOf(item.getSectionNum()), csvField(item.getDomainName()),
                            csvField(ev.getTitle()),
                            csvField(ev.getContent() != null ? ev.getContent() : ""),
                            csvField(ev.getFileName() != null ? ev.getFileName() : ""),
                            ev.getStatus().name(),
                            csvField(ev.getRegistrant() != null ? ev.getRegistrant().getName() : "")
                    )).append('\n');
                }
            }
        }
        // UTF-8 BOM (EF BB BF) 명시적 추가 — 리터럴 char 의존 없이 확실한 Excel 한글 호환
        byte[] bom = {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};
        byte[] content = sb.toString().getBytes(StandardCharsets.UTF_8);
        byte[] result = new byte[bom.length + content.length];
        System.arraycopy(bom, 0, result, 0, bom.length);
        System.arraycopy(content, 0, result, bom.length, content.length);
        return result;
    }

    // ── 일괄등록 엑셀 템플릿 ──────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public byte[] getImportTemplate() throws IOException {
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            // ── 입력 시트 ──────────────────────────────────────────────────────
            XSSFSheet input = wb.createSheet("증적입력");

            XSSFCellStyle headerStyle = wb.createCellStyle();
            headerStyle.setFillForegroundColor(IndexedColors.CORNFLOWER_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            XSSFFont hf = wb.createFont();
            hf.setBold(true);
            hf.setColor(IndexedColors.WHITE.getIndex());
            headerStyle.setFont(hf);

            String[] headers = {"항목코드", "증적제목", "증적내용", "파일명/경로", "준수상태"};
            int[] colWidths = {3000, 8000, 16000, 10000, 4000};
            XSSFRow hRow = input.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                XSSFCell cell = hRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
                input.setColumnWidth(i, colWidths[i]);
            }

            // 샘플 행 2개
            String[][] samples = {
                    {"1.1.1", "정보보호 정책 승인 문서 v2026", "최고경영자 서명이 포함된 정보보호 정책서", "docs/policy/2026_security_policy.pdf", "COMPLIANT"},
                    {"2.5.1", "접근권한 관리 절차서", "사용자 계정 생성·변경·삭제 절차 확인", "docs/access/2026_access_control.pdf", "PARTIAL"}
            };
            for (int r = 0; r < samples.length; r++) {
                XSSFRow row = input.createRow(r + 1);
                for (int c = 0; c < samples[r].length; c++) {
                    row.createCell(c).setCellValue(samples[r][c]);
                }
            }

            // ── 준수상태 안내 시트 ─────────────────────────────────────────────
            XSSFSheet guide = wb.createSheet("입력규칙");
            guide.createRow(0).createCell(0).setCellValue("준수상태 허용값");
            String[][] rules = {
                    {"COMPLIANT", "준수"},
                    {"PARTIAL",   "부분 준수"},
                    {"NON_COMPLIANT", "미준수"},
                    {"NA",        "해당없음 (N/A)"}
            };
            for (int i = 0; i < rules.length; i++) {
                XSSFRow row = guide.createRow(i + 1);
                row.createCell(0).setCellValue(rules[i][0]);
                row.createCell(1).setCellValue(rules[i][1]);
            }

            // ── 항목목록 참고 시트 ────────────────────────────────────────────
            XSSFSheet ref = wb.createSheet("항목목록(참고)");
            XSSFRow refH = ref.createRow(0);
            refH.createCell(0).setCellValue("항목코드");
            refH.createCell(1).setCellValue("항목명");
            refH.createCell(2).setCellValue("도메인");

            List<IsmsItem> items = itemRepository.findAllByOrderBySortOrderAsc();
            for (int i = 0; i < items.size(); i++) {
                XSSFRow row = ref.createRow(i + 1);
                row.createCell(0).setCellValue(items.get(i).getItemCode());
                row.createCell(1).setCellValue(items.get(i).getItemName());
                row.createCell(2).setCellValue(items.get(i).getDomainName());
            }
            ref.setColumnWidth(0, 3000);
            ref.setColumnWidth(1, 10000);
            ref.setColumnWidth(2, 10000);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            wb.write(baos);
            return baos.toByteArray();
        }
    }

    // ── 일괄등록 ────────────────────────────────────────────────────────────────

    @Transactional
    public IsmsDto.BulkImportResult bulkImport(int year, MultipartFile file, User user) throws IOException {
        String filename = file.getOriginalFilename() != null ? file.getOriginalFilename().toLowerCase() : "";
        List<String[]> rows = new ArrayList<>();

        if (filename.endsWith(".xlsx")) {
            try (XSSFWorkbook wb = new XSSFWorkbook(file.getInputStream())) {
                XSSFSheet sheet = wb.getSheetAt(0);
                for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                    XSSFRow row = sheet.getRow(i);
                    if (row == null) continue;
                    String code = xlsxCell(row, 0);
                    if (code.isBlank()) continue; // 빈 행 건너뜀
                    rows.add(new String[]{code, xlsxCell(row, 1), xlsxCell(row, 2), xlsxCell(row, 3), xlsxCell(row, 4)});
                }
            }
        } else if (filename.endsWith(".csv")) {
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                boolean first = true;
                while ((line = reader.readLine()) != null) {
                    if (first) { first = false; continue; } // 헤더 건너뜀
                    if (line.isBlank()) continue;
                    String[] parsed = parseCsvLine(line);
                    if (parsed.length > 0 && !parsed[0].trim().isBlank()) rows.add(parsed);
                }
            }
        } else {
            throw new IllegalArgumentException(".xlsx 또는 .csv 파일만 지원합니다.");
        }

        int success = 0, failed = 0;
        List<IsmsDto.BulkImportResult.RowError> errors = new ArrayList<>();

        for (int i = 0; i < rows.size(); i++) {
            String[] row = rows.get(i);
            int rowNum = i + 2;
            String itemCode = safeGet(row, 0).trim();
            String title    = safeGet(row, 1).trim();

            if (itemCode.isBlank()) {
                errors.add(IsmsDto.BulkImportResult.RowError.builder().row(rowNum).itemCode("").message("항목코드가 비어있습니다").build());
                failed++;
                continue;
            }
            if (title.isBlank()) {
                errors.add(IsmsDto.BulkImportResult.RowError.builder().row(rowNum).itemCode(itemCode).message("증적제목이 비어있습니다").build());
                failed++;
                continue;
            }

            Optional<IsmsItem> itemOpt = itemRepository.findByItemCode(itemCode);
            if (itemOpt.isEmpty()) {
                errors.add(IsmsDto.BulkImportResult.RowError.builder().row(rowNum).itemCode(itemCode).message("존재하지 않는 항목코드: " + itemCode).build());
                failed++;
                continue;
            }

            IsmsEvidence.Status status = parseStatus(safeGet(row, 4).trim(), IsmsEvidence.Status.COMPLIANT);
            evidenceRepository.save(IsmsEvidence.builder()
                    .item(itemOpt.get())
                    .year(year)
                    .title(title)
                    .content(safeGet(row, 2))
                    .fileName(safeGet(row, 3))
                    .status(status)
                    .registrant(user)
                    .build());
            success++;
        }

        return IsmsDto.BulkImportResult.builder()
                .total(rows.size())
                .success(success)
                .failed(failed)
                .errors(errors)
                .build();
    }

    // ── 헬퍼 ─────────────────────────────────────────────────────────────────────

    private static String csvField(String s) {
        if (s == null || s.isEmpty()) return "";
        if (s.contains(",") || s.contains("\"") || s.contains("\n") || s.contains("\r")) {
            return "\"" + s.replace("\"", "\"\"") + "\"";
        }
        return s;
    }

    private static String xlsxCell(XSSFRow row, int col) {
        XSSFCell cell = row.getCell(col);
        if (cell == null) return "";
        if (cell.getCellType() == CellType.NUMERIC) {
            double v = cell.getNumericCellValue();
            return v == Math.floor(v) ? String.valueOf((long) v) : String.valueOf(v);
        }
        return cell.toString().trim();
    }

    private static String[] parseCsvLine(String line) {
        if (line.startsWith("﻿")) line = line.substring(1);
        List<String> fields = new ArrayList<>();
        boolean inQ = false;
        StringBuilder sb = new StringBuilder();
        for (char c : line.toCharArray()) {
            if (c == '"') { inQ = !inQ; }
            else if (c == ',' && !inQ) { fields.add(sb.toString()); sb = new StringBuilder(); }
            else { sb.append(c); }
        }
        fields.add(sb.toString());
        return fields.toArray(new String[0]);
    }

    private static String safeGet(String[] arr, int idx) {
        return (arr != null && idx < arr.length && arr[idx] != null) ? arr[idx] : "";
    }

    private String deriveStatus(Map<String, Long> stats) {
        if (stats.isEmpty()) return null;
        if (stats.getOrDefault("NON_COMPLIANT", 0L) > 0) return "NON_COMPLIANT";
        if (stats.getOrDefault("PARTIAL", 0L) > 0) return "PARTIAL";
        if (stats.getOrDefault("COMPLIANT", 0L) > 0) return "COMPLIANT";
        if (stats.getOrDefault("NA", 0L) > 0) return "NA";
        return null;
    }

    private IsmsEvidence.Status parseStatus(String s, IsmsEvidence.Status defaultVal) {
        if (s == null) return defaultVal;
        try {
            return IsmsEvidence.Status.valueOf(s.toUpperCase());
        } catch (IllegalArgumentException e) {
            return defaultVal;
        }
    }
}
