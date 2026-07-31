package com.monosun.secportal.perf.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/** 성능관리 — 임계시간을 넘긴 화면(요청)·SQL 기록 */
@Entity
@Table(name = "slow_logs",
        indexes = {
                @Index(name = "idx_slow_occurred", columnList = "occurred_at"),
                @Index(name = "idx_slow_type", columnList = "log_type")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SlowLog {

    public enum LogType { SCREEN, SQL }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "log_type", nullable = false, length = 10)
    private SlowLog.LogType logType;

    /** 화면은 "METHOD /uri", SQL은 축약한 구문 */
    @Column(nullable = false, length = 500)
    private String target;

    /** 화면은 메뉴명(추정), SQL은 전체 구문 */
    @Column(columnDefinition = "TEXT")
    private String detail;

    @Column(name = "duration_ms", nullable = false)
    private long durationMs;

    /** 기록 시점의 임계값(ms) — 나중에 기준을 바꿔도 당시 기준을 알 수 있도록 함께 저장 */
    @Column(name = "threshold_ms", nullable = false)
    private int thresholdMs;

    @Column(length = 100)
    private String username;

    @Column(name = "ip_address", length = 60)
    private String ipAddress;

    @Column(name = "http_method", length = 10)
    private String httpMethod;

    @Column(name = "status_code")
    private Integer statusCode;

    @Column(name = "occurred_at", nullable = false)
    private LocalDateTime occurredAt;
}
