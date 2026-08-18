package com.monosun.secportal.isms.service;

import com.monosun.secportal.auth.entity.User;
import com.monosun.secportal.auth.repository.UserRepository;
import com.monosun.secportal.common.exception.BusinessException;
import com.monosun.secportal.common.exception.ResourceNotFoundException;
import com.monosun.secportal.common.service.FileStorageService;
import com.monosun.secportal.isms.dto.IsmsDto;
import com.monosun.secportal.isms.entity.IsmsEvidence;
import com.monosun.secportal.isms.entity.IsmsItem;
import com.monosun.secportal.isms.entity.IsmsItemNote;
import com.monosun.secportal.isms.entity.IsmsPolicyMapping;
import com.monosun.secportal.isms.repository.IsmsEvidenceRepository;
import com.monosun.secportal.isms.repository.IsmsItemNoteRepository;
import com.monosun.secportal.isms.repository.IsmsItemRepository;
import com.monosun.secportal.isms.repository.IsmsPolicyMappingRepository;
import com.monosun.secportal.policy.entity.Policy;
import com.monosun.secportal.policy.entity.PolicyArticle;
import com.monosun.secportal.policy.repository.PolicyArticleRepository;
import com.monosun.secportal.policy.repository.PolicyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.xssf.usermodel.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class IsmsService {

    private final IsmsItemRepository itemRepository;
    private final IsmsEvidenceRepository evidenceRepository;
    private final UserRepository userRepository;
    private final FileStorageService fileStorageService;
    private final IsmsPolicyMappingRepository policyMappingRepository;
    private final PolicyRepository policyRepository;
    private final PolicyArticleRepository policyArticleRepository;
    private final IsmsItemNoteRepository itemNoteRepository;

    @Transactional(readOnly = true)
    public List<IsmsDto.ItemResponse> listItems(Integer year, String domainCode) {
        List<IsmsItem> items = domainCode != null && !domainCode.isBlank()
                ? itemRepository.findByDomainCodeOrderBySortOrderAsc(domainCode)
                : itemRepository.findAllByOrderBySortOrderAsc();

        List<Long> itemIds = items.stream().map(IsmsItem::getId).collect(Collectors.toList());
        Map<Long, List<IsmsDto.PolicyRef>> mappingsMap = buildMappingsMap(itemIds);

        if (year == null) {
            return items.stream().map(item ->
                IsmsDto.ItemResponse.from(item, 0L, null, mappingsMap.get(item.getId()))
            ).collect(Collectors.toList());
        }

        List<Object[]> rawStats = evidenceRepository.countByItemAndStatusForYear(year);
        Map<Long, Map<String, Long>> statsMap = new HashMap<>();
        for (Object[] row : rawStats) {
            Long itemId = (Long) row[0];
            String status = ((IsmsEvidence.Status) row[1]).name();
            Long count = (Long) row[2];
            statsMap.computeIfAbsent(itemId, k -> new HashMap<>()).put(status, count);
        }

        return items.stream().map(item -> {
            Map<String, Long> stats = statsMap.getOrDefault(item.getId(), Collections.emptyMap());
            long evidenceCount = stats.values().stream().mapToLong(Long::longValue).sum();
            String latestStatus = deriveStatus(stats);
            return IsmsDto.ItemResponse.from(item, evidenceCount, latestStatus, mappingsMap.get(item.getId()));
        }).collect(Collectors.toList());
    }

    /** 장(章) 전체 매핑 */
    @Transactional
    public void mapPolicy(Long itemId, Long policyId) {
        if (policyMappingRepository.existsByIsmsItemIdAndPolicyIdAndPolicyArticleIsNull(itemId, policyId)) return;
        IsmsItem item = itemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("IsmsItem", itemId));
        Policy policy = policyRepository.findById(policyId)
                .orElseThrow(() -> new ResourceNotFoundException("Policy", policyId));
        policyMappingRepository.save(IsmsPolicyMapping.builder()
                .ismsItem(item).policy(policy).build());
    }

    /** 장 전체 매핑만 해제한다. 같은 장의 조 단위 매핑은 그대로 둔다. */
    @Transactional
    public void unmapPolicy(Long itemId, Long policyId) {
        policyMappingRepository.deleteByIsmsItemIdAndPolicyIdAndPolicyArticleIsNull(itemId, policyId);
    }

    /** 조(條) 단위 매핑 — 소속 장은 조에서 따라간다. */
    @Transactional
    public void mapArticle(Long itemId, Long articleId) {
        if (policyMappingRepository.existsByIsmsItemIdAndPolicyArticleId(itemId, articleId)) return;
        IsmsItem item = itemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("IsmsItem", itemId));
        PolicyArticle article = policyArticleRepository.findById(articleId)
                .orElseThrow(() -> new ResourceNotFoundException("PolicyArticle", articleId));
        policyMappingRepository.save(IsmsPolicyMapping.builder()
                .ismsItem(item).policy(article.getPolicy()).policyArticle(article).build());
    }

    @Transactional
    public void unmapArticle(Long itemId, Long articleId) {
        policyMappingRepository.deleteByIsmsItemIdAndPolicyArticleId(itemId, articleId);
    }

    /** 장 → 조 순으로 정렬해 화면에서 장 기준으로 묶기 쉽게 만든다(장 전체 매핑이 각 장의 맨 앞). */
    private static final Comparator<IsmsDto.PolicyRef> REF_ORDER =
            Comparator.comparing(IsmsDto.PolicyRef::getId)
                    .thenComparing(r -> r.getArticleId() == null ? 0 : 1)
                    .thenComparing(IsmsDto.PolicyRef::getArticleId, Comparator.nullsFirst(Comparator.naturalOrder()));

    private Map<Long, List<IsmsDto.PolicyRef>> buildMappingsMap(List<Long> itemIds) {
        if (itemIds.isEmpty()) return Collections.emptyMap();
        return policyMappingRepository.findByIsmsItemIdIn(itemIds).stream()
                .collect(Collectors.groupingBy(
                        m -> m.getIsmsItem().getId(),
                        Collectors.collectingAndThen(
                                Collectors.mapping(IsmsService::toPolicyRef, Collectors.toList()),
                                refs -> { refs.sort(REF_ORDER); return refs; })));
    }

    private static IsmsDto.PolicyRef toPolicyRef(IsmsPolicyMapping m) {
        Policy p = m.getPolicy();
        PolicyArticle a = m.getPolicyArticle();
        return IsmsDto.PolicyRef.builder()
                .id(p.getId())
                .title(p.getTitle())
                .status(p.getStatus().name())
                .category(p.getCategory().name())
                .guidelineName(p.getGuidelineName())
                .chapterLabel(p.getChapterLabel())
                .chapterTitle(p.getChapterTitle())
                .articleId(a == null ? null : a.getId())
                .articleLabel(a == null ? null : a.getArticleLabel())
                .articleTitle(a == null ? null : a.getTitle())
                .articleDisplayName(a == null ? null : a.getDisplayName())
                .build();
    }

    @Transactional(readOnly = true)
    public IsmsDto.ItemResponse getItem(Long id) {
        IsmsItem item = itemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("IsmsItem", id));
        return IsmsDto.ItemResponse.from(item);
    }

    // ── 항목별 의견·현재 상태 (연도별) / 이행 가이드 (연도 무관) ────────

    @Transactional(readOnly = true)
    public IsmsDto.ItemNoteResponse getItemNote(Long itemId, int year) {
        return itemNoteRepository.findByItemIdAndYear(itemId, year)
                .map(IsmsDto.ItemNoteResponse::from)
                .orElseGet(() -> IsmsDto.ItemNoteResponse.empty(itemId, year));
    }

    /** 항목·연도 조합당 1건이므로 없으면 만들고 있으면 갱신한다. */
    @Transactional
    public IsmsDto.ItemNoteResponse saveItemNote(Long itemId, int year, IsmsDto.ItemNoteRequest req, User user) {
        IsmsItem item = itemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("IsmsItem", itemId));

        IsmsItemNote note = itemNoteRepository.findByItemIdAndYear(itemId, year)
                .orElseGet(() -> IsmsItemNote.builder().item(item).year(year).build());

        note.setStatusNote(req.getStatusNote());
        note.setOpinion(req.getOpinion());
        note.setUpdater(user);

        return IsmsDto.ItemNoteResponse.from(itemNoteRepository.save(note));
    }

    @Transactional
    public IsmsDto.ItemResponse updateItemGuide(Long itemId, IsmsDto.ItemGuideRequest req) {
        IsmsItem item = itemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("IsmsItem", itemId));
        item.setGuide(req.getGuide());
        return IsmsDto.ItemResponse.from(itemRepository.save(item));
    }

    /** 코드관리 'ISMS-P 101항목' 탭 — 기본 증적제목·증적내용·이행가이드 일괄 수정 */
    @Transactional
    public IsmsDto.ItemResponse updateItemDefaults(Long itemId, IsmsDto.ItemDefaultsRequest req) {
        IsmsItem item = itemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("IsmsItem", itemId));
        item.setDefaultEvidenceTitle(req.getDefaultEvidenceTitle());
        item.setDefaultEvidenceContent(req.getDefaultEvidenceContent());
        item.setEvidenceExamples(req.getEvidenceExamples());
        item.setGuide(req.getGuide());
        return IsmsDto.ItemResponse.from(itemRepository.save(item));
    }

    @Transactional(readOnly = true)
    public List<IsmsDto.EvidenceResponse> listEvidences(Long itemId, Integer year) {
        List<IsmsEvidence> evidences = year != null
                ? evidenceRepository.findByItemIdAndYearOrderByCreatedAtDesc(itemId, year)
                : evidenceRepository.findByItemIdOrderByYearDescCreatedAtDesc(itemId);
        return evidences.stream().map(IsmsDto.EvidenceResponse::from).collect(Collectors.toList());
    }

    @Transactional
    public IsmsDto.EvidenceResponse createEvidence(Long itemId, IsmsDto.EvidenceCreateRequest request,
                                                   MultipartFile file, User user) throws IOException {
        IsmsItem item = itemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("IsmsItem", itemId));
        IsmsEvidence.Status status = parseStatus(request.getStatus(), IsmsEvidence.Status.COMPLIANT);
        IsmsEvidence evidence = IsmsEvidence.builder()
                .item(item)
                .year(request.getYear())
                .title(request.getTitle())
                .content(request.getContent())
                .status(status)
                .registrant(user)
                .build();
        evidence = evidenceRepository.save(evidence);
        if (file != null && !file.isEmpty()) {
            String path = fileStorageService.store(file, "isms/" + evidence.getId());
            evidence.setFilePath(path);
            evidence.setFileName(file.getOriginalFilename());
        }
        return IsmsDto.EvidenceResponse.from(evidence);
    }

    @Transactional
    public IsmsDto.EvidenceResponse updateEvidence(Long evidenceId, IsmsDto.EvidenceUpdateRequest request,
                                                   MultipartFile file) throws IOException {
        IsmsEvidence evidence = evidenceRepository.findById(evidenceId)
                .orElseThrow(() -> new ResourceNotFoundException("IsmsEvidence", evidenceId));
        if (request.getTitle() != null) evidence.setTitle(request.getTitle());
        if (request.getContent() != null) evidence.setContent(request.getContent());
        if (request.getStatus() != null) evidence.setStatus(parseStatus(request.getStatus(), evidence.getStatus()));
        if (file != null && !file.isEmpty()) {
            fileStorageService.delete(evidence.getFilePath());
            String path = fileStorageService.store(file, "isms/" + evidence.getId());
            evidence.setFilePath(path);
            evidence.setFileName(file.getOriginalFilename());
        }
        return IsmsDto.EvidenceResponse.from(evidence);
    }

    @Transactional
    public void deleteEvidence(Long evidenceId) throws IOException {
        IsmsEvidence evidence = evidenceRepository.findById(evidenceId)
                .orElseThrow(() -> new ResourceNotFoundException("IsmsEvidence", evidenceId));
        if (evidence.getFilePath() != null && evidence.getSourceEvidence() == null) {
            fileStorageService.delete(evidence.getFilePath());
        }
        evidenceRepository.delete(evidence);
    }

    @Transactional
    public IsmsDto.EvidenceResponse createEvidenceRef(Long itemId, IsmsDto.EvidenceRefRequest request, User user) {
        IsmsItem item = itemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("IsmsItem", itemId));
        IsmsEvidence source = evidenceRepository.findById(request.getSourceEvidenceId())
                .orElseThrow(() -> new ResourceNotFoundException("IsmsEvidence", request.getSourceEvidenceId()));
        if (source.getFilePath() == null)
            throw new IllegalArgumentException("참조 대상 증적에 파일이 없습니다.");
        IsmsEvidence.Status status = parseStatus(request.getStatus(), IsmsEvidence.Status.COMPLIANT);
        IsmsEvidence ref = IsmsEvidence.builder()
                .item(item)
                .year(request.getYear())
                .title(request.getTitle())
                .status(status)
                .sourceEvidence(source)
                .registrant(user)
                .build();
        return IsmsDto.EvidenceResponse.from(evidenceRepository.save(ref));
    }

    @Transactional(readOnly = true)
    public List<IsmsDto.EvidenceSearchResult> searchEvidences(Long excludeItemId, int year, String keyword) {
        String kw = keyword == null ? "" : keyword.trim();
        return evidenceRepository.searchForRef(year, excludeItemId, kw).stream()
                .map(e -> IsmsDto.EvidenceSearchResult.builder()
                        .id(e.getId())
                        .itemCode(e.getItem().getItemCode())
                        .itemName(e.getItem().getItemName())
                        .title(e.getTitle())
                        .fileName(e.getFileName())
                        .status(e.getStatus().name())
                        .build())
                .collect(Collectors.toList());
    }

    @Transactional
    public IsmsDto.EvidenceResponse removeEvidenceFile(Long evidenceId) throws IOException {
        IsmsEvidence evidence = evidenceRepository.findById(evidenceId)
                .orElseThrow(() -> new ResourceNotFoundException("IsmsEvidence", evidenceId));
        fileStorageService.delete(evidence.getFilePath());
        evidence.setFilePath(null);
        evidence.setFileName(null);
        return IsmsDto.EvidenceResponse.from(evidence);
    }

    @Transactional(readOnly = true)
    public Resource downloadEvidenceFile(Long evidenceId) {
        IsmsEvidence evidence = evidenceRepository.findById(evidenceId)
                .orElseThrow(() -> new ResourceNotFoundException("IsmsEvidence", evidenceId));
        if (evidence.getFilePath() == null && evidence.getSourceEvidence() != null) {
            evidence = evidence.getSourceEvidence();
        }
        if (evidence.getFilePath() == null) throw new ResourceNotFoundException("IsmsEvidence file", evidenceId);
        return fileStorageService.load(evidence.getFilePath());
    }

    @Transactional(readOnly = true)
    public IsmsEvidence getEvidence(Long evidenceId) {
        return evidenceRepository.findById(evidenceId)
                .orElseThrow(() -> new ResourceNotFoundException("IsmsEvidence", evidenceId));
    }

    // ── 전년도 증적 가져오기 ─────────────────────────────────────────────────────

    /** 대상 연도 이전에 증적이 등록된 가장 최근 연도 (없으면 null) */
    @Transactional(readOnly = true)
    public Integer findPreviousYearWithEvidences(int year) {
        List<Integer> years = evidenceRepository.findYearsBefore(year);
        return years.isEmpty() ? null : years.get(0);
    }

    /** 가져오기 / 가져오기 초기화 버튼 상태 */
    @Transactional(readOnly = true)
    public IsmsDto.CopyPreviousStatus copyPreviousStatus(int year) {
        return IsmsDto.CopyPreviousStatus.builder()
                .previousYear(findPreviousYearWithEvidences(year))
                .copiedEvidences(evidenceRepository.countByYearAndCopiedFromYearIsNotNull(year))
                .copiedNotes(itemNoteRepository.countByYearAndCopiedFromYearIsNotNull(year))
                .copiedFromYear(evidenceRepository.findCopiedFromYear(year))
                .build();
    }

    /**
     * 이전(가장 최근) 연도의 증적을 대상 연도로 복사한다.
     * 증적제목·증적내용·준수상태·첨부파일과 연도별 현재상태·의견을 그대로 이어받으며,
     * 대상 연도에 이미 증적이 있는 항목은 건드리지 않고 건너뛴다(중복 복사 방지).
     */
    @Transactional
    public IsmsDto.CopyPreviousResult copyFromPreviousYear(int targetYear, User user) throws IOException {
        Integer sourceYear = findPreviousYearWithEvidences(targetYear);
        if (sourceYear == null) {
            throw new BusinessException("가져올 이전 연도 증적이 없습니다.");
        }

        Set<Long> itemsWithEvidence = new HashSet<>(evidenceRepository.findItemIdsByYear(targetYear));
        Set<Long> skippedItems = new HashSet<>();
        int copiedEvidences = 0;

        for (IsmsEvidence src : evidenceRepository.findByYearOrderByItemSortOrder(sourceYear)) {
            Long itemId = src.getItem().getId();
            if (itemsWithEvidence.contains(itemId)) {
                skippedItems.add(itemId);
                continue;
            }
            IsmsEvidence copy = evidenceRepository.save(IsmsEvidence.builder()
                    .item(src.getItem())
                    .year(targetYear)
                    .title(src.getTitle())
                    .content(src.getContent())
                    .fileName(src.getFileName())
                    .status(src.getStatus())
                    // 참조 증적은 원본 참조를 그대로 이어받는다(파일은 원본에서 내려받는다)
                    .sourceEvidence(src.getSourceEvidence())
                    .registrant(user)
                    .copiedFromYear(sourceYear)
                    .build());
            if (src.getFilePath() != null) {
                // 첨부 파일은 실물을 복제한다 — 한쪽 연도의 증적을 지워도 다른 연도가 깨지지 않도록.
                String path = fileStorageService.copy(src.getFilePath(), "isms/" + copy.getId());
                copy.setFilePath(path);
            }
            copiedEvidences++;
        }

        // 연도별 현재상태·의견 — 대상 연도에 아직 없는 항목만 복사
        Set<Long> itemsWithNote = itemNoteRepository.findByYear(targetYear).stream()
                .map(n -> n.getItem().getId()).collect(Collectors.toSet());
        int copiedNotes = 0;
        for (IsmsItemNote src : itemNoteRepository.findByYear(sourceYear)) {
            if (itemsWithNote.contains(src.getItem().getId())) continue;
            if (!notBlank(src.getStatusNote()) && !notBlank(src.getOpinion())) continue;
            itemNoteRepository.save(IsmsItemNote.builder()
                    .item(src.getItem())
                    .year(targetYear)
                    .statusNote(src.getStatusNote())
                    .opinion(src.getOpinion())
                    .updater(user)
                    .copiedFromYear(sourceYear)
                    .build());
            copiedNotes++;
        }

        return IsmsDto.CopyPreviousResult.builder()
                .sourceYear(sourceYear)
                .targetYear(targetYear)
                .copiedEvidences(copiedEvidences)
                .copiedNotes(copiedNotes)
                .skippedItems(skippedItems.size())
                .build();
    }

    /**
     * 전년도 가져오기로 만들어진 증적·현재상태·의견을 모두 지워 가져오기 전 상태로 되돌린다.
     * 직접 등록·작성한 기록(copiedFromYear = null)은 건드리지 않는다.
     * 가져온 증적을 참조하는 증적이 있으면 참조가 깨지므로 함께 삭제한다.
     */
    @Transactional
    public IsmsDto.RevertCopyResult revertCopyPrevious(int targetYear) throws IOException {
        List<IsmsEvidence> copied = evidenceRepository.findByYearAndCopiedFromYearIsNotNull(targetYear);
        List<IsmsItemNote> copiedNotes = itemNoteRepository.findByYearAndCopiedFromYearIsNotNull(targetYear);
        if (copied.isEmpty() && copiedNotes.isEmpty()) {
            throw new BusinessException("되돌릴 가져오기 내역이 없습니다.");
        }

        Integer copiedFromYear = evidenceRepository.findCopiedFromYear(targetYear);
        Set<Long> copiedIds = copied.stream().map(IsmsEvidence::getId).collect(Collectors.toSet());

        // 가져온 증적을 참조 중인 증적 먼저 정리 (참조 증적 자신은 파일을 갖지 않는다)
        int removedReferences = 0;
        if (!copiedIds.isEmpty()) {
            List<IsmsEvidence> referrers = evidenceRepository.findBySourceEvidenceIdIn(copiedIds).stream()
                    .filter(e -> !copiedIds.contains(e.getId()))
                    .collect(Collectors.toList());
            if (!referrers.isEmpty()) {
                evidenceRepository.deleteAll(referrers);
                evidenceRepository.flush();
                removedReferences = referrers.size();
            }
        }

        for (IsmsEvidence e : copied) {
            // 복사 시 실물을 복제했으므로 원본 연도 파일에는 영향이 없다.
            if (e.getFilePath() != null && e.getSourceEvidence() == null) {
                fileStorageService.delete(e.getFilePath());
            }
        }
        evidenceRepository.deleteAll(copied);
        itemNoteRepository.deleteAll(copiedNotes);

        return IsmsDto.RevertCopyResult.builder()
                .targetYear(targetYear)
                .copiedFromYear(copiedFromYear)
                .removedEvidences(copied.size())
                .removedNotes(copiedNotes.size())
                .removedReferences(removedReferences)
                .build();
    }

    @Transactional(readOnly = true)
    public IsmsDto.SummaryResponse summary(int year) {
        List<IsmsItem> allItems = itemRepository.findAllByOrderBySortOrderAsc();
        List<Object[]> rawStats = evidenceRepository.countByItemAndStatusForYear(year);

        Map<Long, Map<String, Long>> statsMap = new HashMap<>();
        for (Object[] row : rawStats) {
            Long itemId = (Long) row[0];
            String status = ((IsmsEvidence.Status) row[1]).name();
            Long count = (Long) row[2];
            statsMap.computeIfAbsent(itemId, k -> new HashMap<>()).put(status, count);
        }

        // Group items by domain
        Map<String, List<IsmsItem>> byDomain = allItems.stream()
                .collect(Collectors.groupingBy(IsmsItem::getDomainCode, LinkedHashMap::new, Collectors.toList()));

        int totalCompliant = 0, totalPartial = 0, totalNonCompliant = 0, totalNa = 0, totalNoEvidence = 0;
        List<IsmsDto.DomainSummary> domainSummaries = new ArrayList<>();

        for (Map.Entry<String, List<IsmsItem>> entry : byDomain.entrySet()) {
            String dc = entry.getKey();
            List<IsmsItem> domainItems = entry.getValue();
            int compliant = 0, partial = 0, nonCompliant = 0, na = 0, noEvidence = 0;

            for (IsmsItem item : domainItems) {
                Map<String, Long> stats = statsMap.getOrDefault(item.getId(), Collections.emptyMap());
                if (stats.isEmpty()) {
                    noEvidence++;
                } else {
                    String status = deriveStatus(stats);
                    switch (status != null ? status : "NO_EVIDENCE") {
                        case "COMPLIANT" -> compliant++;
                        case "PARTIAL" -> partial++;
                        case "NON_COMPLIANT" -> nonCompliant++;
                        case "NA" -> na++;
                        default -> noEvidence++;
                    }
                }
            }

            totalCompliant += compliant;
            totalPartial += partial;
            totalNonCompliant += nonCompliant;
            totalNa += na;
            totalNoEvidence += noEvidence;

            domainSummaries.add(IsmsDto.DomainSummary.builder()
                    .domainCode(dc)
                    .domainName(domainItems.get(0).getDomainName())
                    .sectionNum(domainItems.get(0).getSectionNum())
                    .total(domainItems.size())
                    .compliant(compliant)
                    .partial(partial)
                    .nonCompliant(nonCompliant)
                    .na(na)
                    .noEvidence(noEvidence)
                    .build());
        }

        return IsmsDto.SummaryResponse.builder()
                .year(year)
                .totalItems(allItems.size())
                .compliant(totalCompliant)
                .partial(totalPartial)
                .nonCompliant(totalNonCompliant)
                .na(totalNa)
                .noEvidence(totalNoEvidence)
                .byDomain(domainSummaries)
                .build();
    }

    // ── CSV 내보내기 ─────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public byte[] exportCsv(int year) {
        List<IsmsItem> allItems = itemRepository.findAllByOrderBySortOrderAsc();
        List<IsmsEvidence> evidences = evidenceRepository.findByYearOrderByItemSortOrder(year);

        Map<Long, List<IsmsEvidence>> byItem = evidences.stream()
                .collect(Collectors.groupingBy(e -> e.getItem().getId()));

        StringBuilder sb = new StringBuilder();
        sb.append("항목코드,항목명,섹션,도메인,증적제목,증적내용,파일명/경로,준수상태,등록자\n");

        for (IsmsItem item : allItems) {
            List<IsmsEvidence> evList = byItem.getOrDefault(item.getId(), Collections.emptyList());
            if (evList.isEmpty()) {
                sb.append(String.join(",",
                        csvField(item.getItemCode()), csvField(item.getItemName()),
                        String.valueOf(item.getSectionNum()), csvField(item.getDomainName()),
                        "", "", "", "", "")).append('\n');
            } else {
                for (IsmsEvidence ev : evList) {
                    sb.append(String.join(",",
                            csvField(item.getItemCode()), csvField(item.getItemName()),
                            String.valueOf(item.getSectionNum()), csvField(item.getDomainName()),
                            csvField(ev.getTitle()),
                            csvField(ev.getContent() != null ? ev.getContent() : ""),
                            csvField(ev.getFileName() != null ? ev.getFileName() : ""),
                            ev.getStatus().name(),
                            csvField(ev.getRegistrant() != null ? ev.getRegistrant().getName() : "")
                    )).append('\n');
                }
            }
        }
        // UTF-8 BOM (EF BB BF) 명시적 추가 — 리터럴 char 의존 없이 확실한 Excel 한글 호환
        byte[] bom = {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};
        byte[] content = sb.toString().getBytes(StandardCharsets.UTF_8);
        byte[] result = new byte[bom.length + content.length];
        System.arraycopy(bom, 0, result, 0, bom.length);
        System.arraycopy(content, 0, result, bom.length, content.length);
        return result;
    }

    // ── 일괄등록 엑셀 템플릿 ──────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public byte[] getImportTemplate() throws IOException {
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            // ── 입력 시트 ──────────────────────────────────────────────────────
            XSSFSheet input = wb.createSheet("증적입력");

            XSSFCellStyle headerStyle = wb.createCellStyle();
            headerStyle.setFillForegroundColor(IndexedColors.CORNFLOWER_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            XSSFFont hf = wb.createFont();
            hf.setBold(true);
            hf.setColor(IndexedColors.WHITE.getIndex());
            headerStyle.setFont(hf);

            // '예시 증적자료'는 참고용 열이며 일괄등록 시 읽지 않는다.
            // 기존에 내려받은 템플릿도 그대로 올릴 수 있도록 0~5 열 순서는 바꾸지 않는다.
            String[] headers = {"항목코드", "증적제목", "증적내용", "이행가이드", "파일명/경로", "준수상태", "예시 증적자료(참고)"};
            int[] colWidths = {3000, 8000, 16000, 20000, 10000, 4000, 16000};
            XSSFRow hRow = input.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                XSSFCell cell = hRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
                input.setColumnWidth(i, colWidths[i]);
            }

            // 이행가이드 셀 — 긴 예시가 들어가므로 줄바꿈 유지 + 상단 정렬
            XSSFCellStyle guideCellStyle = wb.createCellStyle();
            guideCellStyle.setWrapText(true);
            guideCellStyle.setVerticalAlignment(org.apache.poi.ss.usermodel.VerticalAlignment.TOP);

            // 항목별로 한 행씩 미리 채운다. '이행가이드' 열에는 해당 항목의 이행가이드
            // (증적예시 포함)를 넣으며, 가이드가 없는 항목은 비워 둔다(선택 입력 열).
            List<IsmsItem> items = itemRepository.findAllByOrderBySortOrderAsc();
            for (int r = 0; r < items.size(); r++) {
                IsmsItem it = items.get(r);
                XSSFRow row = input.createRow(r + 1);
                row.createCell(0).setCellValue(it.getItemCode());
                row.createCell(1).setCellValue(notBlank(it.getDefaultEvidenceTitle())
                        ? it.getDefaultEvidenceTitle() : it.getItemName() + " 증적자료");
                row.createCell(2).setCellValue(notBlank(it.getDefaultEvidenceContent())
                        ? it.getDefaultEvidenceContent() : it.getItemName() + " 증적 내용을 기재하세요");
                XSSFCell guideCell = row.createCell(3);
                guideCell.setCellValue(it.getGuide() != null ? it.getGuide() : "");
                guideCell.setCellStyle(guideCellStyle);
                row.createCell(4).setCellValue("docs/isms/" + it.getItemCode() + "_증적.pdf");
                row.createCell(5).setCellValue("COMPLIANT");
                XSSFCell examplesCell = row.createCell(6);
                examplesCell.setCellValue(it.getEvidenceExamples() != null ? it.getEvidenceExamples() : "");
                examplesCell.setCellStyle(guideCellStyle);
            }

            // ── 준수상태 안내 시트 ─────────────────────────────────────────────
            XSSFSheet guide = wb.createSheet("입력규칙");
            guide.createRow(0).createCell(0).setCellValue("준수상태 허용값");
            String[][] rules = {
                    {"COMPLIANT", "준수"},
                    {"PARTIAL",   "부분 준수"},
                    {"NON_COMPLIANT", "미준수"},
                    {"NA",        "해당없음 (N/A)"}
            };
            for (int i = 0; i < rules.length; i++) {
                XSSFRow row = guide.createRow(i + 1);
                row.createCell(0).setCellValue(rules[i][0]);
                row.createCell(1).setCellValue(rules[i][1]);
            }

            // ── 항목목록 참고 시트 ────────────────────────────────────────────
            XSSFSheet ref = wb.createSheet("항목목록(참고)");
            XSSFRow refH = ref.createRow(0);
            refH.createCell(0).setCellValue("항목코드");
            refH.createCell(1).setCellValue("항목명");
            refH.createCell(2).setCellValue("도메인");
            refH.createCell(3).setCellValue("예시 증적자료");

            for (int i = 0; i < items.size(); i++) {
                XSSFRow row = ref.createRow(i + 1);
                row.createCell(0).setCellValue(items.get(i).getItemCode());
                row.createCell(1).setCellValue(items.get(i).getItemName());
                row.createCell(2).setCellValue(items.get(i).getDomainName());
                XSSFCell refExamples = row.createCell(3);
                refExamples.setCellValue(items.get(i).getEvidenceExamples() != null
                        ? items.get(i).getEvidenceExamples() : "");
                refExamples.setCellStyle(guideCellStyle);
            }
            ref.setColumnWidth(0, 3000);
            ref.setColumnWidth(1, 10000);
            ref.setColumnWidth(2, 10000);
            ref.setColumnWidth(3, 16000);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            wb.write(baos);
            return baos.toByteArray();
        }
    }

    // ── 일괄등록 ────────────────────────────────────────────────────────────────

    @Transactional
    public IsmsDto.BulkImportResult bulkImport(int year, MultipartFile file, User user) throws IOException {
        String filename = file.getOriginalFilename() != null ? file.getOriginalFilename().toLowerCase() : "";
        List<String[]> rows = new ArrayList<>();

        if (filename.endsWith(".xlsx")) {
            try (XSSFWorkbook wb = new XSSFWorkbook(file.getInputStream())) {
                XSSFSheet sheet = wb.getSheetAt(0);
                for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                    XSSFRow row = sheet.getRow(i);
                    if (row == null) continue;
                    String code = xlsxCell(row, 0);
                    if (code.isBlank()) continue; // 빈 행 건너뜀
                    String title = xlsxCell(row, 1);
                    String content = xlsxCell(row, 2);
                    String guideCol = xlsxCell(row, 3);
                    String fileRef = xlsxCell(row, 4);
                    String status = xlsxCell(row, 5);
                    // 미작성 행 건너뜀: 증적제목·증적내용·파일명·준수상태가 모두 비어있으면
                    // (이행가이드만 남은 행) 작성되지 않은 것으로 본다.
                    if (title.isBlank() && content.isBlank() && fileRef.isBlank() && status.isBlank()) continue;
                    rows.add(new String[]{code, title, content, guideCol, fileRef, status});
                }
            }
        } else if (filename.endsWith(".csv")) {
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                boolean first = true;
                while ((line = reader.readLine()) != null) {
                    if (first) { first = false; continue; } // 헤더 건너뜀
                    if (line.isBlank()) continue;
                    String[] parsed = parseCsvLine(line);
                    if (parsed.length == 0 || parsed[0].trim().isBlank()) continue;
                    // 미작성 행 건너뜀 (xlsx 와 동일 규칙)
                    if (safeGet(parsed, 1).trim().isBlank() && safeGet(parsed, 2).trim().isBlank()
                            && safeGet(parsed, 4).trim().isBlank() && safeGet(parsed, 5).trim().isBlank()) continue;
                    rows.add(parsed);
                }
            }
        } else {
            throw new IllegalArgumentException(".xlsx 또는 .csv 파일만 지원합니다.");
        }

        int success = 0, failed = 0;
        List<IsmsDto.BulkImportResult.RowError> errors = new ArrayList<>();

        for (int i = 0; i < rows.size(); i++) {
            String[] row = rows.get(i);
            int rowNum = i + 2;
            String itemCode = safeGet(row, 0).trim();
            String title    = safeGet(row, 1).trim();

            if (itemCode.isBlank()) {
                errors.add(IsmsDto.BulkImportResult.RowError.builder().row(rowNum).itemCode("").message("항목코드가 비어있습니다").build());
                failed++;
                continue;
            }
            if (title.isBlank()) {
                errors.add(IsmsDto.BulkImportResult.RowError.builder().row(rowNum).itemCode(itemCode).message("증적제목이 비어있습니다").build());
                failed++;
                continue;
            }

            Optional<IsmsItem> itemOpt = itemRepository.findByItemCode(itemCode);
            if (itemOpt.isEmpty()) {
                errors.add(IsmsDto.BulkImportResult.RowError.builder().row(rowNum).itemCode(itemCode).message("존재하지 않는 항목코드: " + itemCode).build());
                failed++;
                continue;
            }

            IsmsItem item = itemOpt.get();
            // 이행가이드(선택 입력) — 값이 있으면 항목의 가이드를 갱신, 비어있으면 유지
            String guideCol = safeGet(row, 3).trim();
            if (!guideCol.isBlank()) {
                item.setGuide(guideCol);
            }

            IsmsEvidence.Status status = parseStatus(safeGet(row, 5).trim(), IsmsEvidence.Status.COMPLIANT);
            evidenceRepository.save(IsmsEvidence.builder()
                    .item(item)
                    .year(year)
                    .title(title)
                    .content(safeGet(row, 2))
                    .fileName(safeGet(row, 4))
                    .status(status)
                    .registrant(user)
                    .build());
            success++;
        }

        return IsmsDto.BulkImportResult.builder()
                .total(rows.size())
                .success(success)
                .failed(failed)
                .errors(errors)
                .build();
    }

    // ── 헬퍼 ─────────────────────────────────────────────────────────────────────

    private static boolean notBlank(String s) { return s != null && !s.isBlank(); }

    private static String csvField(String s) {
        if (s == null || s.isEmpty()) return "";
        if (s.contains(",") || s.contains("\"") || s.contains("\n") || s.contains("\r")) {
            return "\"" + s.replace("\"", "\"\"") + "\"";
        }
        return s;
    }

    private static String xlsxCell(XSSFRow row, int col) {
        XSSFCell cell = row.getCell(col);
        if (cell == null) return "";
        if (cell.getCellType() == CellType.NUMERIC) {
            double v = cell.getNumericCellValue();
            return v == Math.floor(v) ? String.valueOf((long) v) : String.valueOf(v);
        }
        return cell.toString().trim();
    }

    private static String[] parseCsvLine(String line) {
        if (line.startsWith("﻿")) line = line.substring(1);
        List<String> fields = new ArrayList<>();
        boolean inQ = false;
        StringBuilder sb = new StringBuilder();
        for (char c : line.toCharArray()) {
            if (c == '"') { inQ = !inQ; }
            else if (c == ',' && !inQ) { fields.add(sb.toString()); sb = new StringBuilder(); }
            else { sb.append(c); }
        }
        fields.add(sb.toString());
        return fields.toArray(new String[0]);
    }

    private static String safeGet(String[] arr, int idx) {
        return (arr != null && idx < arr.length && arr[idx] != null) ? arr[idx] : "";
    }

    private String deriveStatus(Map<String, Long> stats) {
        if (stats.isEmpty()) return null;
        if (stats.getOrDefault("NON_COMPLIANT", 0L) > 0) return "NON_COMPLIANT";
        if (stats.getOrDefault("PARTIAL", 0L) > 0) return "PARTIAL";
        if (stats.getOrDefault("COMPLIANT", 0L) > 0) return "COMPLIANT";
        if (stats.getOrDefault("NA", 0L) > 0) return "NA";
        return null;
    }

    private IsmsEvidence.Status parseStatus(String s, IsmsEvidence.Status defaultVal) {
        if (s == null) return defaultVal;
        try {
            return IsmsEvidence.Status.valueOf(s.toUpperCase());
        } catch (IllegalArgumentException e) {
            return defaultVal;
        }
    }
}
