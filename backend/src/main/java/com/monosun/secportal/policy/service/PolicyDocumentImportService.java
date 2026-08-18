package com.monosun.secportal.policy.service;

import com.monosun.secportal.audit.service.AuditLogService;
import com.monosun.secportal.auth.entity.User;
import com.monosun.secportal.auth.repository.UserRepository;
import com.monosun.secportal.common.exception.BusinessException;
import com.monosun.secportal.policy.dto.PolicyDocumentDto;
import com.monosun.secportal.policy.entity.Policy;
import com.monosun.secportal.policy.repository.PolicyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;

/**
 * 지침 문서(PDF/DOCX/TXT/MD) 한 건을 <b>지침 &gt; 장 &gt; 조</b> 구조의 보안정책으로 등록한다.
 *
 * <p>문서를 장(章) 단위로 잘라 장마다 정책 1건을 만들고, 제목을
 * {@code "<지침명> - 제N장 <장제목>"} 으로 맞춘다. 이 제목 규칙이 곧 기존
 * {@link PolicyStructureParser} 의 입력이라, 저장하면 지침명·장 컬럼과 조(條) 레코드가
 * 평소 등록 경로와 똑같이 채워진다.
 *
 * <p>같은 제목의 장이 이미 있으면 <b>본문만 갱신</b>한다. 조 레코드는 조 표기 기준으로
 * 재사용되므로 ISMS-P 통제항목에 걸어둔 조 단위 매핑이 끊기지 않는다.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PolicyDocumentImportService {

    private final PolicyDocumentExtractor extractor;
    private final PolicyStructureParser parser;
    private final PolicyArticleService articleService;
    private final PolicyRepository policyRepository;
    private final UserRepository userRepository;
    private final AuditLogService auditLogService;

    /** 지침명을 추측할 때 제목 줄로 보지 않을 머리말성 낱말 */
    private static final List<String> NON_TITLE_HINTS =
            List.of("목차", "차례", "contents", "개정이력", "제정", "시행일");

    // ── 개별 정책 — 문서 파일에서 제목·본문만 뽑아 폼에 채운다 ───────────────

    /**
     * 문서에서 정책 1건의 초안(제목·본문)을 뽑는다. 저장은 하지 않는다.
     * 화면에서 사용자가 손본 뒤 평소 등록 절차로 저장하도록 하기 위한 것이다.
     */
    public PolicyDocumentDto.ExtractResult extractSingle(MultipartFile file) throws IOException {
        PolicyDocumentExtractor.Result r = extractor.extract(file);
        List<String> warnings = new ArrayList<>(r.warnings());

        String guessedTitle = guessTitle(r.text(), file.getOriginalFilename());
        int chapters = splitChapters(r.text()).size();
        int articles = countArticles(r.text());

        if (chapters > 1) {
            warnings.add("문서에서 장(章) " + chapters + "개를 찾았습니다. 장별로 나눠 등록하려면 "
                    + "'지침 문서 등록' 을 사용하세요 — 지금은 문서 전체가 정책 1건으로 들어갑니다.");
        }
        if (articles == 0) {
            warnings.add("조(條) 머리글을 찾지 못했습니다. 본문 전체가 '전문' 한 건으로 등록됩니다.");
        }

        return PolicyDocumentDto.ExtractResult.builder()
                .title(guessedTitle)
                .content(r.text())
                .articleCount(articles)
                .chapterCount(chapters)
                .warnings(warnings)
                .build();
    }

    // ── 지침 전체 — 장별 정책으로 등록 ──────────────────────────────────────

    /**
     * 지침 문서를 장별 정책으로 등록한다.
     *
     * @param dryRun true 면 저장하지 않고 무엇이 만들어질지만 돌려준다(미리보기).
     */
    @Transactional
    public PolicyDocumentDto.ImportResult importGuideline(
            MultipartFile file, PolicyDocumentDto.ImportOptions options, Long authorId) throws IOException {

        PolicyDocumentExtractor.Result extracted = extractor.extract(file);
        List<String> warnings = new ArrayList<>(extracted.warnings());
        String text = extracted.text();

        String guideline = firstNonBlank(
                options.getGuidelineName(),
                guessTitle(text, file.getOriginalFilename()));
        if (guideline.isBlank()) {
            throw new BusinessException("지침명을 찾지 못했습니다. 지침명을 직접 입력해 주세요.");
        }

        List<Section> sections = splitChapters(text);
        if (sections.isEmpty()) {
            // 장 구분이 없는 문서 — 지침명 그대로 정책 1건으로 등록한다.
            warnings.add("장(章) 머리글(제N장)을 찾지 못해 문서 전체를 정책 1건으로 등록합니다.");
            sections = List.of(new Section(null, null, null, text));
        }

        List<PolicyDocumentDto.ChapterDraft> drafts = new ArrayList<>();
        int created = 0, updated = 0, totalArticles = 0;

        for (Section s : sections) {
            String title = buildTitle(guideline, s);
            String content = s.body().strip();
            if (content.isBlank()) {
                warnings.add(title + " — 본문이 비어 있어 건너뜁니다.");
                continue;
            }

            int articleCount = countArticles(content);
            totalArticles += articleCount;
            Optional<Policy> existing = policyRepository.findFirstByTitle(title);

            drafts.add(PolicyDocumentDto.ChapterDraft.builder()
                    .title(title)
                    .chapterLabel(s.label())
                    .chapterTitle(s.title())
                    .articleCount(articleCount)
                    .contentLength(content.length())
                    .existing(existing.isPresent())
                    .existingPolicyId(existing.map(Policy::getId).orElse(null))
                    .build());

            if (options.isDryRun()) continue;

            if (existing.isPresent()) {
                Policy p = existing.get();
                p.setContent(content);
                if (options.getCategory() != null) p.setCategory(options.getCategory());
                if (options.getStatus() != null) p.setStatus(options.getStatus());
                if (options.getVersion() != null && !options.getVersion().isBlank()) p.setVersion(options.getVersion());
                if (options.getEffectiveDate() != null) p.setEffectiveDate(options.getEffectiveDate());
                articleService.sync(p);   // 조 레코드는 조 표기 기준으로 재사용된다(ISMS 매핑 유지)
                auditLogService.log("POLICY_UPDATED", "POLICY", p.getId(), "지침 문서 등록: " + title);
                updated++;
            } else {
                User author = userRepository.findById(authorId)
                        .orElseThrow(() -> new BusinessException("User not found"));
                Policy p = Policy.builder()
                        .title(title)
                        .content(content)
                        .category(options.getCategory() != null ? options.getCategory() : Policy.Category.GENERAL)
                        .status(options.getStatus() != null ? options.getStatus() : Policy.Status.DRAFT)
                        .version(options.getVersion() != null && !options.getVersion().isBlank()
                                ? options.getVersion() : "1.0")
                        .effectiveDate(options.getEffectiveDate())
                        .author(author)
                        .build();
                Policy saved = policyRepository.save(p);
                articleService.sync(saved);
                auditLogService.log("POLICY_CREATED", "POLICY", saved.getId(), "지침 문서 등록: " + title);
                created++;
            }
        }

        if (drafts.isEmpty()) {
            throw new BusinessException("등록할 내용을 찾지 못했습니다. 문서 본문을 확인해 주세요.");
        }

        return PolicyDocumentDto.ImportResult.builder()
                .guidelineName(guideline)
                .fileName(file.getOriginalFilename())
                .dryRun(options.isDryRun())
                .created(created)
                .updated(updated)
                .articleCount(totalArticles)
                .chapters(drafts)
                .warnings(warnings)
                .build();
    }

    // ── 파싱 보조 ──────────────────────────────────────────────────────────

    /** 장 머리글로 잘라낸 한 도막. label 이 null 이면 장 구분이 없는 문서다. */
    private record Section(Integer no, String label, String title, String body) {}

    /**
     * 본문을 장(章) 단위로 자른다. 첫 장 앞의 도입부(제정·개정 이력 등)는
     * 첫 장 본문 앞에 붙여 잃지 않도록 한다.
     */
    private List<Section> splitChapters(String text) {
        String[] lines = text.split("\n", -1);
        List<Integer> heads = new ArrayList<>();
        List<Matcher> matchers = new ArrayList<>();

        for (int i = 0; i < lines.length; i++) {
            String bare = lines[i].replaceFirst("^\\s*#{1,6}\\s*", "").strip();
            Matcher m = PolicyDocumentExtractor.CHAPTER_LINE.matcher(bare);
            if (m.matches()) {
                heads.add(i);
                matchers.add(m);
            }
        }
        if (heads.isEmpty()) return List.of();

        String preamble = join(lines, 0, heads.get(0)).strip();
        List<Section> sections = new ArrayList<>();
        for (int i = 0; i < heads.size(); i++) {
            Matcher m = matchers.get(i);
            int from = heads.get(i) + 1;
            int to = (i + 1 < heads.size()) ? heads.get(i + 1) : lines.length;
            String body = join(lines, from, to);
            if (i == 0 && !preamble.isBlank()) body = preamble + "\n\n" + body;

            String titlePart = m.group("title") == null ? null : m.group("title").strip();
            if (titlePart != null && titlePart.isBlank()) titlePart = null;

            if (m.group("supplement") != null) {
                sections.add(new Section(null, "부칙", titlePart, body));
            } else {
                int no = Integer.parseInt(m.group("no"));
                sections.add(new Section(no, "제" + no + "장", titlePart, body));
            }
        }
        return sections;
    }

    /** "개인정보보호 지침 - 제1장 총칙" — 기존 제목 규칙과 같은 형태로 맞춘다. */
    private String buildTitle(String guideline, Section s) {
        if (s.label() == null) return guideline;
        StringBuilder sb = new StringBuilder(guideline).append(" - ").append(s.label());
        if (s.title() != null && !s.title().isBlank()) sb.append(' ').append(s.title().strip());
        return sb.toString();
    }

    /** 조 개수 — '전문'(조 머리글 앞 도입부)은 빼고 센다. */
    private int countArticles(String content) {
        return (int) parser.parseArticles(content).stream()
                .filter(a -> a.getArticleNo() != null)
                .count();
    }

    /**
     * 지침명 추측 — 장·조 머리글이 아닌 첫 의미 있는 줄을 쓰고,
     * 마땅치 않으면 파일명(확장자 제외)을 쓴다.
     */
    private String guessTitle(String text, String filename) {
        for (String raw : text.split("\n", -1)) {
            String line = raw.replaceFirst("^\\s*#{1,6}\\s*", "").strip();
            if (line.isEmpty() || line.length() > 60) continue;
            if (PolicyDocumentExtractor.CHAPTER_LINE.matcher(line).matches()) break;
            if (line.startsWith("제") && line.contains("조")) break;
            String lower = line.toLowerCase();
            if (NON_TITLE_HINTS.stream().anyMatch(lower::contains)) continue;
            return line;
        }
        return stripExtension(filename);
    }

    private String stripExtension(String filename) {
        if (filename == null || filename.isBlank()) return "";
        int dot = filename.lastIndexOf('.');
        return (dot > 0 ? filename.substring(0, dot) : filename).strip();
    }

    private static String join(String[] lines, int from, int to) {
        return String.join("\n", java.util.Arrays.asList(lines).subList(from, Math.min(to, lines.length)));
    }

    private static String firstNonBlank(String a, String b) {
        if (a != null && !a.isBlank()) return a.strip();
        return b == null ? "" : b.strip();
    }
}
