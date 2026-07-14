package com.monosun.secportal.privacy.service;

import com.monosun.secportal.common.exception.BusinessException;
import com.monosun.secportal.privacy.dto.ContractorDto;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 개인정보처리방침 페이지에서 "개인정보 처리업무의 위탁" 표를 찾아
 * 수탁사 / 위탁업무 / 재수탁사를 추출한다.
 *
 * 방침 페이지마다 표 구조가 제각각이므로 헤더 텍스트의 키워드로 컬럼을 식별하고,
 * rowspan/colspan 을 펼친 격자로 변환한 뒤 행을 읽는다.
 */
@Slf4j
@Service
public class PrivacyPolicyParser {

    private static final int TIMEOUT_MS = 15_000;
    private static final int MAX_BODY_BYTES = 8 * 1024 * 1024;
    private static final String USER_AGENT =
            "Mozilla/5.0 (compatible; KSecPortal/1.0; +privacy-policy-import)";

    /** 재수탁 컬럼 — 반드시 수탁/위탁 판정보다 먼저 검사한다. */
    private static final String[] SUB_KEYS = {"재위탁", "재수탁", "재처리", "2차수탁"};
    /** 위탁업무 컬럼 */
    private static final String[] TASK_KEYS = {"업무", "내용", "목적", "위탁사항"};
    /** 수탁사 컬럼 */
    private static final String[] NAME_KEYS = {"수탁", "위탁받는", "위탁받은", "받는자", "업체", "회사", "기관", "사업자", "성명"};

    /** 값 셀이 "없음"류일 때 빈 값으로 취급 */
    private static final String[] EMPTY_VALUES = {"없음", "해당없음", "해당사항없음", "-", "–", "—", "N/A", "없습니다"};

    public ContractorDto.PolicyParseResponse parse(String url) {
        String normalized = normalizeUrl(url);
        Document doc = fetch(normalized);

        Map<String, ContractorDto.ParsedContractor> merged = new LinkedHashMap<>();
        int matchedTables = 0;

        for (Element table : doc.select("table")) {
            List<ContractorDto.ParsedContractor> rows = parseTable(table);
            if (rows.isEmpty()) continue;
            matchedTables++;
            for (ContractorDto.ParsedContractor row : rows) {
                // 같은 수탁사가 여러 행/표에 나오면 위탁업무를 합친다
                merged.merge(dedupeKey(row), row, this::mergeRow);
            }
        }

        if (merged.isEmpty()) {
            throw new BusinessException(
                    "해당 페이지에서 수탁사 표를 찾지 못했습니다. " +
                    "개인정보처리방침 본문이 표(table)로 작성된 페이지 URL인지 확인하세요. " +
                    "(자바스크립트로 그려지는 페이지는 인식하지 못할 수 있습니다)");
        }

        return ContractorDto.PolicyParseResponse.builder()
                .sourceUrl(normalized)
                .tableCount(matchedTables)
                .items(new ArrayList<>(merged.values()))
                .build();
    }

    // ── 표 1개 파싱 ────────────────────────────────────────────────────

    private List<ContractorDto.ParsedContractor> parseTable(Element table) {
        List<List<String>> grid = toGrid(table);
        if (grid.size() < 2) return List.of();

        // 표 앞부분(최대 5행)에서 헤더 행을 찾는다 — 제목 행이 앞에 붙는 표가 있다
        for (int r = 0; r < Math.min(grid.size(), 5); r++) {
            List<String> header = grid.get(r);
            if (!isConsignmentHeader(header)) continue;

            int nameCol = -1, taskCol = -1, subCol = -1;
            for (int i = 0; i < header.size(); i++) {
                String h = headerKey(header.get(i));
                if (h.isEmpty()) continue;
                if (subCol < 0 && containsAny(h, SUB_KEYS)) { subCol = i; continue; }
                if (taskCol < 0 && containsAny(h, TASK_KEYS)) { taskCol = i; continue; }
                if (nameCol < 0 && containsAny(h, NAME_KEYS)) { nameCol = i; }
            }
            if (nameCol < 0) continue;

            return readRows(grid, r, squash(header.get(nameCol)), nameCol, taskCol, subCol);
        }
        return List.of();
    }

