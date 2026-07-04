package com.monosun.secportal.admin.service;

import com.monosun.secportal.asset.dto.AssetBulkUploadResult;
import com.monosun.secportal.audit.service.AuditLogService;
import com.monosun.secportal.auth.entity.User;
import com.monosun.secportal.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserBulkService {

    private final UserRepository userRepository;
    private final AuditLogService auditLogService;
    private final PasswordEncoder passwordEncoder;

    private static final String[] HEADERS = {
            "이메일*", "이름*", "비밀번호*", "역할", "부서"
    };

    public byte[] generateTemplate() throws IOException {
        try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = wb.createSheet("사용자 목록");

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
            ex.createCell(0).setCellValue("user1@company.com");
            ex.createCell(1).setCellValue("홍길동");
            ex.createCell(2).setCellValue("SecurePass1!");
            ex.createCell(3).setCellValue("USER");
            ex.createCell(4).setCellValue("IT팀");

            Sheet rs = wb.createSheet("입력 규칙");
            CellStyle bold = wb.createCellStyle();
            Font bf = wb.createFont(); bf.setBold(true); bold.setFont(bf);
            String[][] rules = {
                    {"컬럼", "필수", "허용 값"},
                    {"이메일", "필수", "유효한 이메일 주소 (중복 불가)"},
                    {"이름", "필수", "자유 텍스트"},
                    {"비밀번호", "필수", "최소 8자 이상"},
                    {"역할", "선택", "ADMIN, MANAGER, USER (기본값: USER)"},
                    {"부서", "선택", "자유 텍스트"}
            };
            for (int r = 0; r < rules.length; r++) {
                Row row = rs.createRow(r);
                for (int c = 0; c < rules[r].length; c++) {
                    Cell cell = row.createCell(c);
                    cell.setCellValue(rules[r][c]);
                    if (r == 0) cell.setCellStyle(bold);
                    rs.setColumnWidth(c, c == 2 ? 18000 : 5000);
                }
            }

            wb.write(out);
            return out.toByteArray();
        }
    }

    @Transactional
    public AssetBulkUploadResult upload(MultipartFile file, User requester) throws IOException {
        List<AssetBulkUploadResult.RowError> errors = new ArrayList<>();
        int total = 0, success = 0;

        try (Workbook wb = WorkbookFactory.create(file.getInputStream())) {
            Sheet sheet = wb.getSheetAt(0);
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null || isEmptyRow(row)) continue;
                total++;
                try {
                    User user = parseRow(row);
                    userRepository.save(user);
                    success++;
                } catch (Exception e) {
                    errors.add(new AssetBulkUploadResult.RowError(i + 1, e.getMessage()));
                }
            }
        }

        if (success > 0)
            auditLogService.log("USER_BULK_UPLOAD", "USER", 0L, success + "건 일괄 등록", requester);
        return new AssetBulkUploadResult(total, success, total - success, errors);
    }

    private User parseRow(Row row) {
        String email = getString(row, 0);
        if (email == null || email.isBlank()) throw new IllegalArgumentException("이메일은 필수입니다");
        if (userRepository.existsByEmail(email.trim())) throw new IllegalArgumentException("이미 사용 중인 이메일: " + email);

        String name = getString(row, 1);
        if (name == null || name.isBlank()) throw new IllegalArgumentException("이름은 필수입니다");

        String password = getString(row, 2);
        if (password == null || password.isBlank()) throw new IllegalArgumentException("비밀번호는 필수입니다");
        if (password.length() < 8) throw new IllegalArgumentException("비밀번호는 최소 8자 이상이어야 합니다");

        User.Role role = User.Role.USER;
        String roleStr = getString(row, 3);
        if (roleStr != null && !roleStr.isBlank()) {
            try { role = User.Role.valueOf(roleStr.trim().toUpperCase()); }
            catch (Exception e) { throw new IllegalArgumentException("유효하지 않은 역할: " + roleStr); }
        }

        String department = getString(row, 4);

        return User.builder()
                .email(email.trim())
                .name(name.trim())
                .password(passwordEncoder.encode(password))
                .role(role)
                .department(department)
                .active(true)
                .build();
    }

    private String getString(Row row, int col) {
        Cell cell = row.getCell(col, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
        if (cell == null) return null;
        return switch (cell.getCellType()) {
            case STRING -> { String v = cell.getStringCellValue().trim(); yield v.isEmpty() ? null : v; }
            case NUMERIC -> String.valueOf((long) cell.getNumericCellValue());
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
