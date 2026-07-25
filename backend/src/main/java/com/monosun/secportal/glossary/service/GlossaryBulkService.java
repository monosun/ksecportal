package com.monosun.secportal.glossary.service;

import com.monosun.secportal.audit.service.AuditLogService;
import com.monosun.secportal.glossary.dto.GlossaryDto;
import com.monosun.secportal.glossary.entity.GlossaryTerm;
import com.monosun.secportal.glossary.repository.GlossaryTermRepository;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 보안 용어집 엑셀 템플릿 다운로드 / 일괄 등록.
 *
 * 용어(한글)를 키로 삼아, 이미 있는 용어는 <b>덮어쓰기 여부에 따라</b> 갱신하거나 건너뛴다.
 * 파일 안에서 같은 용어가 반복되면 첫 행만 반영한다.
 */
@Service
@RequiredArgsConstructor
public class GlossaryBulkService {

    private final GlossaryTermRepository repository;
    private final AuditLogService auditLogService;

    private static final String[] HEADERS = {
            "한글 용어*", "영문 표기", "약어", "분류", "의미", "관련 키워드", "정렬 순서", "사용여부(Y/N)"
    };
    private static final int LAST_COL = HEADERS.length - 1;

    public byte[] generateTemplate() throws IOException {
        try (XSSFWorkbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = wb.createSheet("보안 용어집");

            CellStyle headerStyle = wb.createCellStyle();
            Font headerFont = wb.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            Row header = sheet.createRow(0);
            for (int i = 0; i < HEADERS.length; i++) {
                Cell cell = header.createCell(i);
                cell.setCellValue(HEADERS[i]);
                cell.setCellStyle(headerStyle);
            }

            // 예시 행 — 실제 기본 용어와 같은 형식으로 채워 둔다
            Row ex1 = sheet.createRow(1);
            ex1.createCell(0).setCellValue("다중요소인증");
            ex1.createCell(1).setCellValue("Multi-Factor Authentication");
            ex1.createCell(2).setCellValue("MFA");
            ex1.createCell(3).setCellValue("접근통제");
            ex1.createCell(4).setCellValue("서로 다른 인증 요소를 두 개 이상 사용해 신원을 확인하는 방식");
            ex1.createCell(5).setCellValue("OTP, 생체인증");
            ex1.createCell(6).setCellValue(1);
            ex1.createCell(7).setCellValue("Y");

            Row ex2 = sheet.createRow(2);
            ex2.createCell(0).setCellValue("망분리");
            ex2.createCell(1).setCellValue("Network Segregation");
            ex2.createCell(2).setCellValue("");
            ex2.createCell(3).setCellValue("네트워크 보안");
            ex2.createCell(4).setCellValue("업무망과 인터넷망을 분리해 침해 확산을 차단하는 통제");
            ex2.createCell(5).setCellValue("물리적 망분리, 논리적 망분리");
            ex2.createCell(6).setCellValue(2);
            ex2.createCell(7).setCellValue("Y");

            int[] widths = {22, 34, 10, 16, 60, 30, 10, 14};
            for (int i = 0; i < widths.length; i++) sheet.setColumnWidth(i, widths[i] * 256);
            sheet.createFreezePane(0, 1);

            wb.write(out);
            return out.toByteArray();
        }
    }

