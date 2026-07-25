package com.monosun.secportal.glossary.service;

import com.monosun.secportal.audit.service.AuditLogService;
import com.monosun.secportal.common.exception.BusinessException;
import com.monosun.secportal.common.exception.ResourceNotFoundException;
import com.monosun.secportal.glossary.dto.GlossaryDto;
import com.monosun.secportal.glossary.entity.GlossaryTerm;
import com.monosun.secportal.glossary.repository.GlossaryTermRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class GlossaryService {

    private final GlossaryTermRepository repository;
    private final AuditLogService auditLogService;

    /**
     * 용어 목록. activeOnly=true 면 사용 중인 용어만 반환한다(용어집 화면용).
     * 검색은 용어·영문·약어·정의·키워드를 한 번에 훑는다 — 데이터가 수백 건 규모라 메모리 필터로 충분하다.
     */
    @Transactional(readOnly = true)
    public List<GlossaryDto.TermResponse> list(String keyword, String category, Boolean activeOnly) {
        List<GlossaryTerm> terms = Boolean.TRUE.equals(activeOnly)
                ? repository.findByActiveTrueOrderBySortOrderAscIdAsc()
                : repository.findAllByOrderBySortOrderAscIdAsc();

        String kw = keyword == null ? "" : keyword.trim().toLowerCase();
        String cat = category == null ? "" : category.trim();

        return terms.stream()
                .filter(t -> cat.isEmpty() || cat.equals(t.getCategory()))
                .filter(t -> kw.isEmpty() || matches(t, kw))
                .map(GlossaryDto.TermResponse::from)
                .toList();
    }

    private boolean matches(GlossaryTerm t, String kw) {
        return contains(t.getName(), kw) || contains(t.getNameEn(), kw) || contains(t.getAbbreviation(), kw)
                || contains(t.getCategory(), kw) || contains(t.getDefinition(), kw) || contains(t.getKeywords(), kw);
    }

    private boolean contains(String v, String kw) {
        return v != null && v.toLowerCase().contains(kw);
    }

    @Transactional(readOnly = true)
    public GlossaryDto.SummaryResponse summary() {
        List<GlossaryTerm> terms = repository.findByActiveTrueOrderBySortOrderAscIdAsc();

        Map<String, Long> counts = new LinkedHashMap<>();
        long abbreviations = 0;
        for (GlossaryTerm t : terms) {
            String c = (t.getCategory() == null || t.getCategory().isBlank()) ? "미분류" : t.getCategory();
            counts.merge(c, 1L, Long::sum);
            if (t.getAbbreviation() != null && !t.getAbbreviation().isBlank()) abbreviations++;
        }

        return GlossaryDto.SummaryResponse.builder()
                .total(terms.size())
                .abbreviations(abbreviations)
                .byCategory(counts.entrySet().stream()
                        .map(e -> GlossaryDto.CategoryCount.builder()
                                .category(e.getKey()).count(e.getValue()).build())
                        .toList())
                .build();
    }

    @Transactional
    public GlossaryDto.TermResponse create(GlossaryDto.TermRequest req) {
        if (req.getName() == null || req.getName().isBlank())
            throw new BusinessException("용어를 입력해주세요.");
        if (repository.existsByNameIgnoreCase(req.getName().trim()))
            throw new BusinessException("이미 등록된 용어입니다: " + req.getName().trim());

        int nextOrder = repository.findAllByOrderBySortOrderAscIdAsc().stream()
                .mapToInt(t -> t.getSortOrder() == null ? 0 : t.getSortOrder())
                .max().orElse(0) + 1;

        GlossaryTerm term = repository.save(GlossaryTerm.builder()
                .name(req.getName().trim())
                .nameEn(req.getNameEn())
                .abbreviation(req.getAbbreviation())
                .category(req.getCategory())
                .definition(req.getDefinition())
                .keywords(req.getKeywords())
                .sortOrder(req.getSortOrder() != null ? req.getSortOrder() : nextOrder)
                .active(req.getActive() == null || req.getActive())
                .build());

        auditLogService.log("CREATE", "GLOSSARY_TERM", term.getId(), "보안용어 등록: " + term.getName());
        return GlossaryDto.TermResponse.from(term);
    }

    @Transactional
    public GlossaryDto.TermResponse update(Long id, GlossaryDto.TermRequest req) {
        GlossaryTerm term = find(id);

        if (req.getName() != null && !req.getName().isBlank()) {
            String newName = req.getName().trim();
            if (!newName.equalsIgnoreCase(term.getName()) && repository.existsByNameIgnoreCase(newName))
                throw new BusinessException("이미 등록된 용어입니다: " + newName);
            term.setName(newName);
        }
        if (req.getNameEn() != null) term.setNameEn(req.getNameEn());
        if (req.getAbbreviation() != null) term.setAbbreviation(req.getAbbreviation());
        if (req.getCategory() != null) term.setCategory(req.getCategory());
        if (req.getDefinition() != null) term.setDefinition(req.getDefinition());
        if (req.getKeywords() != null) term.setKeywords(req.getKeywords());
        if (req.getSortOrder() != null) term.setSortOrder(req.getSortOrder());
        if (req.getActive() != null) term.setActive(req.getActive());

        auditLogService.log("UPDATE", "GLOSSARY_TERM", id, "보안용어 수정: " + term.getName());
        return GlossaryDto.TermResponse.from(term);
    }

    @Transactional
    public void delete(Long id) {
        GlossaryTerm term = find(id);
        auditLogService.log("DELETE", "GLOSSARY_TERM", id, "보안용어 삭제: " + term.getName());
        repository.delete(term);
    }

    private GlossaryTerm find(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("GlossaryTerm", id));
    }
}
