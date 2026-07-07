package com.monosun.secportal.sbom.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "sbom_components")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SbomComponent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "software_id", nullable = false)
    private SbomSoftware software;

    // CycloneDX component.type — library, framework, application, container,
    // operating-system, device, firmware, file
    @Column(nullable = false, length = 30)
    @Builder.Default
    private String componentType = "library";

    // CycloneDX component.group (네임스페이스, 예: org.springframework.boot)
    @Column(name = "group_name", length = 200)
    private String groupName;

    @Column(nullable = false, length = 200)
    private String libraryName;

    @Column(length = 100)
    private String libraryVersion;

    // CycloneDX component.purl (Package URL, 예: pkg:maven/org.apache.poi/poi-ooxml@5.2.5)
    @Column(length = 500)
    private String purl;

    // SPDX 라이선스 ID 권장 (CycloneDX licenses[].license.id)
    @Column(length = 100)
    private String license;

    @Column(columnDefinition = "TEXT")
    private String remarks;

    @Column(nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