    /**
     * @param overwrite true 면 이미 있는 용어의 내용을 갱신, false 면 건너뛴다.
     */
    @Transactional
    public GlossaryDto.BulkResult upload(MultipartFile file, boolean overwrite) throws IOException {
        List<GlossaryDto.BulkResult.RowError> errors = new ArrayList<>();
        List<GlossaryTerm> toSave = new ArrayList<>();
        Set<String> seenInFile = new HashSet<>();
        int total = 0, success = 0, updated = 0, skipped = 0;

        int nextOrder = repository.findAllByOrderBySortOrderAscIdAsc().stream()
                .mapToInt(t -> t.getSortOrder() == null ? 0 : t.getSortOrder())
                .max().orElse(0) + 1;

        try (Workbook wb = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = wb.getSheetAt(0);
            for (int r = 1; r <= sheet.getLastRowNum(); r++) {
                Row row = sheet.getRow(r);
                if (row == null || isEmptyRow(row)) continue;
                int rowNo = r + 1;
                total++;
                try {
                    String name = str(row, 0);
                    if (name.isBlank()) {
                        errors.add(new GlossaryDto.BulkResult.RowError(rowNo, "한글 용어가 비어 있습니다."));
                        continue;
                    }
                    // 파일 안 중복은 첫 행만 반영한다
                    if (!seenInFile.add(name.toLowerCase())) {
                        skipped++;
                        continue;
                    }

                    String nameEn = str(row, 1);
                    String abbr = str(row, 2);
                    String category = str(row, 3);
                    String definition = str(row, 4);
                    String keywords = str(row, 5);
                    Integer sortOrder = parseInt(str(row, 6));
                    Boolean active = parseYn(str(row, 7));

                    GlossaryTerm existing = repository.findFirstByNameIgnoreCase(name).orElse(null);
                    if (existing != null) {
                        if (!overwrite) {
                            skipped++;
                            continue;
                        }
                        // 값이 있는 칸만 갱신 — 빈 칸으로 기존 내용을 지우지 않는다
                        if (!nameEn.isBlank()) existing.setNameEn(nameEn);
                        if (!abbr.isBlank()) existing.setAbbreviation(abbr);
                        if (!category.isBlank()) existing.setCategory(category);
                        if (!definition.isBlank()) existing.setDefinition(definition);
                        if (!keywords.isBlank()) existing.setKeywords(keywords);
                        if (sortOrder != null) existing.setSortOrder(sortOrder);
                        if (active != null) existing.setActive(active);
                        updated++;
                        continue;
                    }

                    toSave.add(GlossaryTerm.builder()
                            .name(name)
                            .nameEn(blankToNull(nameEn))
                            .abbreviation(blankToNull(abbr))
                            .category(blankToNull(category))
                            .definition(blankToNull(definition))
                            .keywords(blankToNull(keywords))
                            .sortOrder(sortOrder != null ? sortOrder : nextOrder++)
                            .active(active == null || active)
                            .build());
                    success++;
                } catch (Exception e) {
                    errors.add(new GlossaryDto.BulkResult.RowError(rowNo, e.getMessage()));
                }
            }
        }

        repository.saveAll(toSave);
        auditLogService.log("CREATE", "GLOSSARY_TERM", null,
                "보안용어 일괄등록 — 신규 " + success + ", 갱신 " + updated
                        + ", 제외 " + skipped + ", 실패 " + errors.size());

        return GlossaryDto.BulkResult.builder()
                .total(total)
                .success(success)
                .updated(updated)
                .skipped(skipped)
                .failed(errors.size())
                .errors(errors)
                .build();
    }

    // ── 헬퍼 ────────────────────────────────────────────────────────────────

    private boolean isEmptyRow(Row row) {
        for (int c = 0; c <= LAST_COL; c++) {
            if (!str(row, c).isBlank()) return false;
        }
        return true;
    }

    private String str(Row row, int col) {
        Cell cell = row.getCell(col);
        if (cell == null) return "";
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue().trim();
            case NUMERIC -> {
                double d = cell.getNumericCellValue();
                yield d == Math.floor(d) ? String.valueOf((long) d) : String.valueOf(d);
            }
            case BOOLEAN -> cell.getBooleanCellValue() ? "Y" : "N";
            case FORMULA -> cell.getRichStringCellValue().getString().trim();
            default -> "";
        };
    }

    private Integer parseInt(String v) {
        if (v == null || v.isBlank()) return null;
        try {
            return Integer.parseInt(v.trim());
        } catch (NumberFormatException e) {
            return null;   // 숫자가 아니면 자동 번호를 쓴다
        }
    }

    /** 사용여부 — Y/N, 사용/미사용, true/false 를 받는다. 비어 있으면 null(기본값 사용). */
    private Boolean parseYn(String v) {
        if (v == null || v.isBlank()) return null;
        String s = v.trim().toLowerCase();
        if (s.startsWith("y") || s.startsWith("t") || s.equals("1") || s.equals("사용")) return true;
        if (s.startsWith("n") || s.startsWith("f") || s.equals("0") || s.equals("미사용")) return false;
        return null;
    }

    private String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s;
    }
}
