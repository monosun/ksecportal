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

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    @Builder.Default
    private int likelihood = 3;

    @Column(nullable = false)
    @Builder.Default
    private int impact = 3;
}
