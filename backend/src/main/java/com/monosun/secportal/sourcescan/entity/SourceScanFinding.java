package com.monosun.secportal.sourcescan.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "source_scan_findings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SourceScanFinding {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "scan_id", nullable = false)
    private SourceScan scan;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Category category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Severity severity;

    // 정렬용 (CRITICAL=0 … INFO=4)
    @Column(nullable = false)
    private int severityRank;

    @Column(nullable = false, length = 500)
    private String title;

    // 패키지명(의존성) 또는 룰 ID(코드) 또는 시크릿 유형
    @Column(length = 300)
    private String identifier;

    // manifest 경로 또는 파일:라인
    @Column(length = 500)
    private String location;

    @Column(length = 30)
    private String cveId;

    @Column(length = 500)
    private String htmlUrl;

    public enum Category {
        DEPENDENCY,     // Dependabot alerts
        CODE_SCANNING,  // Code scanning alerts (CodeQL 등)
        SECRET,         // Secret scanning alerts
        SAST            // 내장 OWASP 소스코드 정적분석 (소스 직접 점검)
    }

    public enum Severity {
        CRITICAL, HIGH, MEDIUM, LOW, INFO;

        public int rank() { return ordinal(); }

        public static Severity fromString(String s) {
            if (s == null) return INFO;
            try {
                return valueOf(s.trim().toUpperCase());
            } catch (Exception e) {
                return INFO;
            }
        }
    }
}
