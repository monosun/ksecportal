package com.monosun.secportal.policy.service;

import com.monosun.secportal.audit.service.AuditLogService;
import com.monosun.secportal.common.exception.ResourceNotFoundException;
import com.monosun.secportal.policy.dto.PolicyArticleDto;
import com.monosun.secportal.policy.entity.Policy;
import com.monosun.secportal.policy.entity.PolicyArticle;
import com.monosun.secportal.policy.repository.PolicyArticleRepository;
import com.monosun.secportal.policy.repository.PolicyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 보안정책 조(條) 세분화 · 조문 검색 서비스.
 *
 * <p>정책(장) 본문을 파싱해 {@link PolicyArticle} 로 펼쳐 두고, 지침/장/조/제목/본문 기준으로 검색한다.
 * 조 데이터는 정책 본문의 파생물이므로 정책 저장 시마다 {@link #sync(Policy)} 로 다시 만든다.
 */
@Service
@RequiredArgsConstructor
public class PolicyArticleService {

    /** 검색 범위 — 프론트 드롭다운과 1:1 대응 */
    public enum Scope { ALL, TITLE, CONTENT, GUIDELINE, CHAPTER, ARTICLE }

    private final PolicyArticleRepository articleRepository;
    private final PolicyRepository policyRepository;
    private final PolicyStructureParser parser;
    private final AuditLogService auditLogService;

    // ── 세분화(등록) ─────────────────────────────────────────────────────────

    /**
     * 정책 1건의 제목·본문을 다시 파싱해 지침/장 정보와 조 목록을 동기화한다.
     * 기존 조는 지우고 새로 만들기 때문에 본문 수정 결과가 항상 그대로 반영된다.
     *
     * @return 등록된 조 개수
     */
    @Transactional
    public int sync(Policy policy) {
        parser.applyTitleStructure(policy);

        // orphanRemoval 로 기존 조를 지우고 새로 파싱한 조로 교체한다.
        policy.getArticles().clear();
        for (PolicyArticle a : parser.parseArticles(policy.getContent())) {
            a.setPolicy(policy);
            policy.getArticles().add(a);
        }
        policyRepository.save(policy);
        return policy.getArticles().size();
    }

    /** 전체 정책을 다시 세분화한다. 관리자 재파싱 및 최초 구동 시 초기화에 쓰인다. */
    @Transactional
    public int syncAll() {
        int count = 0;
        for (Policy policy : policyRepository.findAll()) {
            count += sync(policy);
        }
        auditLogService.log("POLICY_ARTICLE_RESYNC", "POLICY", 0L, count + "개 조문 재등록");
        return count;
    }

    // ── 조회 · 검색 ──────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<PolicyArticleDto.Response> listByPolicy(Long policyId) {
        if (!policyRepository.existsById(policyId)) {
            throw new ResourceNotFoundException("Policy", policyId);
        }
        return articleRepository.findByPolicyIdOrderBySortOrderAsc(policyId).stream()
                .map(PolicyArticleDto.Response::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public Page<PolicyArticleDto.Response> search(String guideline, Integer chapterNo, Long policyId,
                                                  Integer articleNo, Policy.Status status, Policy.Category category,
                                                  String keyword, Scope scope, Pageable pageable) {
        String kw = (keyword == null || keyword.isBlank())
                ? null
                : "%" + keyword.trim().toLowerCase() + "%";
        Scope s = scope == null ? Scope.ALL : scope;
        boolean all = s == Scope.ALL;

        // 정렬은 쿼리의 ORDER BY(지침 > 장 > 조)를 쓰므로 Pageable 의 정렬은 떼어낸다.
        Pageable paging = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize());

        return articleRepository.search(
                blankToNull(guideline), chapterNo, policyId, articleNo, status, category, kw,
                all || s == Scope.TITLE,
                all || s == Scope.CONTENT,
                all || s == Scope.GUIDELINE,
                all || s == Scope.CHAPTER,
                all || s == Scope.ARTICLE,
                paging
        ).map(PolicyArticleDto.Response::from);
    }

    /** 검색 필터용 지침 &gt; 장 목록 */
    @Transactional(readOnly = true)
    public PolicyArticleDto.Facets facets() {
        Map<String, List<PolicyArticleDto.Chapter>> grouped = new LinkedHashMap<>();

        for (Object[] row : policyRepository.findChapterFacets()) {
            Long id = (Long) row[0];
            String guideline = (String) row[1];
            Integer chapterNo = (Integer) row[2];
            String chapterLabel = (String) row[3];
            String chapterTitle = (String) row[4];

            String key = (guideline == null || guideline.isBlank()) ? "(미분류)" : guideline;
            String label = buildChapterLabel(chapterLabel, chapterTitle);

            grouped.computeIfAbsent(key, k -> new ArrayList<>())
                    .add(PolicyArticleDto.Chapter.builder()
                            .policyId(id)
                            .chapterNo(chapterNo)
                            .chapterLabel(chapterLabel)
                            .chapterTitle(chapterTitle)
                            .label(label)
                            .build());
        }

        List<PolicyArticleDto.Guideline> guidelines = grouped.entrySet().stream()
                .map(e -> PolicyArticleDto.Guideline.builder()
                        .name(e.getKey())
                        .chapters(e.getValue())
                        .build())
                .toList();

        return PolicyArticleDto.Facets.builder()
                .guidelines(guidelines)
                .totalArticles((int) articleRepository.count())
                .build();
    }

    private String buildChapterLabel(String chapterLabel, String chapterTitle) {
        if (chapterLabel == null && chapterTitle == null) return "(장 없음)";
        if (chapterLabel == null) return chapterTitle;
        return chapterTitle == null ? chapterLabel : chapterLabel + " " + chapterTitle;
    }

    private String blankToNull(String v) {
        return (v == null || v.isBlank()) ? null : v.trim();
    }
}
