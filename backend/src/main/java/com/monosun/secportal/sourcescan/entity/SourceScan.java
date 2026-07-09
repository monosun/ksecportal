package com.monosun.secportal.sourcescan.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "source_scans")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SourceScan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // owner/repo 형식
    @Column(nullable = false, length = 300)
    private String repository;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private Status status = Status.SUCCESS;

    // 카테고리별 조회 결과 메시지 (미활성/권한 없음 등)
    @Column(columnDefinition = "TEXT")
    private String message;

    @Column(nullable = false) @Builder.Default private int criticalCount = 0;
    @Column(nullable = false) @Builder.Default private int highCount = 0;
    @Column(nullable = false) @Builder.Default private int mediumCount = 0;
    @Column(nullable = false) @Builder.Default private int lowCount = 0;

    @Column(nullable = false) @Builder.Default private int dependencyCount = 0;
    @Column(nullable = false) @Builder.Default private int codeCount = 0;
    @Column(nullable = false) @Builder.Default private int secretCount = 0;
    @Column(nullable = false) @Builder.Default private int sastCount = 0;

    @OneToMany(mappedBy = "scan", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("severityRank ASC, id ASC")
    @Builder.Default
    private List<SourceScanFinding> findings = new ArrayList<>();

    @Column(nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    public enum Status {
        SUCCESS,   // 모든 카테고리 점검 성공
        PARTIAL,   // 일부 카테고리 조회 실패(미활성·권한 없음)
        FAILED     // 저장소 접근 실패
    }
}
