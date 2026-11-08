package com.monosun.secportal.detection.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/** 공격 탐지로 차단한 IP. blockedUntil 이 null 이면 해제할 때까지 차단한다. */
@Entity
@Table(name = "detection_blocked_ips", indexes = {
        @Index(name = "idx_detection_blocked_ip", columnList = "ip")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BlockedIp {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 64)
    private String ip;

    @Column(length = 500)
    private String reason;

    @Column(name = "blocked_until")
    private LocalDateTime blockedUntil;

    /** 자동 차단이면 SYSTEM, 수동이면 처리자 이메일 */
    @Column(name = "created_by", length = 200)
    private String createdBy;

    @Column(name = "event_id")
    private Long eventId;

    @Builder.Default
    private Boolean active = true;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "released_at")
    private LocalDateTime releasedAt;

    @Column(name = "released_by", length = 200)
    private String releasedBy;

    public boolean isEffective(LocalDateTime now) {
        return Boolean.TRUE.equals(active) && (blockedUntil == null || blockedUntil.isAfter(now));
    }
}
