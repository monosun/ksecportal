package com.monosun.secportal.security.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "security_events")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SecurityEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "integration_id", nullable = false)
    private SecurityIntegration integration;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EventSeverity severity;

    @Column(name = "event_type", nullable = false)
    private String eventType;

    @Column(name = "source_ip")
    private String sourceIp;

    @Column(name = "destination_ip")
    private String destinationIp;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String message;

    @Column(name = "detected_at")
    private LocalDateTime detectedAt;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    public enum EventSeverity {
        CRITICAL, HIGH, MEDIUM, LOW, INFO
    }
}
