package com.monosun.secportal.policy.service;

import com.monosun.secportal.policy.entity.Policy;
import com.monosun.secportal.policy.entity.PolicyArticle;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 보안정책 문서 구조 파서.
 *
 * <p>제목에서 <b>지침 &gt; 장</b>을, 본문에서 <b>조</b>를 뽑아낸다.
 * <pre>
 *   제목: "개인정보보호 지침 - 제1장 총칙"  →  지침="개인정보보호 지침", 장=제1장 "총칙"
 *   본문: "### 제1조(목적)\n- 본 지침은 …"   →  조=제1조 "목적" + 본문
 * </pre>
 * 파싱에 실패해도 예외를 던지지 않고 최대한 원문을 보존한다(전체 제목을 지침명으로,
 * 전체 본문을 단일 "전문" 조로).
 */
@Component
public class PolicyStructureParser {

    /** "지침명 - 제N장 장제목" / "지침명 - 부칙" */
    private static final Pattern TITLE_PATTERN = Pattern.compile(
            "^\\s*(?<guideline>.+?)\\s*[-–—]\\s*(?:제\\s*(?<no>\\d+)\\s*장|(?<supplement>부\\s*칙))\\s*(?<ctitle>.*)$");

    /** 제목 자체가 "제N장 …" 으로 시작하는 경우(지침명 없음) */
    private static final Pattern CHAPTER_ONLY_PATTERN = Pattern.compile(
            "^\\s*(?:제\\s*(?<no>\\d+)\\s*장|(?<supplement>부\\s*칙))\\s*(?<ctitle>.*)$");

    /**
     * 마크다운 머리글 형태의 조 — "### 제1조(목적)", "## 제3조의2 (특례)".
     * 제목 뒤의 "&lt;개정 2024.5.28&gt;" 같은 꼬리말은 note 로 따로 보존한다.
     */
    private static final Pattern ARTICLE_HEADING = Pattern.compile(
            "^\\s{0,3}#{1,6}\\s*제\\s*(?<no>\\d+)\\s*조(?:\\s*의\\s*(?<sub>\\d+))?\\s*"
            + "(?:[(（]\\s*(?<title>[^)）]*?)\\s*[)）])?\\s*(?<note>.*?)\\s*$");

    /**
     * 머리글 표시가 없는 문서용 대체 규칙 — 줄 전체가 "제1조(목적)" 인 경우만.
     * 본문 문장("제3조의 규정에 따라 …")을 조로 오인하지 않도록 꼬리말은 &lt;…&gt; 형태만 허용한다.
     */
    private static final Pattern ARTICLE_PLAIN = Pattern.compile(
            "^\\s*제\\s*(?<no>\\d+)\\s*조(?:\\s*의\\s*(?<sub>\\d+))?\\s*"
            + "(?:[(（]\\s*(?<title>[^)）]*?)\\s*[)）])?\\s*(?<note><[^>\\n]*>)?\\s*$");

    /** 제목에서 파생한 지침/장 정보를 정책 엔티티에 반영한다. */
    public void applyTitleStructure(Policy policy) {
        Chapter c = parseTitle(policy.getTitle());
        policy.setGuidelineName(c.guideline());
        policy.setChapterNo(c.chapterNo());
        policy.setChapterLabel(c.chapterLabel());
        policy.setChapterTitle(c.chapterTitle());
    }

    /** 제목 → 지침명 + 장 */
    public Chapter parseTitle(String rawTitle) {
        String title = rawTitle == null ? "" : rawTitle.trim();
        if (title.isEmpty()) return new Chapter("", null, null, null);

        Matcher m = TITLE_PATTERN.matcher(title);
        if (m.matches()) {
            return toChapter(m.group("guideline").trim(), m);
        }
        m = CHAPTER_ONLY_PATTERN.matcher(title);
        if (m.matches()) {
            return toChapter("", m);
        }
        // 장 구분이 없는 단일 문서 — 제목 전체를 지침명으로 본다.
        return new Chapter(title, null, null, null);
    }

    private Chapter toChapter(String guideline, Matcher m) {
        String ctitle = blankToNull(m.group("ctitle"));
        if (m.group("supplement") != null) {
            return new Chapter(guideline, null, "부칙", ctitle);
        }
        int no = Integer.parseInt(m.group("no"));
        return new Chapter(guideline, no, "제" + no + "장", ctitle);
    }

    /**
     * 본문 → 조 목록. 조 머리글이 하나도 없으면 본문 전체를 "전문" 한 건으로 돌려준다.
     * 반환된 엔티티에는 policy 가 아직 설정되어 있지 않다.
     */
    public List<PolicyArticle> parseArticles(String content) {
        List<PolicyArticle> result = new ArrayList<>();
        if (content == null || content.isBlank()) return result;

        String[] lines = content.replace("\r\n", "\n").replace("\r", "\n").split("\n", -1);

        // 1차: 마크다운 머리글 규칙. 한 건도 못 찾으면 2차 규칙으로 재시도한다.
        List<Integer> heads = new ArrayList<>();
        List<Matcher> matchers = new ArrayList<>();
        collect(lines, ARTICLE_HEADING, heads, matchers);
        if (heads.isEmpty()) collect(lines, ARTICLE_PLAIN, heads, matchers);

        if (heads.isEmpty()) {
            result.add(article(null, null, "전문", "", null, content.trim(), 0));
            return result;
        }

        int order = 0;
        // 첫 조 앞의 도입부는 "전문" 으로 보존한다.
        String preamble = join(lines, 0, heads.get(0));
        if (!preamble.isBlank()) {
            result.add(article(null, null, "전문", "", null, preamble, order++));
        }

        for (int i = 0; i < heads.size(); i++) {
            Matcher m = matchers.get(i);
            int from = heads.get(i) + 1;
            int to = (i + 1 < heads.size()) ? heads.get(i + 1) : lines.length;

            Integer no = Integer.valueOf(m.group("no"));
            String subRaw = m.group("sub");
            Integer sub = subRaw == null ? null : Integer.valueOf(subRaw);
            String label = "제" + no + "조" + (sub == null ? "" : "의" + sub);
            String title = m.group("title") == null ? "" : m.group("title").trim();
            String note = blankToNull(m.group("note"));

            result.add(article(no, sub, label, title, note, join(lines, from, to), order++));
        }
        return result;
    }

    private void collect(String[] lines, Pattern pattern, List<Integer> heads, List<Matcher> matchers) {
        for (int i = 0; i < lines.length; i++) {
            Matcher m = pattern.matcher(lines[i]);
            if (m.matches()) {
                heads.add(i);
                matchers.add(m);
            }
        }
    }

    private PolicyArticle article(Integer no, Integer sub, String label, String title,
                                  String note, String body, int order) {
        return PolicyArticle.builder()
                .articleNo(no)
                .articleSubNo(sub)
                .articleLabel(label)
                .title(title == null ? "" : title)
                .note(note)
                .content(body == null ? "" : body.trim())
                .sortOrder(order)
                .build();
    }

    private String join(String[] lines, int from, int toExclusive) {
        StringBuilder sb = new StringBuilder();
        for (int i = from; i < toExclusive && i < lines.length; i++) {
            sb.append(lines[i]).append('\n');
        }
        return sb.toString().trim();
    }

    private String blankToNull(String v) {
        return (v == null || v.isBlank()) ? null : v.trim();
    }

    /** 제목에서 파생한 지침/장 정보 */
    public record Chapter(String guideline, Integer chapterNo, String chapterLabel, String chapterTitle) {}
}
