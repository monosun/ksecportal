package com.monosun.secportal.threat.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "threat_defaults")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ThreatDefault {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "risk_id", nullable = false, length = 20)
    private String riskId;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(nullable = false, length = 50)
    private String type;

    @Column(length = 100)
    private String category;

    @Column(name = "asset_detail", length = 100)
    private String assetDetail;

    /** 대상 자산유형(복수) — 코드관리 ASSET_TYPE 값을 콤마로 구분해 저장한다 (예: "SERVER,NETWORK") */
    @Column(name = "asset_types", length = 500)
    private String assetTypes;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    @Builder.Default
    private int likelihood = 3;

    @Column(nullable = false)
    @Builder.Default
    private int impact = 3;
}
