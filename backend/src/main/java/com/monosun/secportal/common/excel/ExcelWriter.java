package com.monosun.secportal.common.excel;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * 결과 리포트용 엑셀 작성 헬퍼.
 * 제목 / 메타 정보(라벨-값) / 표 헤더 / 데이터 행 형태가 반복되는 화면별 내려받기에서 공통으로 쓴다.
 */
public class ExcelWriter implements AutoCloseable {

    private final XSSFWorkbook wb = new XSSFWorkbook();

    private final CellStyle titleStyle;
    private final CellStyle metaLabelStyle;
    private final CellStyle metaValueStyle;
    private final CellStyle headerStyle;
    private final CellStyle cellStyle;
    private final CellStyle centerStyle;
    private final CellStyle wrapStyle;

    public ExcelWriter() {
        Font titleFont = wb.createFont();
        titleFont.setBold(true);
        titleFont.setFontHeightInPoints((short) 14);

        Font boldFont = wb.createFont();
        boldFont.setBold(true);

        Font whiteBoldFont = wb.createFont();
        whiteBoldFont.setBold(true);
        whiteBoldFont.setColor(IndexedColors.WHITE.getIndex());

        titleStyle = wb.createCellStyle();
        titleStyle.setFont(titleFont);
        titleStyle.setAlignment(HorizontalAlignment.CENTER);
        titleStyle.setVerticalAlignment(VerticalAlignment.CENTER);

        metaLabelStyle = wb.createCellStyle();
        metaLabelStyle.setFont(boldFont);
        metaLabelStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        metaLabelStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        border(metaLabelStyle);

        metaValueStyle = wb.createCellStyle();
        border(metaValueStyle);

        headerStyle = wb.createCellStyle();
        headerStyle.setFont(whiteBoldFont);
        headerStyle.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        headerStyle.setAlignment(HorizontalAlignment.CENTER);
        headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        border(headerStyle);

        cellStyle = wb.createCellStyle();
        cellStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        border(cellStyle);

        centerStyle = wb.createCellStyle();
        centerStyle.setAlignment(HorizontalAlignment.CENTER);
        centerStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        border(centerStyle);

        wrapStyle = wb.createCellStyle();
        wrapStyle.setVerticalAlignment(VerticalAlignment.TOP);
        wrapStyle.setWrapText(true);
        border(wrapStyle);
    }

    public Sheet sheet(String name) {
        return wb.createSheet(sanitizeSheetName(name));
    }

    /** 시트 상단 제목 행. 다음에 쓸 행 번호를 돌려준다. */
    public int title(Sheet sheet, int rowNum, String text, int lastCol) {
        Row row = sheet.createRow(rowNum);
        row.setHeightInPoints(30);
        Cell cell = row.createCell(0);
        cell.setCellValue(text);
        cell.setCellStyle(titleStyle);
        if (lastCol > 0) sheet.addMergedRegion(new CellRangeAddress(rowNum, rowNum, 0, lastCol));
        return rowNum + 1;
    }

    /** 라벨-값 2열 메타 정보 블록. */
    public int meta(Sheet sheet, int rowNum, String[][] pairs) {
        for (String[] pair : pairs) {
            Row row = sheet.createRow(rowNum++);
            row.setHeightInPoints(18);
            Cell label = row.createCell(0);
            label.setCellValue(pair[0]);
            label.setCellStyle(metaLabelStyle);
            Cell value = row.createCell(1);
            value.setCellValue(nvl(pair[1]));
            value.setCellStyle(metaValueStyle);
        }
        return rowNum;
    }

    /** 표 헤더 행. */
    public int header(Sheet sheet, int rowNum, String[] headers) {
        Row row = sheet.createRow(rowNum);
        row.setHeightInPoints(22);
        for (int i = 0; i < headers.length; i++) {
            Cell cell = row.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }
        return rowNum + 1;
    }

    /**
     * 데이터 행. 값이 Number 면 숫자 셀로, 그 외에는 문자열로 쓴다.
     * center 에 포함된 열 인덱스는 가운데 정렬한다.
     */
    public int row(Sheet sheet, int rowNum, Object[] values, int... center) {
        Row row = sheet.createRow(rowNum);
        row.setHeightInPoints(18);
        for (int i = 0; i < values.length; i++) {
            Cell cell = row.createCell(i);
            Object v = values[i];
            if (v instanceof Number n) {
                cell.setCellValue(n.doubleValue());
            } else {
                cell.setCellValue(nvl(v == null ? null : v.toString()));
            }
            cell.setCellStyle(contains(center, i) ? centerStyle : cellStyle);
        }
        return rowNum + 1;
    }

    /** 여러 줄 텍스트(총평·개선사항 등)를 병합 셀에 담는다. */
    public int textBlock(Sheet sheet, int rowNum, String label, String text, int lastCol, int heightPoints) {
        Row labelRow = sheet.createRow(rowNum);
        labelRow.setHeightInPoints(18);
        Cell labelCell = labelRow.createCell(0);
        labelCell.setCellValue(label);
        labelCell.setCellStyle(metaLabelStyle);
        if (lastCol > 0) sheet.addMergedRegion(new CellRangeAddress(rowNum, rowNum, 0, lastCol));
        rowNum++;

        Row textRow = sheet.createRow(rowNum);
        textRow.setHeightInPoints(heightPoints);
        Cell textCell = textRow.createCell(0);
        textCell.setCellValue(nvl(text));
        textCell.setCellStyle(wrapStyle);
        if (lastCol > 0) sheet.addMergedRegion(new CellRangeAddress(rowNum, rowNum, 0, lastCol));
        return rowNum + 1;
    }

    public void widths(Sheet sheet, int... widths) {
        for (int i = 0; i < widths.length; i++) {
            sheet.setColumnWidth(i, widths[i] * 256);
        }
    }

    public byte[] toBytes() {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            wb.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Excel 생성 실패", e);
        }
    }

    @Override
    public void close() {
        try { wb.close(); } catch (IOException ignored) { }
    }

    /** Excel 시트명 금지 문자 제거, 최대 31자. */
    public static String sanitizeSheetName(String name) {
        if (name == null || name.isBlank()) name = "Sheet";
        String sanitized = name.replaceAll("[\\\\/*?:\\[\\]]", "_");
        return sanitized.length() > 31 ? sanitized.substring(0, 31) : sanitized;
    }

    private static boolean contains(int[] arr, int v) {
        for (int i : arr) if (i == v) return true;
        return false;
    }

    private static void border(CellStyle style) {
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
    }

    private static String nvl(String s) {
        return s != null ? s : "";
    }
}
