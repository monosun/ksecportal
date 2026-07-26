package com.monosun.secportal.relatedsite.entity;

import com.monosun.secportal.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 보안·개인정보 업무에 참고하는 외부 사이트 — 보안 가이드 및 자료 &gt; 관련 사이트 화면에서
 * 홈페이지를 등록해 두고, 각 사이트의 최신 게시물(RSS/Atom) 또는 대표 소개문(og 메타)을 가져와 보여준다.
 */
@Entity
@Table(name = "related_sites",
        indexes = {
                @Index(name = "idx_relsite_category", columnList = "category"),
                @Index(name = "idx_relsite_sort", columnList = "sort_order"),
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RelatedSite extends BaseEntity {

    /** 본문 수집 결과 */
    public enum FetchStatus {
        /** 아직 한 번도 가져오지 않음 */
        NONE,
        /** 게시물 목록(피드) 수집 성공 */
        FEED,
        /** 피드가 없어 사이트 소개문(og 메타)만 수집 */
        SUMMARY,
        /** 접속했으나 가져올 내용이 없음 */
        EMPTY,
        /** 접속 실패(망 차단·타임아웃·오류) */
        ERROR
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 사이트 이름 (예: 인터넷 보호나라) */
    @Column(nullable = false, length = 200)
    private String name;

    /** 홈페이지 주소 */
    @Column(nullable = false, length = 500)
    private String url;

    /**
     * 게시물 목록 주소(RSS/Atom). 비워두면 홈페이지에서 피드 링크를 자동 탐색하고,
     * 그래도 없으면 og 메타의 소개문만 가져온다.
     */
    @Column(name = "feed_url", length = 500)
    private String feedUrl;

    /** 분류 (유관기관·법령/제도·취약점 정보 등) */
    @Column(length = 100)
    private String category;

    /** 사이트 설명 — 비워두면 수집한 소개문을 화면에 대신 보여준다 */
    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "sort_order")
    @Builder.Default
    private Integer sortOrder = 0;

    /** 미사용 처리하면 관련 사이트 화면에서 제외된다 */
    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;

    // ── 수집 결과 (refresh 시 갱신) ──────────────────────────────────────────

    @Enumerated(EnumType.STRING)
    @Column(name = "fetch_status", length = 20)
    @Builder.Default
    private FetchStatus fetchStatus = FetchStatus.NONE;

    /** 수집 실패 사유 등 안내 문구 */
    @Column(name = "fetch_message", length = 500)
    private String fetchMessage;

    /** 수집한 사이트 소개문 (피드가 없을 때 화면에 보여준다) */
    @Column(name = "fetched_summary", columnDefinition = "TEXT")
    private String fetchedSummary;

    @Column(name = "last_fetched_at")
    private LocalDateTime lastFetchedAt;
}
