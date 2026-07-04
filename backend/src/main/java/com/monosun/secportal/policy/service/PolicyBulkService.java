package com.monosun.secportal.policy.service;

import com.monosun.secportal.asset.dto.AssetBulkUploadResult;
import com.monosun.secportal.audit.service.AuditLogService;
import com.monosun.secportal.auth.entity.User;
import com.monosun.secportal.policy.entity.Policy;
import com.monosun.secportal.policy.repository.PolicyRepository;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PolicyBulkService {

    private final PolicyRepository policyRepository;
    private final AuditLogService auditLogService;

    private static final String[] HEADERS = {
            "제목*", "카테고리*", "내용", "상태", "버전", "시행일(YYYY-MM-DD)"
    };

    public byte[] generateTemplate() throws IOException {
        try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = wb.createSheet("보안 정책 목록");

            CellStyle headerStyle = wb.createCellStyle();
            headerStyle.setFillForegroundColor(IndexedColors.CORNFLOWER_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);
            Font hf = wb.createFont();
            hf.setBold(true);
            hf.setColor(IndexedColors.WHITE.getIndex());
            headerStyle.setFont(hf);

            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < HEADERS.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(HEADERS[i]);
                cell.setCellStyle(headerStyle);
                sheet.setColumnWidth(i, 6000);
            }

            // 예시 행
            Row ex = sheet.createRow(1);
            ex.createCell(0).setCellValue("정보보호 정책 v2024");
            ex.createCell(1).setCellValue("GENERAL");
            ex.createCell(2).setCellValue("전사 정보보호 정책입니다.");
            ex.createCell(3).setCellValue("DRAFT");
            ex.createCell(4).setCellValue("1.0");
            ex.createCell(5).setCellValue("2024-01-01");

            // 입력 규칙 시트
            Sheet ruleSheet = wb.createSheet("입력 규칙");
            CellStyle bold = wb.createCellStyle();
            Font bf = wb.createFont(); bf.setBold(true); bold.setFont(bf);
            String[][] rules = {
                    {"컬럼", "필수", "허용 값"},
                    {"제목", "필수", "자유 텍스트"},
                    {"카테고리", "필수", "GENERAL, ACCESS_CONTROL, DATA_PROTECTION, INCIDENT_RESPONSE, NETWORK, PHYSICAL, VENDOR, OTHER"},
                    {"내용", "선택", "자유 텍스트 (기본값: 빈 문자열)"},
                    {"상태", "선택", "DRAFT, REVIEW, PUBLISHED, ARCHIVED (기본값: DRAFT)"},
                    {"버전", "선택", "자유 텍스트 (기본값: 1.0)"},
                    {"시행일", "선택", "YYYY-MM-DD 형식 (예: 2024-01-01)"}
            };
            for (int r = 0; r < rules.length; r++) {
                Row row = ruleSheet.createRow(r);
                for (int c = 0; c < rules[r].length; c++) {
                    Cell cell = row.createCell(c);
                    cell.setCellValue(rules[r][c]);
                    if (r == 0) cell.setCellStyle(bold);
                    ruleSheet.setColumnWidth(c, c == 2 ? 20000 : 5000);
                }
            }

            wb.write(out);
            return out.toByteArray();
        }
    }

    @Transactional
    public AssetBulkUploadResult upload(MultipartFile file, User author) throws IOException {
        List<AssetBulkUploadResult.RowError> errors = new ArrayList<>();
        int total = 0, success = 0;

        try (Workbook wb = WorkbookFactory.create(file.getInputStream())) {
            Sheet sheet = wb.getSheetAt(0);
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null || isEmptyRow(row)) continue;
                total++;
                try {
                    policyRepository.save(parseRow(row, author));
                    success++;
                } catch (Exception e) {
                    errors.add(new AssetBulkUploadResult.RowError(i + 1, e.getMessage()));
                }
            }
        }

        if (success > 0) {
            auditLogService.log("POLICY_BULK_UPLOAD", "POLICY", 0L, success + "건 일괄 등록");
        }
        return new AssetBulkUploadResult(total, success, total - success, errors);
    }

    private Policy parseRow(Row row, User author) {
        String title = getString(row, 0);
        if (title == null || title.isBlank()) throw new IllegalArgumentException("제목은 필수입니다");

        String catStr = getString(row, 1);
        if (catStr == null || catStr.isBlank()) throw new IllegalArgumentException("카테고리는 필수입니다");
        Policy.Category category;
        try {
            category = Policy.Category.valueOf(catStr.trim().toUpperCase());
        } catch (Exception e) {
            throw new IllegalArgumentException("유효하지 않은 카테고리: " + catStr);
        }

        String content = getString(row, 2);
        if (content == null) content = "";

        Policy.Status status = Policy.Status.DRAFT;
        String statusStr = getString(row, 3);
        if (statusStr != null && !statusStr.isBlank()) {
            try { status = Policy.Status.valueOf(statusStr.trim().toUpperCase()); }
            catch (Exception e) { throw new IllegalArgumentException("유효하지 않은 상태: " + statusStr); }
        }

        String version = getString(row, 4);
        if (version == null || version.isBlank()) version = "1.0";

        LocalDate effectiveDate = null;
        String dateStr = getString(row, 5);
        if (dateStr != null && !dateStr.isBlank()) {
            try { effectiveDate = LocalDate.parse(dateStr.trim()); }
            catch (Exception e) { throw new IllegalArgumentException("날짜 형식 오류 (YYYY-MM-DD): " + dateStr); }
        }

        return Policy.builder()
                .title(title.trim())
                .content(content)
                .category(category)
                .status(status)
                .version(version.trim())
                .effectiveDate(effectiveDate)
                .author(author)
                .build();
    }

    private String getString(Row row, int col) {
        Cell cell = row.getCell(col, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
        if (cell == null) return null;
        return switch (cell.getCellType()) {
            case STRING -> { String v = cell.getStringCellValue().trim(); yield v.isEmpty() ? null : v; }
            case NUMERIC -> DateUtil.isCellDateFormatted(cell)
                    ? cell.getLocalDateTimeCellValue().toLocalDate().toString()
                    : String.valueOf((long) cell.getNumericCellValue());
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            default -> null;
        };
    }

    private boolean isEmptyRow(Row row) {
        for (int i = 0; i < HEADERS.length; i++) {
            Cell c = row.getCell(i);
            if (c != null && c.getCellType() != CellType.BLANK
                    && !(c.getCellType() == CellType.STRING && c.getStringCellValue().isBlank())) {
                return false;
            }
        }
        return true;
    }
}
