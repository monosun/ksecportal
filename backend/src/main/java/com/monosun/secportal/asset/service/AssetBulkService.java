package com.monosun.secportal.asset.service;

import com.monosun.secportal.asset.dto.AssetBulkUploadResult;
import com.monosun.secportal.asset.entity.Asset;
import com.monosun.secportal.asset.repository.AssetRepository;
import com.monosun.secportal.audit.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AssetBulkService {

    private final AssetRepository assetRepository;
    private final AuditLogService auditLogService;

    // 컬럼 인덱스 상수
    private static final int COL_NAME            = 0;
    private static final int COL_ASSET_CATEGORY  = 1;
    private static final int COL_TYPE            = 2;
    private static final int COL_ENVIRONMENT     = 3;
    private static final int COL_CLOUD_PROVIDER  = 4;
    private static final int COL_CLOUD_RES_ID    = 5;
    private static final int COL_REGION          = 6;
    private static final int COL_IP              = 7;
    private static final int COL_OS              = 8;
    private static final int COL_SPEC            = 9;
    private static final int COL_OWNER           = 10;
    private static final int COL_DEPARTMENT      = 11;
    private static final int COL_OPERATOR        = 12;
    private static final int COL_LOCATION        = 13;
    private static final int COL_DESCRIPTION     = 14;
    private static final int COL_STATUS          = 15;
    private static final int COL_CRITICALITY     = 16;
    private static final int COL_CONFIDENTIALITY = 17;
    private static final int COL_INTEGRITY       = 18;
    private static final int COL_AVAILABILITY    = 19;
    private static final int COL_PI_INCLUDED     = 20;
    private static final int COL_PI_TYPE         = 21;
    private static final int COL_PI_PROCESSING   = 22;
    private static final int COL_LINKED_SYSTEMS  = 23;
    private static final int COL_ACCESS_CTRL     = 24;
    private static final int COL_BACKUP          = 25;
    private static final int COL_LOG_MGMT        = 26;
    private static final int COL_MONTHLY_COST    = 27;
    private static final int COL_CONTRACT_EXPIRY = 28;
    private static final int COL_LAST_INSPECTION = 29;
    private static final int COL_NEXT_INSPECTION = 30;
    private static final int COL_LAST_REVIEW     = 31;
    private static final int COL_REMARKS         = 32;

    private static final String[] HEADERS = {
            "자산명*",             // 0
            "자산유형",             // 1
            "자산분류*",            // 2
            "운영환경",             // 3
            "클라우드 공급자",       // 4
            "클라우드 리소스 ID",    // 5
            "리전",                // 6
            "IP 주소",             // 7
            "운영체제",             // 8
            "사양",                // 9
            "자산소유자(Owner)",    // 10
            "관리부서",             // 11
            "운영담당자",           // 12
            "위치",                // 13
            "자산설명",             // 14
            "상태",                // 15
            "중요도",              // 16
            "기밀성(C)",           // 17
            "무결성(I)",           // 18
            "가용성(A)",           // 19
            "개인정보 포함 여부(O/X)",  // 20
            "개인정보 유형",         // 21
            "개인정보 처리 여부(O/X)", // 22
            "연계 시스템",           // 23
            "접근권한 관리 대상(O/X)", // 24
            "백업 대상(O/X)",       // 25
            "로그 관리 대상(O/X)",   // 26
            "월 비용(원)",          // 27
            "계약 만료일(YYYY-MM-DD)",  // 28
            "최근 점검일(YYYY-MM-DD)", // 29
            "다음 점검일(YYYY-MM-DD)", // 30
            "최종 검토일(YYYY-MM-DD)", // 31
            "비고"                 // 32
    };

    private static final String[] VALID_TYPES =
            {"SERVER", "WORKSTATION", "NETWORK", "APPLICATION", "DATABASE",
             "EC2", "RDS", "S3", "ELB", "LAMBDA", "CLOUD_OTHER", "OTHER"};

    // ── 템플릿 생성 ────────────────────────────────────────────────────────────

    public byte[] generateTemplate() throws IOException {
        try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            Sheet sheet = wb.createSheet("자산 목록");

            CellStyle headerStyle = wb.createCellStyle();
            headerStyle.setFillForegroundColor(IndexedColors.CORNFLOWER_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            Font headerFont = wb.createFont();
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.WHITE.getIndex());
            headerStyle.setFont(headerFont);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);

            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < HEADERS.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(HEADERS[i]);
                cell.setCellStyle(headerStyle);
                sheet.setColumnWidth(i, 5500);
            }
            // 넓은 컬럼 조정
            sheet.setColumnWidth(COL_CLOUD_RES_ID, 8000);
            sheet.setColumnWidth(COL_LINKED_SYSTEMS, 7000);
            sheet.setColumnWidth(COL_REMARKS, 7000);

            // 예시 행 1 — 온프레미스 서버
            addExampleRow(sheet, 1, new String[]{
                    "메인 웹 서버", "HW", "SERVER", "PRODUCTION", "ON_PREMISE", "", "",
                    "192.168.1.10", "Ubuntu 22.04 LTS", "CPU: 8코어 / RAM: 32GB",
                    "System Admin", "IT", "김철수", "IDC 서울",
                    "프로덕션 웹 애플리케이션 서버",
                    "OPERATIONAL", "HIGH", "HIGH", "MEDIUM", "HIGH",
                    "O", "고객정보", "O", "HR 시스템, 결제 시스템",
                    "O", "O", "O",
                    "", "", "", "", "", ""
            });

            // 예시 행 2 — AWS EC2
            addExampleRow(sheet, 2, new String[]{
                    "SecPortal EC2", "SW", "EC2", "PRODUCTION", "AWS", "i-0a1b2c3d4e5f67890", "ap-northeast-2",
                    "13.124.10.100", "Amazon Linux 2023", "t3.medium (vCPU: 2 / RAM: 4GB)",
                    "System Admin", "IT", "이영희", "AWS ap-northeast-2",
                    "메인 서비스 EC2 인스턴스",
                    "OPERATIONAL", "HIGH", "HIGH", "HIGH", "HIGH",
                    "X", "", "X", "",
                    "O", "O", "O",
                    "36000", "", "", "", "", ""
            });

            // 입력 규칙 시트
            Sheet hintSheet = wb.createSheet("입력 규칙");
            CellStyle boldStyle = wb.createCellStyle();
            Font boldFont = wb.createFont();
            boldFont.setBold(true);
            boldStyle.setFont(boldFont);

            String[][] hints = {
                    {"컬럼", "필수", "허용 값 / 형식"},
                    {"자산명", "필수", "자유 텍스트"},
                    {"자산유형", "선택", "INFO(정보), SW, HW, SERVICE(서비스), PERSONNEL(인력), FACILITY(시설)"},
                    {"자산분류", "필수", String.join(", ", VALID_TYPES)},
                    {"운영환경", "선택", "PRODUCTION, STAGING, DEVELOPMENT, TEST (기본값: PRODUCTION)"},
                    {"클라우드 공급자", "선택", "ON_PREMISE, AWS, GCP, AZURE, OTHER (기본값: ON_PREMISE)"},
                    {"클라우드 리소스 ID", "선택", "EC2 인스턴스 ID, ARN 등"},
                    {"리전", "선택", "ap-northeast-2, us-east-1 등"},
                    {"IP 주소", "선택", "IPv4 형식 (예: 192.168.1.10)"},
                    {"운영체제", "선택", "자유 텍스트 (예: Ubuntu 22.04 LTS)"},
                    {"사양", "선택", "자유 텍스트 (예: CPU: 8코어 / RAM: 32GB)"},
                    {"자산소유자(Owner)", "선택", "담당자 이름"},
                    {"관리부서", "선택", "부서명"},
                    {"운영담당자", "선택", "실무 담당자 이름"},
                    {"위치", "선택", "AWS, IDC 서울, 사무실 3층 등 자유 텍스트"},
                    {"자산설명", "선택", "자유 텍스트"},
                    {"상태", "선택", "OPERATIONAL(운영중), SUSPENDED(중지), DISPOSED(폐기) (기본값: OPERATIONAL)"},
                    {"중요도", "선택", "HIGH, MEDIUM, LOW (기본값: MEDIUM)"},
                    {"기밀성(C)", "선택", "HIGH, MEDIUM, LOW (기본값: MEDIUM)"},
                    {"무결성(I)", "선택", "HIGH, MEDIUM, LOW (기본값: MEDIUM)"},
                    {"가용성(A)", "선택", "HIGH, MEDIUM, LOW (기본값: MEDIUM)"},
                    {"개인정보 포함 여부", "선택", "O 또는 X"},
                    {"개인정보 유형", "선택", "고객정보, 임직원정보 등 자유 텍스트"},
                    {"개인정보 처리 여부", "선택", "O 또는 X"},
                    {"연계 시스템", "선택", "자유 텍스트 (예: HR 시스템, 결제 시스템)"},
                    {"접근권한 관리 대상", "선택", "O 또는 X"},
                    {"백업 대상", "선택", "O 또는 X"},
                    {"로그 관리 대상", "선택", "O 또는 X"},
                    {"월 비용(원)", "선택", "숫자만 입력 (예: 36000)"},
                    {"계약 만료일 / 최근 점검일 / 다음 점검일 / 최종 검토일", "선택", "YYYY-MM-DD 형식 (예: 2026-12-31)"},
                    {"비고", "선택", "자유 텍스트"},
            };

            for (int r = 0; r < hints.length; r++) {
                Row row = hintSheet.createRow(r);
                for (int c = 0; c < hints[r].length; c++) {
                    Cell cell = row.createCell(c);
                    cell.setCellValue(hints[r][c]);
                    if (r == 0) cell.setCellStyle(boldStyle);
                    hintSheet.setColumnWidth(c, c == 2 ? 22000 : 6000);
                }
            }

            wb.write(out);
            return out.toByteArray();
        }
    }

    private void addExampleRow(Sheet sheet, int rowNum, String[] values) {
        Row row = sheet.createRow(rowNum);
        for (int i = 0; i < values.length; i++) {
            row.createCell(i).setCellValue(values[i]);
        }
    }

    // ── 엑셀 업로드 파싱 ──────────────────────────────────────────────────────

    @Transactional
    public AssetBulkUploadResult upload(MultipartFile file) throws IOException {
        List<AssetBulkUploadResult.RowError> errors = new ArrayList<>();
        int total = 0, success = 0;

        try (Workbook wb = WorkbookFactory.create(file.getInputStream())) {
            Sheet sheet = wb.getSheetAt(0);
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null || isEmptyRow(row)) continue;
                total++;
                try {
                    assetRepository.save(parseRow(row));
                    success++;
                } catch (Exception e) {
                    errors.add(new AssetBulkUploadResult.RowError(i + 1, e.getMessage()));
                }
            }
        }

        if (success > 0) {
            auditLogService.log("ASSET_BULK_UPLOAD", "ASSET", 0L, success + "건 일괄 등록");
        }
        return new AssetBulkUploadResult(total, success, total - success, errors);
    }

    private Asset parseRow(Row row) {
        String name = getString(row, COL_NAME);
        if (name == null || name.isBlank()) throw new IllegalArgumentException("자산명은 필수입니다");

        String typeStr = getString(row, COL_TYPE);
        if (typeStr == null || typeStr.isBlank()) throw new IllegalArgumentException("자산분류는 필수입니다");
        String type = typeStr.trim().toUpperCase();
        boolean validType = false;
        for (String vt : VALID_TYPES) { if (vt.equals(type)) { validType = true; break; } }
        if (!validType) {
            throw new IllegalArgumentException("유효하지 않은 자산분류: " + typeStr
                    + " (허용값: " + String.join(", ", VALID_TYPES) + ")");
        }

        Asset.AssetCategory assetCategory = parseEnum(Asset.AssetCategory.class, getString(row, COL_ASSET_CATEGORY), null);
        Asset.Environment env = parseEnum(Asset.Environment.class, getString(row, COL_ENVIRONMENT), Asset.Environment.PRODUCTION);
        Asset.CloudProvider cp = parseEnum(Asset.CloudProvider.class, getString(row, COL_CLOUD_PROVIDER), Asset.CloudProvider.ON_PREMISE);
        Asset.Status status = parseEnum(Asset.Status.class, getString(row, COL_STATUS), Asset.Status.OPERATIONAL);
        Asset.Criticality crit = parseEnum(Asset.Criticality.class, getString(row, COL_CRITICALITY), Asset.Criticality.MEDIUM);
        Asset.Criticality conf = parseEnum(Asset.Criticality.class, getString(row, COL_CONFIDENTIALITY), Asset.Criticality.MEDIUM);
        Asset.Criticality intg = parseEnum(Asset.Criticality.class, getString(row, COL_INTEGRITY), Asset.Criticality.MEDIUM);
        Asset.Criticality avai = parseEnum(Asset.Criticality.class, getString(row, COL_AVAILABILITY), Asset.Criticality.MEDIUM);

        BigDecimal cost = null;
        String costStr = getString(row, COL_MONTHLY_COST);
        if (costStr != null && !costStr.isBlank()) {
            try {
                cost = new BigDecimal(costStr.trim());
            } catch (Exception e) {
                throw new IllegalArgumentException("월 비용 형식 오류 (숫자만 입력): " + costStr);
            }
        }

        return Asset.builder()
                .name(name.trim())
                .assetCategory(assetCategory)
                .type(type)
                .environment(env)
                .cloudProvider(cp)
                .cloudResourceId(getString(row, COL_CLOUD_RES_ID))
                .region(getString(row, COL_REGION))
                .ipAddress(getString(row, COL_IP))
                .osType(getString(row, COL_OS))
                .spec(getString(row, COL_SPEC))
                .owner(getString(row, COL_OWNER))
                .department(getString(row, COL_DEPARTMENT))
                .operator(getString(row, COL_OPERATOR))
                .location(getString(row, COL_LOCATION))
                .description(getString(row, COL_DESCRIPTION))
                .status(status)
                .criticality(crit)
                .confidentiality(conf)
                .integrity(intg)
                .availability(avai)
                .personalInfoIncluded(parseBoolean(getString(row, COL_PI_INCLUDED)))
                .personalInfoType(getString(row, COL_PI_TYPE))
                .personalInfoProcessing(parseBoolean(getString(row, COL_PI_PROCESSING)))
                .linkedSystems(getString(row, COL_LINKED_SYSTEMS))
                .accessControlTarget(parseBoolean(getString(row, COL_ACCESS_CTRL)))
                .backupTarget(parseBoolean(getString(row, COL_BACKUP)))
                .logManagementTarget(parseBoolean(getString(row, COL_LOG_MGMT)))
                .monthlyCost(cost)
                .contractExpiry(parseDate(getString(row, COL_CONTRACT_EXPIRY)))
                .lastInspectionDate(parseDate(getString(row, COL_LAST_INSPECTION)))
                .nextInspectionDate(parseDate(getString(row, COL_NEXT_INSPECTION)))
                .lastReviewDate(parseDate(getString(row, COL_LAST_REVIEW)))
                .remarks(getString(row, COL_REMARKS))
                .build();
    }

    private String getString(Row row, int col) {
        Cell cell = row.getCell(col, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
        if (cell == null) return null;
        return switch (cell.getCellType()) {
            case STRING -> {
                String v = cell.getStringCellValue().trim();
                yield v.isEmpty() ? null : v;
            }
            case NUMERIC -> DateUtil.isCellDateFormatted(cell)
                    ? cell.getLocalDateTimeCellValue().toLocalDate().toString()
                    : String.valueOf((long) cell.getNumericCellValue());
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            default -> null;
        };
    }

    private <T extends Enum<T>> T parseEnum(Class<T> cls, String val, T defaultVal) {
        if (val == null || val.isBlank()) return defaultVal;
        try {
            return Enum.valueOf(cls, val.trim().toUpperCase());
        } catch (Exception e) {
            throw new IllegalArgumentException("유효하지 않은 값 '" + val + "' (" + cls.getSimpleName() + ")");
        }
    }

    private boolean parseBoolean(String val) {
        if (val == null) return false;
        String v = val.trim().toUpperCase();
        return "O".equals(v) || "Y".equals(v) || "TRUE".equals(v) || "1".equals(v);
    }

    private LocalDate parseDate(String val) {
        if (val == null || val.isBlank()) return null;
        try {
            return LocalDate.parse(val.trim());
        } catch (Exception e) {
            throw new IllegalArgumentException("날짜 형식 오류 (YYYY-MM-DD): " + val);
        }
    }

    private boolean isEmptyRow(Row row) {
        for (int i = 0; i < HEADERS.length; i++) {
            Cell c = row.getCell(i);
            if (c != null && c.getCellType() != CellType.BLANK
                    && c.getCellType() != CellType.STRING
                    || (c != null && c.getCellType() == CellType.STRING
                        && !c.getStringCellValue().isBlank())) {
                return false;
            }
        }
        return true;
    }
}