    /**
     * 위탁 표인지 판정한다. 헤더에 "수탁" 또는 "위탁"이 반드시 있어야 하므로
     * 제3자 제공 표("제공받는 자 / 제공 목적 / 제공 항목")는 걸러진다.
     */
    private boolean isConsignmentHeader(List<String> header) {
        for (String c : header) {
            String h = squash(c);
            if (h.contains("수탁") || h.contains("위탁")) return true;
        }
        return false;
    }

    private List<ContractorDto.ParsedContractor> readRows(List<List<String>> grid, int headerRow,
                                                          String headerName, int nameCol, int taskCol, int subCol) {
        List<ContractorDto.ParsedContractor> rows = new ArrayList<>();
        for (int r = headerRow + 1; r < grid.size(); r++) {
            List<String> row = grid.get(r);
            String name = clean(cell(row, nameCol));
            if (name.isEmpty()) continue;
            if (squash(name).equals(headerName)) continue;   // 중간에 반복되는 헤더 행

            rows.add(ContractorDto.ParsedContractor.builder()
                    .name(truncate(name, 200))
                    .serviceType(truncate(clean(cell(row, taskCol)), 500))
                    .subContractor(truncate(clean(cell(row, subCol)), 500))
                    .existing(false)
                    .build());
        }
        return rows;
    }

    /**
     * rowspan / colspan 을 펼쳐 표를 직사각형 격자로 변환한다.
     * (수탁사 셀이 여러 행에 걸쳐 병합된 표가 흔하다)
     */
    private List<List<String>> toGrid(Element table) {
        List<List<String>> grid = new ArrayList<>();
        // col -> [남은 행 수, 값]
        Map<Integer, Integer> spanRows = new HashMap<>();
        Map<Integer, String> spanValue = new HashMap<>();

        for (Element tr : table.select("tr")) {
            Elements cells = tr.select("th, td");
            if (cells.isEmpty()) continue;

            List<String> row = new ArrayList<>();
            int col = 0, idx = 0;
            while (idx < cells.size() || hasSpanAtOrAfter(spanRows, col)) {
                if (spanRows.containsKey(col)) {
                    row.add(spanValue.get(col));
                    int left = spanRows.get(col) - 1;
                    if (left <= 0) { spanRows.remove(col); spanValue.remove(col); }
                    else spanRows.put(col, left);
                    col++;
                    continue;
                }
                if (idx >= cells.size()) { row.add(""); col++; continue; }

                Element cell = cells.get(idx++);
                String value = cellText(cell);
                int colspan = attrInt(cell, "colspan");
                int rowspan = attrInt(cell, "rowspan");
                for (int k = 0; k < colspan; k++) {
                    row.add(value);
                    if (rowspan > 1) {
                        spanRows.put(col, rowspan - 1);
                        spanValue.put(col, value);
                    }
                    col++;
                }
            }
            grid.add(row);
        }
        return grid;
    }

    private boolean hasSpanAtOrAfter(Map<Integer, Integer> spanRows, int col) {
        for (Integer c : spanRows.keySet()) if (c >= col) return true;
        return false;
    }

    private int attrInt(Element cell, String attr) {
        try {
            int v = Integer.parseInt(cell.attr(attr).trim());
            return Math.max(1, Math.min(v, 100));   // 비정상 값 방어
        } catch (NumberFormatException e) {
            return 1;
        }
    }

    /** <br>, <li> 등 줄바꿈 요소는 공백으로 치환해서 텍스트를 얻는다. */
    private String cellText(Element cell) {
        Element copy = cell.clone();
        copy.select("br").after(" ");
        copy.select("li, p, div, tr").after(" ");
        return copy.text().replaceAll("\\s+", " ").trim();
    }

    // ── HTTP ──────────────────────────────────────────────────────────

    private Document fetch(String url) {
        try {
            return Jsoup.connect(url)
                    .userAgent(USER_AGENT)
                    .timeout(TIMEOUT_MS)
                    .maxBodySize(MAX_BODY_BYTES)
                    .followRedirects(true)
                    .ignoreHttpErrors(false)
                    .get();
        } catch (org.jsoup.HttpStatusException e) {
            throw new BusinessException("페이지를 불러오지 못했습니다. (HTTP " + e.getStatusCode() + ")");
        } catch (IOException e) {
            log.warn("개인정보처리방침 페이지 조회 실패: {}", url, e);
            throw new BusinessException("페이지를 불러오지 못했습니다: " + e.getMessage());
        }
    }

