package com.monosun.secportal.security.entity;

import com.monosun.secportal.common.entity.BaseEntity;
import jakarta.persistence.*;
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

    @Column(name = "api_key")
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
