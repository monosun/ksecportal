package com.monosun.secportal.sourcescan.entity;

import jakarta.persistence.*;
import com.monosun.secportal.common.crypto.EncryptedStringConverter;
import lombok.*;

import java.time.LocalDateTime;

/**
 * GitHub 연동 설정 (단일 행).
 * 토큰은 ADMIN 전용 API에서만 다루며 조회 시 마스킹된다 — 공개 app_settings에 저장하지 않는다.
 */
@Entity
@Table(name = "github_config")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GithubConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** GitHub 토큰 — 저장 시 암호화(AES-256-GCM) */
    @Convert(converter = EncryptedStringConverter.class)
    @Column(length = 1024)
    private String token;

    // GitHub Enterprise 지원용 — 기본 https://api.github.com
    @Column(length = 300)
    private String apiBaseUrl;

    // 기본 owner(조직/계정) — 저장소 목록 조회 시 참고용
    @Column(length = 200)
    private String owner;

    @Column(nullable = false)
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PreUpdate
    void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
