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

    @Column(nullable = false, length = 200)
    private String libraryName;

    @Column(length = 100)
    private String libraryVersion;

    @Column(length = 100)
    private String license;

    @Column(columnDefinition = "TEXT")
    private String remarks;

    @Column(nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
