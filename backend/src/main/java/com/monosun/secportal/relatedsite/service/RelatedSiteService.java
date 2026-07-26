package com.monosun.secportal.relatedsite.service;

import com.monosun.secportal.audit.service.AuditLogService;
import com.monosun.secportal.common.exception.BusinessException;
import com.monosun.secportal.common.exception.ResourceNotFoundException;
import com.monosun.secportal.relatedsite.dto.RelatedSiteDto;
import com.monosun.secportal.relatedsite.entity.RelatedSite;
import com.monosun.secportal.relatedsite.entity.RelatedSiteItem;
import com.monosun.secportal.relatedsite.repository.RelatedSiteItemRepository;
import com.monosun.secportal.relatedsite.repository.RelatedSiteRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class RelatedSiteService {

    private final RelatedSiteRepository repository;
    private final RelatedSiteItemRepository itemRepository;
    private final SiteContentFetcher fetcher;
    private final AuditLogService auditLogService;

    // ── 조회 ──────────────────────────────────────────────────────────────

    /** activeOnly=true 면 사용 중인 사이트만 (관련 사이트 화면용) */
    @Transactional(readOnly = true)
    public List<RelatedSiteDto.SiteResponse> list(String keyword, String category, Boolean activeOnly) {
        List<RelatedSite> sites = Boolean.FALSE.equals(activeOnly)
                ? repository.findAllByOrderBySortOrderAscIdAsc()
                : repository.findByActiveTrueOrderBySortOrderAscIdAsc();

        String kw = keyword == null ? "" : keyword.trim().toLowerCase();
        String cat = category == null ? "" : category.trim();

        List<RelatedSite> filtered = sites.stream()
                .filter(s -> cat.isEmpty() || cat.equals(s.getCategory()))
                .filter(s -> kw.isEmpty() || matches(s, kw))
                .toList();

        Map<Long, List<RelatedSiteItem>> itemsBySite = loadItems(filtered);
        return filtered.stream()
                .map(s -> RelatedSiteDto.SiteResponse.from(s, itemsBySite.get(s.getId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public RelatedSiteDto.SiteResponse get(Long id) {
        RelatedSite site = find(id);
        return RelatedSiteDto.SiteResponse.from(site, itemRepository.findBySiteIdOrderBySortOrderAscIdAsc(id));
    }

    private boolean matches(RelatedSite s, String kw) {
        return contains(s.getName(), kw) || contains(s.getUrl(), kw)
                || contains(s.getCategory(), kw) || contains(s.getDescription(), kw)
                || contains(s.getFetchedSummary(), kw);
    }

    private boolean contains(String v, String kw) {
        return v != null && v.toLowerCase().contains(kw);
    }

    /** 사이트별 게시물을 한 번에 읽어 N+1 조회를 피한다 */
    private Map<Long, List<RelatedSiteItem>> loadItems(List<RelatedSite> sites) {
        Map<Long, List<RelatedSiteItem>> map = new LinkedHashMap<>();
        if (sites.isEmpty()) return map;
        List<Long> ids = sites.stream().map(RelatedSite::getId).toList();
        for (RelatedSiteItem item : itemRepository.findBySiteIdInOrderBySortOrderAscIdAsc(ids)) {
            map.computeIfAbsent(item.getSiteId(), k -> new ArrayList<>()).add(item);
        }
        return map;
    }

    // ── 등록·수정·삭제 ────────────────────────────────────────────────────

    @Transactional
    public RelatedSiteDto.SiteResponse create(RelatedSiteDto.SiteRequest req) {
        if (req.getName() == null || req.getName().isBlank())
            throw new BusinessException("사이트 이름을 입력해주세요.");
        String url = normalizeUrl(req.getUrl(), true);
        if (repository.existsByUrlIgnoreCase(url))
            throw new BusinessException("이미 등록된 사이트입니다: " + url);

        int nextOrder = repository.findAllByOrderBySortOrderAscIdAsc().stream()
                .mapToInt(s -> s.getSortOrder() == null ? 0 : s.getSortOrder())
                .max().orElse(0) + 1;

        RelatedSite site = repository.save(RelatedSite.builder()
                .name(req.getName().trim())
                .url(url)
                .feedUrl(normalizeUrl(req.getFeedUrl(), false))
                .category(trimToNull(req.getCategory()))
                .description(trimToNull(req.getDescription()))
                .sortOrder(req.getSortOrder() != null ? req.getSortOrder() : nextOrder)
                .active(req.getActive() == null || req.getActive())
                .fetchStatus(RelatedSite.FetchStatus.NONE)
                .build());

        auditLogService.log("CREATE", "RELATED_SITE", site.getId(), "관련 사이트 등록: " + site.getName());

        // 등록 직후 한 번 가져와 화면에 바로 내용이 보이게 한다 (실패해도 등록은 유지)
        refreshOne(site);
        return RelatedSiteDto.SiteResponse.from(site, itemRepository.findBySiteIdOrderBySortOrderAscIdAsc(site.getId()));
    }

    @Transactional
    public RelatedSiteDto.SiteResponse update(Long id, RelatedSiteDto.SiteRequest req) {
        RelatedSite site = find(id);
        boolean sourceChanged = false;

        if (req.getName() != null && !req.getName().isBlank()) site.setName(req.getName().trim());
        if (req.getUrl() != null && !req.getUrl().isBlank()) {
            String url = normalizeUrl(req.getUrl(), true);
            if (!url.equalsIgnoreCase(site.getUrl())) {
                if (repository.existsByUrlIgnoreCase(url))
                    throw new BusinessException("이미 등록된 사이트입니다: " + url);
                site.setUrl(url);
                sourceChanged = true;
            }
        }
        if (req.getFeedUrl() != null) {
            String feed = normalizeUrl(req.getFeedUrl(), false);
            if (!Objects.equals(feed, site.getFeedUrl())) {
                site.setFeedUrl(feed);
                sourceChanged = true;
            }
        }
        if (req.getCategory() != null) site.setCategory(trimToNull(req.getCategory()));
        if (req.getDescription() != null) site.setDescription(trimToNull(req.getDescription()));
        if (req.getSortOrder() != null) site.setSortOrder(req.getSortOrder());
        if (req.getActive() != null) site.setActive(req.getActive());

        auditLogService.log("UPDATE", "RELATED_SITE", id, "관련 사이트 수정: " + site.getName());

        // 주소가 바뀌었으면 이전 게시물은 의미가 없으므로 새로 가져온다
        if (sourceChanged) refreshOne(site);
        return RelatedSiteDto.SiteResponse.from(site, itemRepository.findBySiteIdOrderBySortOrderAscIdAsc(id));
    }

    @Transactional
    public void delete(Long id) {
        RelatedSite site = find(id);
        itemRepository.deleteBySiteId(id);
        auditLogService.log("DELETE", "RELATED_SITE", id, "관련 사이트 삭제: " + site.getName());
        repository.delete(site);
    }

    // ── 내용 새로고침 ─────────────────────────────────────────────────────

    @Transactional
    public RelatedSiteDto.SiteResponse refresh(Long id) {
        RelatedSite site = find(id);
        refreshOne(site);
        return RelatedSiteDto.SiteResponse.from(site, itemRepository.findBySiteIdOrderBySortOrderAscIdAsc(id));
    }

    /** 사용 중인 사이트 전체를 새로고침한다 (화면의 "전체 새로고침"·일 1회 스케줄러 공용) */
    @Transactional
    public RelatedSiteDto.RefreshResult refreshAll() {
        List<RelatedSite> sites = repository.findByActiveTrueOrderBySortOrderAscIdAsc();
        int succeeded = 0, failed = 0, items = 0;

        for (RelatedSite site : sites) {
            int fetched = refreshOne(site);
            if (site.getFetchStatus() == RelatedSite.FetchStatus.ERROR) failed++;
            else succeeded++;
            items += fetched;
        }

        Map<Long, List<RelatedSiteItem>> itemsBySite = loadItems(sites);
        return RelatedSiteDto.RefreshResult.builder()
                .total(sites.size())
                .succeeded(succeeded)
                .failed(failed)
                .items(items)
                .sites(sites.stream().map(s -> RelatedSiteDto.SiteResponse.from(s, itemsBySite.get(s.getId()))).toList())
                .build();
    }

    /** 사이트 한 곳을 가져와 결과·게시물을 갱신한다. 반환값은 저장한 게시물 수. */
    private int refreshOne(RelatedSite site) {
        SiteContentFetcher.FetchResult result;
        try {
            result = fetcher.fetch(site);
        } catch (Exception e) {   // 수집 실패가 화면 전체를 막지 않도록 방어
            log.warn("[관련사이트] {} 수집 실패", site.getUrl(), e);
            result = SiteContentFetcher.FetchResult.builder()
                    .status(RelatedSite.FetchStatus.ERROR)
                    .message("수집 중 오류가 발생했습니다: " + e.getClass().getSimpleName())
                    .build();
        }

        site.setLastFetchedAt(LocalDateTime.now());
        site.setFetchStatus(result.getStatus());
        site.setFetchMessage(result.getMessage());
        if (result.getDiscoveredFeedUrl() != null) site.setFeedUrl(result.getDiscoveredFeedUrl());

        if (result.getStatus() == RelatedSite.FetchStatus.SUMMARY) {
            site.setFetchedSummary(result.getSummary());
        }

        List<SiteContentFetcher.FetchedItem> fetched = result.getItems();
        if (fetched != null && !fetched.isEmpty()) {
            itemRepository.deleteBySiteId(site.getId());
            itemRepository.flush();
            int order = 0;
            LocalDateTime now = LocalDateTime.now();
            for (SiteContentFetcher.FetchedItem f : fetched) {
                itemRepository.save(RelatedSiteItem.builder()
                        .siteId(site.getId())
                        .title(f.getTitle())
                        .link(f.getLink())
                        .summary(f.getSummary())
                        .publishedText(f.getPublishedText())
                        .publishedAt(f.getPublishedAt())
                        .sortOrder(order++)
                        .fetchedAt(now)
                        .build());
            }
            return fetched.size();
        }
        // 실패했을 때는 마지막으로 가져온 게시물을 그대로 두어 화면이 비지 않게 한다
        return 0;
    }

    // ── 공통 ──────────────────────────────────────────────────────────────

    /** http/https 주소만 허용한다 (서버가 직접 접속하는 주소이므로 형식을 강제) */
    private String normalizeUrl(String raw, boolean required) {
        String url = trimToNull(raw);
        if (url == null) {
            if (required) throw new BusinessException("사이트 주소(URL)를 입력해주세요.");
            return null;
        }
        if (!url.matches("(?i)^https?://.*")) url = "https://" + url;
        try {
            URI uri = URI.create(url);
            if (uri.getHost() == null || uri.getHost().isBlank())
                throw new BusinessException("올바른 주소 형식이 아닙니다: " + raw);
        } catch (IllegalArgumentException e) {
            throw new BusinessException("올바른 주소 형식이 아닙니다: " + raw);
        }
        return url;
    }

    private String trimToNull(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }

    private RelatedSite find(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("RelatedSite", id));
    }
}
