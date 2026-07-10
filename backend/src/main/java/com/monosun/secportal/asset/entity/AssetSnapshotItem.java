package com.monosun.secportal.asset.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * 자산 스냅샷에 포함된 개별 자산의 시점 복사본.
 * 이후 원본 자산이 변경·삭제되어도 이 값은 저장 시점 그대로 유지된다.
 */
@Entity
@Table(name = "asset_snapshot_items", indexes = @Index(name = "idx_snapshot", columnList = "snapshotId"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AssetSnapshotItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long snapshotId;

    /** 저장 시점의 원본 자산 ID (참고용) */
    private Long assetId;

    private String name;
    private String assetCategory;
    private String type;
    private String owner;
    private String department;
    private String operator;
    private String environment;
    private String location;
    private String status;
    private String cloudProvider;
    private String ipAddress;
    private String region;
    private String cloudResourceId;
    private String osType;
    private String criticality;

    @Column(nullable = false)
    @Builder.Default
    private boolean personalInfoIncluded = false;

    @Column(precision = 12, scale = 2)
    private BigDecimal monthlyCost;
}
