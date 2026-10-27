package com.monosun.secportal.common.excel;

import org.apache.poi.ss.usermodel.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 스프레드시트 수식 삽입(CSV/Formula Injection) 방어.
 *
 * <p>사용자가 입력한 값이 {@code = + - @} 또는 탭·CR 로 시작하면 엑셀이 셀을 편집(활성화)하는 순간 수식으로 해석한다.
 * xlsx 는 값을 바꾸지 않고 셀에 "텍스트로 표시(quotePrefix)" 스타일을 붙이고({@link #protect(Workbook)}),
 * 스타일이 없는 CSV 는 값 앞에 작은따옴표를 붙인다({@link #csv(String)}).</p>
 */
public final class SpreadsheetSafe {

    private SpreadsheetSafe() {}

    public static boolean isDangerous(String s) {
        if (s == null || s.isEmpty()) return false;
        char c = s.charAt(0);
        return c == '=' || c == '+' || c == '-' || c == '@' || c == '\t' || c == '\r';
    }

    /** CSV 셀 값 — 수식으로 해석될 수 있으면 작은따옴표를 앞에 붙인다. */
    public static String csv(String s) {
        return isDangerous(s) ? "'" + s : s;
    }

    /** 내보낼 때 붙인 작은따옴표를 가져오기(import) 시 되돌린다 — {@code '=1+1} → {@code =1+1}. */
    public static String unescape(String s) {
        if (s != null && s.length() > 1 && s.charAt(0) == '\'' && isDangerous(s.substring(1))) return s.substring(1);
        return s;
    }

    /**
     * 워크북의 모든 문자열 셀 중 수식으로 해석될 수 있는 값에 quotePrefix 스타일을 적용한다.
     * 값 자체는 그대로 두므로 내보낸 파일을 다시 가져오기(import)해도 내용이 바뀌지 않는다.
     * {@code wb.write(...)} 직전에 호출한다.
     */
    public static void protect(Workbook wb) {
        Map<Short, CellStyle> quoted = new HashMap<>();
        for (Sheet sheet : wb) {
            for (Row row : sheet) {
                for (Cell cell : row) {
                    if (cell.getCellType() != CellType.STRING) continue;
                    if (!isDangerous(cell.getStringCellValue())) continue;
                    CellStyle base = cell.getCellStyle();
                    if (base.getQuotePrefixed()) continue;
                    CellStyle style = quoted.computeIfAbsent(base.getIndex(), k -> {
                        CellStyle s = wb.createCellStyle();
                        s.cloneStyleFrom(base);
                        s.setQuotePrefixed(true);
                        return s;
                    });
                    cell.setCellStyle(style);
                }
            }
        }
    }
}
