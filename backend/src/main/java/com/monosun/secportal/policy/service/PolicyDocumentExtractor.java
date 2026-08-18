package com.monosun.secportal.policy.service;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 지침 문서(PDF/DOCX/TXT/MD)에서 본문 텍스트를 뽑아 <b>정책 본문 형식(마크다운)</b>으로 다듬는다.
 *
 * <p>PDF 는 페이지 머리말·꼬리말·쪽번호가 본문에 섞여 나오므로, 여러 페이지에 반복되는 줄과
 * 쪽번호로 보이는 줄을 걷어낸 뒤 조 머리글을 {@code ### 제N조(제목)} 으로 올린다.
 * 이렇게 해두면 기존 {@link PolicyStructureParser} 가 그대로 조를 인식한다.
 *
 * <p>추출 품질은 원본 문서에 좌우된다(스캔 이미지 PDF 는 텍스트가 없어 추출되지 않는다).
 * 실패 사유는 예외 대신 {@link Result#warnings()} 로 돌려 화면에서 사용자가 판단하게 한다.
 */
@Component
public class PolicyDocumentExtractor {

    /** 쪽번호로 보이는 줄 — "12", "- 12 -", "12 / 40", "Page 12", "- 12 -쪽" */
    private static final Pattern PAGE_NUMBER = Pattern.compile(
            "^\\s*(?:page\\s*)?[-–—\\[(]?\\s*\\d{1,4}\\s*(?:/\\s*\\d{1,4})?\\s*[-–—\\])]?\\s*(?:쪽|페이지)?\\s*$",
            Pattern.CASE_INSENSITIVE);

    /** 조 머리글 — "제1조(목적)", "제3조의2 (특례)", 앞뒤 장식 기호 허용 */
    private static final Pattern ARTICLE_LINE = Pattern.compile(
            "^\\s*[◇◆○●■□▪•\\-]?\\s*제\\s*(?<no>\\d{1,3})\\s*조(?:\\s*의\\s*(?<sub>\\d{1,2}))?\\s*"
            + "(?:[(（]\\s*(?<title>[^)）]{0,80}?)\\s*[)）])?\\s*(?<rest>.*?)\\s*$");

    /** 장 머리글 — "제1장 총칙", "부칙" */
    public static final Pattern CHAPTER_LINE = Pattern.compile(
            "^\\s*[◇◆○●■□]?\\s*(?:제\\s*(?<no>\\d{1,3})\\s*장|(?<supplement>부\\s*칙))\\s*(?<title>[^\\n]{0,80}?)\\s*$");

    /** 목차 머리글 — "목차", "목 차", "차례", "CONTENTS" 만 있는 줄 */
    private static final Pattern TOC_HEADING = Pattern.compile(
            "^\\s*[<\\[(]?\\s*(?:목\\s*차|차\\s*례|table\\s+of\\s+contents|contents)\\s*[>\\])]?\\s*$",
            Pattern.CASE_INSENSITIVE);

    /** 점선 안내가 붙은 목차 항목 — "제1조(목적) ………… 3" */
    private static final Pattern TOC_DOT_LEADER = Pattern.compile(
            "^\\s*\\S.*?[.·‥…]{3,}\\s*\\d{1,4}\\s*$");

    /** 끝에 쪽번호가 붙은 줄 — "제1장 총칙   3" (머리글일 때만 목차로 본다) */
    private static final Pattern TRAILING_PAGE = Pattern.compile(
            "^\\s*(?<head>\\S.*?)\\s+(?<page>\\d{1,4})\\s*$");

    /** 목차 블록을 훑을 때 한 번에 지울 수 있는 최대 줄 수 — 폭주 방지 */
    private static final int TOC_SCAN_LIMIT = 300;

    /** 추출 결과 — 본문과 함께, 사용자에게 보여줄 경고를 싣는다. */
    public record Result(String text, List<String> warnings) {}

    public boolean isSupported(String filename) {
        String f = filename == null ? "" : filename.toLowerCase();
        return f.endsWith(".pdf") || f.endsWith(".docx") || f.endsWith(".txt") || f.endsWith(".md");
    }

    /**
     * 파일에서 본문을 뽑아 마크다운으로 다듬는다.
     *
     * @throws IllegalArgumentException 지원하지 않는 확장자이거나 본문을 한 글자도 뽑지 못한 경우
     */
    public Result extract(MultipartFile file) throws IOException {
        String name = file.getOriginalFilename() == null ? "" : file.getOriginalFilename();
        String lower = name.toLowerCase();
        List<String> warnings = new ArrayList<>();

        String raw;
        if (lower.endsWith(".pdf")) {
            raw = fromPdf(file, warnings);
        } else if (lower.endsWith(".docx")) {
            raw = fromDocx(file);
        } else if (lower.endsWith(".txt") || lower.endsWith(".md")) {
            raw = new String(file.getBytes(), StandardCharsets.UTF_8);
        } else if (lower.endsWith(".doc") || lower.endsWith(".hwp") || lower.endsWith(".hwpx")) {
            throw new IllegalArgumentException(
                    "지원하지 않는 형식입니다: " + name + " — PDF 또는 DOCX 로 변환해 올려주세요. "
                    + "(구형 .doc, 한글 .hwp/.hwpx 는 본문을 읽을 수 없습니다)");
        } else {
            throw new IllegalArgumentException(
                    "지원하지 않는 형식입니다: " + name + " — PDF, DOCX, TXT, MD 만 등록할 수 있습니다.");
        }

        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException(
                    "문서에서 글자를 찾지 못했습니다. 스캔 이미지로만 된 PDF 는 본문을 읽을 수 없습니다 "
                    + "(텍스트가 살아있는 PDF 나 DOCX 로 올려주세요).");
        }

        String normalized = normalize(raw, warnings);
        if (normalized.isBlank()) {
            throw new IllegalArgumentException("문서에서 본문으로 쓸 내용을 찾지 못했습니다.");
        }
        return new Result(normalized, warnings);
    }

    // ── 포맷별 추출 ────────────────────────────────────────────────────────

    /** 쪽 사이에 구분자를 넣어 뽑는다. 머리말·꼬리말 제거에 쪽 경계가 필요하다. */
    private String fromPdf(MultipartFile file, List<String> warnings) throws IOException {
        try (InputStream in = file.getInputStream();
             PDDocument doc = Loader.loadPDF(in.readAllBytes())) {
            if (doc.isEncrypted()) {
                warnings.add("암호가 걸린 PDF 입니다. 추출된 본문이 일부일 수 있습니다.");
            }
            int pages = doc.getNumberOfPages();
            StringBuilder sb = new StringBuilder();
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            for (int p = 1; p <= pages; p++) {
                stripper.setStartPage(p);
                stripper.setEndPage(p);
                sb.append(stripper.getText(doc));
                if (p < pages) sb.append("\f");
            }
            return sb.toString();
        }
    }

    private String fromDocx(MultipartFile file) throws IOException {
        try (InputStream in = file.getInputStream(); XWPFDocument doc = new XWPFDocument(in)) {
            StringBuilder sb = new StringBuilder();
            for (XWPFParagraph p : doc.getParagraphs()) {
                sb.append(p.getText() == null ? "" : p.getText()).append('\n');
            }
            return sb.toString();
        }
    }

    // ── 정리 ──────────────────────────────────────────────────────────────

    /** 경고를 모으지 않는 호출용(테스트·내부). */
    String normalize(String raw) {
        return normalize(raw, new ArrayList<>());
    }

    /**
     * 쪽 머리말·꼬리말·쪽번호와 <b>목차</b>를 걷어내고 조 머리글을 마크다운 머리글로 올린다.
     * 쪽 경계(\f)가 없는 포맷은 반복 줄 제거를 건너뛴다.
     */
    String normalize(String raw, List<String> warnings) {
        String unified = raw.replace("\r\n", "\n").replace("\r", "\n");
        String[] pages = unified.split("\f", -1);
        Set<String> repeated = pages.length >= 3 ? repeatedEdgeLines(pages) : Set.of();

        List<String> cleaned = new ArrayList<>();
        for (String page : pages) {
            for (String line : page.split("\n", -1)) {
                String t = line.strip();
                if (t.isEmpty()) { cleaned.add(""); continue; }
                if (PAGE_NUMBER.matcher(t).matches()) continue;
                if (repeated.contains(t)) continue;
                cleaned.add(t);
            }
        }

        int dropped = cleaned.size();
        List<String> body = removeTableOfContents(cleaned);
        dropped -= body.size();
        if (dropped > 0) {
            warnings.add("목차로 보이는 " + dropped + "줄을 제외했습니다.");
        }

        List<String> out = new ArrayList<>();
        for (String t : body) {
            if (t.isEmpty()) { addLine(out, ""); continue; }
            addLine(out, promoteHeading(t));
        }
        return String.join("\n", trimEdges(out)).replaceAll("\n{3,}", "\n\n").strip();
    }

    /**
     * 목차를 걷어낸다. 목차를 남기면 "제1장 …", "제1조(…) …" 항목이 본문의 장·조로
     * 잘못 잡혀 빈 껍데기 장이 등록되므로, 등록 전에 반드시 떼어낸다.
     *
     * <p>두 갈래로 지운다.
     * <ol>
     *   <li><b>목차 머리글("목 차")부터 본문이 시작될 때까지</b> — 목차에 실린 장·조 머리글이
     *       본문에서 다시 나오는 지점을 본문 시작으로 본다(목차는 본문 차례를 그대로 옮긴 것이므로).</li>
     *   <li><b>쪽번호가 달린 차례 줄</b> — "제1조(목적) …… 3" 처럼 점선이나 쪽번호가 붙은 줄은
     *       목차 머리글이 없는 문서에서도 목차로 본다.</li>
     * </ol>
     */
    private List<String> removeTableOfContents(List<String> lines) {
        List<String> out = new ArrayList<>();
        Set<String> tocHeadings = new LinkedHashSet<>();
        boolean inToc = false;
        int scanned = 0;

        for (String line : lines) {
            if (!inToc && TOC_HEADING.matcher(line).matches()) {
                inToc = true;
                scanned = 0;
                continue;
            }

            if (inToc) {
                scanned++;
                String head = tocHeadingKey(line);
                // 목차에 있던 머리글이 다시 나오면 거기서 본문이 시작된 것으로 본다.
                if (head != null && tocHeadings.contains(head)) {
                    inToc = false;
                } else if (scanned > TOC_SCAN_LIMIT || isBodyProse(line)) {
                    // 안전장치 — 차례가 아닌 긴 문장이 나오면 목차가 끝난 것으로 본다.
                    inToc = false;
                } else {
                    if (head != null) tocHeadings.add(head);
                    continue;   // 목차 줄은 버린다
                }
            }

            if (isTocEntry(line)) continue;   // 쪽번호가 달린 차례 줄
            out.add(line);
        }
        return out;
    }

    /** 장·조 머리글이면 비교용 키(공백 제거)를, 아니면 null 을 돌려준다. */
    private String tocHeadingKey(String line) {
        String bare = stripLeaders(line);
        if (bare.isEmpty()) return null;
        if (!CHAPTER_LINE.matcher(bare).matches() && !isArticleHeading(bare)) return null;
        return bare.replaceAll("\\s+", "");
    }

    /** 목차가 끝났다고 볼 만한 본문 문장 — 쪽번호·점선이 없는 긴 줄 */
    private boolean isBodyProse(String line) {
        return line.length() > 60 && !isTocEntry(line) && tocHeadingKey(line) == null;
    }

    /** 점선 안내와 끝의 쪽번호를 떼어낸다. */
    private String stripLeaders(String line) {
        return line.replaceAll("[.·‥…\\u00B7]{2,}\\s*\\d{0,4}\\s*$", "")
                   .replaceAll("\\s+\\d{1,4}\\s*$", "")
                   .strip();
    }

    /**
     * 쪽번호가 달린 차례 줄인지 본다.
     * 끝에 숫자만 붙은 줄은 <b>장·조 머리글이면서 짧을 때만</b> 목차로 본다
     * ("제5조(비밀번호) 비밀번호는 … 2026" 같은 본문 문장을 지우지 않기 위한 조건).
     */
    private boolean isTocEntry(String line) {
        if (line.isEmpty()) return false;
        if (TOC_DOT_LEADER.matcher(line).matches()) return true;

        Matcher m = TRAILING_PAGE.matcher(line);
        if (!m.matches()) return false;
        String head = m.group("head").strip();
        if (head.length() > 60) return false;
        return CHAPTER_LINE.matcher(head).matches() || isArticleHeading(head);
    }

    /**
     * 쪽마다 위·아래 2줄에서 되풀이되는 줄(머리말·꼬리말)을 찾는다.
     * 절반 이상의 쪽에 나타나야 머리말로 본다 — 본문 문장을 지우지 않기 위한 기준이다.
     */
    private Set<String> repeatedEdgeLines(String[] pages) {
        Map<String, Integer> counter = new HashMap<>();
        for (String page : pages) {
            List<String> lines = new ArrayList<>();
            for (String l : page.split("\n", -1)) {
                String t = l.strip();
                if (!t.isEmpty()) lines.add(t);
            }
            Set<String> edges = new LinkedHashSet<>();
            for (int i = 0; i < Math.min(2, lines.size()); i++) edges.add(lines.get(i));
            for (int i = Math.max(0, lines.size() - 2); i < lines.size(); i++) edges.add(lines.get(i));
            for (String e : edges) counter.merge(e, 1, Integer::sum);
        }
        int threshold = Math.max(2, pages.length / 2);
        Set<String> repeated = new LinkedHashSet<>();
        counter.forEach((line, n) -> {
            // 장·조 머리글은 되풀이돼도 본문이므로 남긴다.
            if (n >= threshold
                    && !CHAPTER_LINE.matcher(line).matches()
                    && !isArticleHeading(line)) {
                repeated.add(line);
            }
        });
        return repeated;
    }

    /** 조 머리글 줄이면 "### 제N조(제목)" 형태로 올린다. 이미 마크다운이면 그대로 둔다. */
    private String promoteHeading(String line) {
        if (line.startsWith("#")) return line;

        Matcher c = CHAPTER_LINE.matcher(line);
        if (c.matches()) {
            return "## " + line;
        }
        Matcher m = ARTICLE_LINE.matcher(line);
        if (m.matches()) {
            String label = "제" + m.group("no") + "조"
                    + (m.group("sub") != null ? "의" + m.group("sub") : "");
            String title = m.group("title");
            String rest = m.group("rest") == null ? "" : m.group("rest").strip();
            StringBuilder sb = new StringBuilder("### ").append(label);
            if (title != null && !title.isBlank()) sb.append('(').append(title.strip()).append(')');
            // 머리글 뒤에 본문이 이어 붙은 줄은 조 제목만 남기고 본문은 다음 줄로 내린다.
            if (!rest.isEmpty()) sb.append('\n').append(rest);
            return sb.toString();
        }
        return line;
    }

    private boolean isArticleHeading(String line) {
        Matcher m = ARTICLE_LINE.matcher(line);
        // 제목 괄호가 있어야 머리글로 본다("제3조의 규정에 따라 …" 같은 본문 문장 보호).
        return m.matches() && m.group("title") != null;
    }

    /** 빈 줄이 연달아 쌓이지 않게 넣는다. */
    private void addLine(List<String> out, String line) {
        if (line.isEmpty() && (out.isEmpty() || out.get(out.size() - 1).isEmpty())) return;
        out.add(line);
    }

    private List<String> trimEdges(List<String> lines) {
        int from = 0, to = lines.size();
        while (from < to && lines.get(from).isBlank()) from++;
        while (to > from && lines.get(to - 1).isBlank()) to--;
        return lines.subList(from, to);
    }
}
