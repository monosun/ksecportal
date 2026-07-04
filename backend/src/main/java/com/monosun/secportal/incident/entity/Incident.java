package com.monosun.secportal.incident.entity;

import com.monosun.secportal.auth.entity.User;
import com.monosun.secportal.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "incidents")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Incident extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Severity severity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private Status status = Status.OPEN;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private IncidentType type;

    @Column(columnDefinition = "TEXT")
    private String affectedSystems;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reporter_id")
    private User reporter;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assignee_id")
    private User assignee;

    @Column(name = "assignee_text")
    private String assigneeText;

    private LocalDateTime detectedAt;
    private LocalDateTime resolvedAt;

    public enum Severity { CRITICAL, HIGH, MEDIUM, LOW }

    public enum Status { OPEN, INVESTIGATING, CONTAINED, RESOLVED, CLOSED }

    public enum IncidentType {
        MALWARE, PHISHING, DATA_BREACH, UNAUTHORIZED_ACCESS, DDOS, INSIDER_THREAT, PHYSICAL, OTHER
    }
}
