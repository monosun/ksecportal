package com.monosun.secportal.relatedsite.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 관련 사이트에서 가져온 최신 게시물 한 건.
 * 새로고침할 때마다 해당 사이트의 기존 항목을 지우고 다시 채운다(캐시 성격).
 */
@Entity
@Table(name = "related_site_items",
        indexes = @Index(name = "idx_relsite_item_site", columnList = "site_id"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RelatedSiteItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "site_id", nullable = false)
    private Long siteId;

    @Column(nullable = false, length = 500)
    private String title;

    @Column(length = 1000)
    private String link;

    @Column(columnDefinition = "TEXT")
    private String summary;

    /** 원문에 적힌 게시일 문자열 (형식이 제각각이라 그대로 보관) */
    @Column(name = "published_text", length = 100)
    private String publishedText;

    /** 파싱에 성공한 게시일 — 정렬·기간 표시에 쓴다 */
    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    /** 피드에 실린 순서 */
    @Column(name = "sort_order")
    @Builder.Default
    private Integer sortOrder = 0;

    @Column(name = "fetched_at")
    private LocalDateTime fetchedAt;
}
