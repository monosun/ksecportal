package com.monosun.secportal.detection.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 자동화·AI 공격 탐지 이벤트.
 *
 * <p>같은 IP·같은 유형의 이벤트가 짧은 시간 안에 반복되면 새 행을 만들지 않고 {@link #occurrences} 와
 * {@link #lastSeenAt} 만 갱신한다(이벤트 폭주 방지). enum 컬럼은 MySQL 네이티브 ENUM 이 생기지 않도록
 * VARCHAR 로 고정한다 — 값 추가 시 마이그레이션이 필요 없다.
 */
@Entity
@Table(name = "detection_events", indexes = {
        @Index(name = "idx_detection_events_detected", columnList = "detected_at"),
        @Index(name = "idx_detection_events_ip", columnList = "source_ip"),
        @Index(name = "idx_detection_events_status", columnList = "status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DetectionEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "VARCHAR(30)")
    private Category category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "VARCHAR(20)")
    private Severity severity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "VARCHAR(20)")
    @Builder.Default
    private Status status = Status.NEW;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String detail;

    @Column(name = "source_ip", length = 64)
    private String sourceIp;

    @Column(name = "user_email", length = 200)
    private String userEmail;

    @Column(name = "user_agent", length = 500)
    private String userAgent;

    @Column(name = "request_path", length = 500)
    private String requestPath;

    /** 행위 분석 점수(행위 기반 탐지에서만 사용) */
    private Integer score;

    @Builder.Default
    private Integer occurrences = 1;

    @Column(name = "detected_at", nullable = false)
    private LocalDateTime detectedAt;

    @Column(name = "last_seen_at", nullable = false)
    private LocalDateTime lastSeenAt;

    /** 이 이벤트로 IP 를 자동 차단했는지 */
    @Column(name = "auto_blocked")
    @Builder.Default
    private Boolean autoBlocked = false;

    /** 침해사고로 전환된 경우 그 사고 ID */
    @Column(name = "incident_id")
    private Long incidentId;

    @Column(name = "handled_by", length = 100)
    private String handledBy;

    @Column(name = "handled_at")
    private LocalDateTime handledAt;

    @Column(name = "handler_note", length = 1000)
    private String handlerNote;

    public enum Category {
        /** 공격 도구가 흔히 찔러 보는 경로(.env, .git, wp-admin 등) 접근 */
        HONEYPOT_SCAN,
        /** robots.txt·숨김 링크로만 알 수 있는 미끼 경로 접근 — HTML/robots 를 파싱해 따라간 자동화 도구 */
        HONEYPOT_BAIT,
        /** 미끼 페이지의 프롬프트 인젝션 지시를 따른 요청 — LLM 에이전트로 확인 */
        LLM_AGENT,
        /** 미끼에만 노출한 가짜 API 키 사용 */
        HONEYTOKEN,
        /** 존재하지 않는 미끼 계정으로 로그인 시도 */
        HONEY_ACCOUNT,
        /** 요청 속도·간격 균일성·오류 비율 등 행위 점수 초과 */
        BEHAVIOR,
        /** 한 IP 에서 여러 계정·다수 로그인 실패(무차별 대입·크리덴셜 스터핑) */
        LOGIN_ATTACK,
        /** 수동 등록·알림 테스트 */
        TEST
    }

    public enum Severity {
        INFO, LOW, MEDIUM, HIGH, CRITICAL;

        public boolean atLeast(Severity other) {
            return other == null || this.ordinal() >= other.ordinal();
        }
    }

    public enum Status {
        /** 신규 */
        NEW,
        /** 확인함(조사 중) */
        ACKNOWLEDGED,
        /** 오탐 */
        FALSE_POSITIVE,
        /** 조치 완료 */
        RESOLVED,
        /** 침해사고로 전환 */
        ESCALATED
    }
}
