package com.monosun.secportal.security.entity;

import com.monosun.secportal.common.entity.BaseEntity;
import jakarta.persistence.*;
import com.monosun.secportal.common.crypto.EncryptedStringConverter;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "security_integrations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SecurityIntegration extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(name = "solution_type", nullable = false)
    private String solutionType;

    private String vendor;

    private String host;

    /** 연동 API 키 — 저장 시 암호화(AES-256-GCM) */
    @Convert(converter = EncryptedStringConverter.class)
    @Column(name = "api_key", length = 1024)
    private String apiKey;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private ConnectionStatus status = ConnectionStatus.DISCONNECTED;

    @Column(name = "last_sync_at")
    private LocalDateTime lastSyncAt;

    public enum ConnectionStatus {
        CONNECTED, DISCONNECTED, ERROR
    }
}