    /** http/https 만 허용하고 내부망 주소로의 요청(SSRF)을 차단한다. */
    private String normalizeUrl(String raw) {
        if (raw == null || raw.isBlank()) throw new BusinessException("URL을 입력하세요.");
        String url = raw.trim();
        if (!url.matches("(?i)^https?://.*")) url = "https://" + url;

        URI uri;
        try {
            uri = URI.create(url);
        } catch (IllegalArgumentException e) {
            throw new BusinessException("올바른 URL 형식이 아닙니다.");
        }
        String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(Locale.ROOT);
        if (!scheme.equals("http") && !scheme.equals("https")) {
            throw new BusinessException("http 또는 https URL만 사용할 수 있습니다.");
        }
        String host = uri.getHost();
        if (host == null || host.isBlank()) throw new BusinessException("올바른 URL 형식이 아닙니다.");

        try {
            for (InetAddress addr : InetAddress.getAllByName(host)) {
                if (addr.isLoopbackAddress() || addr.isSiteLocalAddress() || addr.isLinkLocalAddress()
                        || addr.isAnyLocalAddress() || addr.isMulticastAddress()) {
                    throw new BusinessException("내부망 주소는 조회할 수 없습니다.");
                }
            }
        } catch (UnknownHostException e) {
            throw new BusinessException("주소를 찾을 수 없습니다: " + host);
        }
        return url;
    }

    // ── Helpers ───────────────────────────────────────────────────────

    private ContractorDto.ParsedContractor mergeRow(ContractorDto.ParsedContractor a,
                                                    ContractorDto.ParsedContractor b) {
        a.setServiceType(truncate(joinDistinct(a.getServiceType(), b.getServiceType()), 500));
        a.setSubContractor(truncate(joinDistinct(a.getSubContractor(), b.getSubContractor()), 500));
        return a;
    }

    private String joinDistinct(String a, String b) {
        if (a == null || a.isBlank()) return b == null ? "" : b;
        if (b == null || b.isBlank() || a.contains(b)) return a;
        return a + ", " + b;
    }

    private String dedupeKey(ContractorDto.ParsedContractor c) {
        return squash(c.getName()).toLowerCase(Locale.ROOT);
    }

    private String cell(List<String> row, int col) {
        if (col < 0 || col >= row.size()) return "";
        String v = row.get(col);
        return v == null ? "" : v;
    }

    /** 셀 값 정리 — 각주 기호 제거, "없음"류는 빈 값으로. */
    private String clean(String raw) {
        if (raw == null) return "";
        String v = raw.replaceAll("[\\u200b\\u00a0]", " ").replaceAll("\\s+", " ").trim();
        v = v.replaceAll("^[※*·•\\-–—]+\\s*", "").trim();
        for (String empty : EMPTY_VALUES) {
            if (v.equalsIgnoreCase(empty)) return "";
        }
        return v;
    }

    private String squash(String s) {
        return s == null ? "" : s.replaceAll("\\s+", "");
    }

    /**
     * 헤더 셀에서 컬럼 판정용 키를 만든다.
     * 괄호 주석과 각주는 제거한다 — 예: "수탁업체 (* 표시는 재위탁자)" 를 그대로 두면
     * '재위탁' 때문에 재수탁 컬럼으로 잘못 잡힌다.
     */
    private String headerKey(String s) {
        if (s == null) return "";
        String v = s.replaceAll("[\\(（\\[].*?[\\)）\\]]", " ")   // 괄호 주석
                    .replaceAll("[※*].*$", " ");                // 각주 이후
        return squash(v);
    }

    private String truncate(String s, int max) {
        if (s == null) return null;
        return s.length() <= max ? s : s.substring(0, max);
    }

    private boolean containsAny(String haystack, String[]... keySets) {
        for (String[] keys : keySets) {
            for (String k : keys) if (haystack.contains(k)) return true;
        }
        return false;
    }
}
