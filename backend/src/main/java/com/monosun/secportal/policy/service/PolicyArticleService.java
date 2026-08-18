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

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 보안정책 조(條) 세분화 · 조문 검색 서비스.
 *
 * <p>정책(장) 본문을 파싱해 {@link PolicyArticle} 로 펼쳐 두고, 지침/장/조/제목/본문 기준으로 검색한다.
 * 조 데이터는 정책 본문의 파생물이므로 정책 저장 시마다 {@link #sync(Policy)} 로 다시 맞춘다.
 */
@Service
@RequiredArgsConstructor
public class PolicyArticleService {

    /** 검색 범위 — 프론트 드롭다운과 1:1 대응 */
    /**
     * 검색 범위. {@code HEADING} 은 조 표기(제N조)와 조 제목을 함께 보는 범위로,
     * 본문까지 훑는 {@code ALL} 과 달리 조를 이름으로 찾을 때 쓴다
     * (ISMS-P 통제항목 매핑의 정책 추가 검색).
     */
    public enum Scope { ALL, TITLE, CONTENT, GUIDELINE, CHAPTER, ARTICLE, HEADING }

    private final PolicyArticleRepository articleRepository;
    private final PolicyRepository policyRepository;
    private final PolicyStructureParser parser;
    private final AuditLogService auditLogService;

    // ── 세분화(등록) ─────────────────────────────────────────────────────────

    /**
     * 정책 1건의 제목·본문을 다시 파싱해 지침/장 정보와 조 목록을 동기화한다.
     *
     * <p><b>조 레코드는 지우고 새로 만들지 않고 조 표기(제N조) 기준으로 재사용한다.</b>
     * ISMS-P 통제항목이 조 id 로 매핑을 걸어 두기 때문에, 본문을 조금 고쳤다고 id 가 바뀌면
     * 매핑이 통째로 사라진다. 본문에서 없어진 조만 orphanRemoval 로 지운다.
     *
     * @return 등록된 조 개수
     */
    @Transactional
    public int sync(Policy policy) {
        parser.applyTitleStructure(policy);

        // 같은 표기가 여러 번 나올 수 있으므로 표기별 큐에서 앞에서부터 하나씩 꺼내 쓴다.
        Map<String, Deque<PolicyArticle>> reusable = new LinkedHashMap<>();
        for (PolicyArticle a : policy.getArticles()) {
            reusable.computeIfAbsent(a.getArticleLabel(), k -> new ArrayDeque<>()).add(a);
        }

        Set<PolicyArticle> kept = Collections.newSetFromMap(new IdentityHashMap<>());
        List<PolicyArticle> added = new ArrayList<>();

        for (PolicyArticle parsed : parser.parseArticles(policy.getContent())) {
            Deque<PolicyArticle> bucket = reusable.get(parsed.getArticleLabel());
            PolicyArticle target = (bucket == null) ? null : bucket.poll();
            if (target == null) {
                added.add(parsed);
                continue;
            }
            target.setArticleNo(parsed.getArticleNo());
            target.setArticleSubNo(parsed.getArticleSubNo());
            target.setTitle(parsed.getTitle());
            target.setNote(parsed.getNote());
            target.setContent(parsed.getContent());
            target.setSortOrder(parsed.getSortOrder());
            kept.add(target);
        }

        // 본문에서 사라진 조는 여기서 빠지며 orphanRemoval 이 삭제한다.
        policy.getArticles().removeIf(a -> !kept.contains(a));
        for (PolicyArticle a : added) {
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
                all || s == Scope.TITLE || s == Scope.HEADING,
                all || s == Scope.CONTENT,
                all || s == Scope.GUIDELINE,
                all || s == Scope.CHAPTER,
                all || s == Scope.ARTICLE || s == Scope.HEADING,
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
