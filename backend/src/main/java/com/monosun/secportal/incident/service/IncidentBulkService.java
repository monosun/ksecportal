package com.monosun.secportal.incident.service;

import com.monosun.secportal.asset.dto.AssetBulkUploadResult;
import com.monosun.secportal.audit.service.AuditLogService;
import com.monosun.secportal.auth.entity.User;
import com.monosun.secportal.incident.entity.Incident;
import com.monosun.secportal.incident.repository.IncidentRepository;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class IncidentBulkService {

    private final IncidentRepository incidentRepository;
    private final AuditLogService auditLogService;

    private static final String[] HEADERS = {
            "제목*", "유형*", "심각도*", "설명", "영향 시스템",
            "상태", "탐지 시각(YYYY-MM-DD HH:mm)"
    };
    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public byte[] generateTemplate() throws IOException {
        try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = wb.createSheet("보안 인시던트 목록");

            CellStyle hs = wb.createCellStyle();
            hs.setFillForegroundColor(IndexedColors.CORNFLOWER_BLUE.getIndex());
            hs.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            hs.setAlignment(HorizontalAlignment.CENTER);
            Font hf = wb.createFont(); hf.setBold(true); hf.setColor(IndexedColors.WHITE.getIndex());
            hs.setFont(hf);

            Row hr = sheet.createRow(0);
            for (int i = 0; i < HEADERS.length; i++) {
                Cell c = hr.createCell(i);
                c.setCellValue(HEADERS[i]);
                c.setCellStyle(hs);
                sheet.setColumnWidth(i, 7000);
            }

            Row ex = sheet.createRow(1);
            ex.createCell(0).setCellValue("랜섬웨어 감염 사고");
            ex.createCell(1).setCellValue("MALWARE");
            ex.createCell(2).setCellValue("HIGH");
            ex.createCell(3).setCellValue("파일 서버 랜섬웨어 감염 발생");
            ex.createCell(4).setCellValue("파일 서버, NAS");
            ex.createCell(5).setCellValue("OPEN");
            ex.createCell(6).setCellValue("2024-03-15 09:00");

            Sheet rs = wb.createSheet("입력 규칙");
            CellStyle bold = wb.createCellStyle();
            Font bf = wb.createFont(); bf.setBold(true); bold.setFont(bf);
            String[][] rules = {
                    {"컬럼", "필수", "허용 값"},
                    {"제목", "필수", "자유 텍스트"},
                    {"유형", "필수", "MALWARE, PHISHING, DATA_BREACH, UNAUTHORIZED_ACCESS, DDOS, INSIDER_THREAT, PHYSICAL, OTHER"},
                    {"심각도", "필수", "CRITICAL, HIGH, MEDIUM, LOW"},
                    {"설명", "선택", "자유 텍스트"},
                    {"영향 시스템", "선택", "자유 텍스트"},
                    {"상태", "선택", "OPEN, INVESTIGATING, CONTAINED, RESOLVED, CLOSED (기본값: OPEN)"},
                    {"탐지 시각", "선택", "YYYY-MM-DD HH:mm 형식 (예: 2024-03-15 09:00)"}
            };
            for (int r = 0; r < rules.length; r++) {
                Row row = rs.createRow(r);
                for (int c = 0; c < rules[r].length; c++) {
                    Cell cell = row.createCell(c);
                    cell.setCellValue(rules[r][c]);
                    if (r == 0) cell.setCellStyle(bold);
                    rs.setColumnWidth(c, c == 2 ? 25000 : 5000);
                }
            }

            wb.write(out);
            return out.toByteArray();
        }
    }

    @Transactional
    public AssetBulkUploadResult upload(MultipartFile file, User reporter) throws IOException {
        List<AssetBulkUploadResult.RowError> errors = new ArrayList<>();
        int total = 0, success = 0;

        try (Workbook wb = WorkbookFactory.create(file.getInputStream())) {
            Sheet sheet = wb.getSheetAt(0);
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null || isEmptyRow(row)) continue;
                total++;
                try {
                    incidentRepository.save(parseRow(row, reporter));
                    success++;
                } catch (Exception e) {
                    errors.add(new AssetBulkUploadResult.RowError(i + 1, e.getMessage()));
                }
            }
        }

        if (success > 0) auditLogService.log("INCIDENT_BULK_UPLOAD", "INCIDENT", 0L, success + "건 일괄 등록");
        return new AssetBulkUploadResult(total, success, total - success, errors);
    }

    private Incident parseRow(Row row, User reporter) {
        String title = getString(row, 0);
        if (title == null || title.isBlank()) throw new IllegalArgumentException("제목은 필수입니다");

        String typeStr = getString(row, 1);
        if (typeStr == null || typeStr.isBlank()) throw new IllegalArgumentException("유형은 필수입니다");
        Incident.IncidentType type;
        try { type = Incident.IncidentType.valueOf(typeStr.trim().toUpperCase()); }
        catch (Exception e) { throw new IllegalArgumentException("유효하지 않은 유형: " + typeStr); }

        String sevStr = getString(row, 2);
        if (sevStr == null || sevStr.isBlank()) throw new IllegalArgumentException("심각도는 필수입니다");
        Incident.Severity severity;
        try { severity = Incident.Severity.valueOf(sevStr.trim().toUpperCase()); }
        catch (Exception e) { throw new IllegalArgumentException("유효하지 않은 심각도: " + sevStr); }

        String description = getString(row, 3);
        String affectedSystems = getString(row, 4);

        Incident.Status status = Incident.Status.OPEN;
        String statusStr = getString(row, 5);
        if (statusStr != null && !statusStr.isBlank()) {
            try { status = Incident.Status.valueOf(statusStr.trim().toUpperCase()); }
            catch (Exception e) { throw new IllegalArgumentException("유효하지 않은 상태: " + statusStr); }
        }

        LocalDateTime detectedAt = null;
        String dtStr = getString(row, 6);
        if (dtStr != null && !dtStr.isBlank()) {
            try { detectedAt = LocalDateTime.parse(dtStr.trim(), DT_FMT); }
            catch (Exception e) { throw new IllegalArgumentException("탐지 시각 형식 오류 (YYYY-MM-DD HH:mm): " + dtStr); }
        }

        return Incident.builder()
                .title(title.trim())
                .type(type)
                .severity(severity)
                .status(status)
                .description(description)
                .affectedSystems(affectedSystems)
                .detectedAt(detectedAt)
                .reporter(reporter)
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
