package com.monosun.secportal.threat.entity;

import com.monosun.secportal.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "threats")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Threat extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

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

    @Column(length = 500)
    private String remark;
}
