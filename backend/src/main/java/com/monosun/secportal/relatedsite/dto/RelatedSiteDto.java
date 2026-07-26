package com.monosun.secportal.relatedsite.dto;

import com.monosun.secportal.relatedsite.entity.RelatedSite;
import com.monosun.secportal.relatedsite.entity.RelatedSiteItem;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

public class RelatedSiteDto {

    @Getter @Builder
    public static class ItemResponse {
        private Long id;
        private String title;
        private String link;
        private String summary;
        private String publishedText;
        private LocalDateTime publishedAt;

        public static ItemResponse from(RelatedSiteItem i) {
            return ItemResponse.builder()
                    .id(i.getId())
                    .title(i.getTitle())
                    .link(i.getLink())
                    .summary(i.getSummary())
                    .publishedText(i.getPublishedText())
                    .publishedAt(i.getPublishedAt())
                    .build();
        }
    }

    @Getter @Builder
    public static class SiteResponse {
        private Long id;
        private String name;
        private String url;
        private String feedUrl;
        private String category;
        private String description;
        private Integer sortOrder;
        private boolean active;
        private String fetchStatus;
        private String fetchMessage;
        private String fetchedSummary;
        private LocalDateTime lastFetchedAt;
        private List<ItemResponse> items;

        public static SiteResponse from(RelatedSite s, List<RelatedSiteItem> items) {
            return SiteResponse.builder()
                    .id(s.getId())
                    .name(s.getName())
                    .url(s.getUrl())
                    .feedUrl(s.getFeedUrl())
                    .category(s.getCategory())
                    .description(s.getDescription())
                    .sortOrder(s.getSortOrder())
                    .active(s.isActive())
                    .fetchStatus(s.getFetchStatus() == null ? null : s.getFetchStatus().name())
                    .fetchMessage(s.getFetchMessage())
                    .fetchedSummary(s.getFetchedSummary())
                    .lastFetchedAt(s.getLastFetchedAt())
                    .items(items == null ? List.of() : items.stream().map(ItemResponse::from).toList())
                    .build();
        }
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor
    public static class SiteRequest {
        private String name;
        private String url;
        private String feedUrl;
        private String category;
        private String description;
        private Integer sortOrder;
        private Boolean active;
    }

    /** 새로고침 결과 요약 */
    @Getter @Builder
    public static class RefreshResult {
        private int total;      // 시도한 사이트 수
        private int succeeded;  // 게시물·소개문을 가져온 수
        private int failed;     // 접속 실패 수
        private int items;      // 수집한 게시물 총 건수
        private List<SiteResponse> sites;
    }
}
