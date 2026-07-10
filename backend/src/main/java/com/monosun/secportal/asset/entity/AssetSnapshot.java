package com.monosun.secportal.asset.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 자산 목록의 특정 시점(스냅샷) 헤더.
 * 저장 시점의 자산들을 {@link AssetSnapshotItem} 으로 복사해 보관한다.
 */
@Entity
@Table(name = "asset_snapshots")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AssetSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 시점 제목 (예: "2026년 상반기 자산 현황") */
    private String title;

    @Column(columnDefinition = "TEXT")
    private String memo;

    /** 저장 시점의 자산 수 */
    @Column(nullable = false)
    @Builder.Default
    private int assetCount = 0;

    /** 저장한 사용자 이름 */
    private String createdBy;

    @Column(nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
