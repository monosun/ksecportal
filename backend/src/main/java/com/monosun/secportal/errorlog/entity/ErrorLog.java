package com.monosun.secportal.errorlog.entity;

import com.monosun.secportal.auth.entity.User;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 프로그램 처리 중 발생한 오류 1건.
 * 백엔드 전역 예외 처리기(GlobalExceptionHandler), 스케줄러, 화면(JS)에서 기록한다.
 */
@Entity
@Table(name = "error_logs", indexes = {
        @Index(name = "idx_error_logs_occurred", columnList = "occurred_at"),
        @Index(name = "idx_error_logs_fingerprint", columnList = "fingerprint"),
        @Index(name = "idx_error_logs_status", columnList = "status")
})
@Getter
@NoArgsConstructor
public class ErrorLog {

    public enum Level { ERROR, WARN }

    /** 오류가 어디에서 발생했는지 */
    public enum Source { BACKEND, FRONTEND, SCHEDULER }

    /** 담당자가 관리하는 처리 상태 */
    public enum Status { NEW, IN_PROGRESS, RESOLVED, IGNORED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "occurred_at", nullable = false, updatable = false)
    private LocalDateTime occurredAt = LocalDateTime.now();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Level level = Level.ERROR;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Source source = Source.BACKEND;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status = Status.NEW;

    /** 예외 클래스명(프론트엔드는 오류 이름) */
    @Column(length = 255)
    private String exceptionType;

    @Column(columnDefinition = "TEXT")
    private String message;

    @Column(columnDefinition = "LONGTEXT")
    private String stackTrace;

    @Column(length = 10)
    private String httpMethod;

    @Column(length = 500)
    private String requestUri;

    private Integer httpStatus;

    private Long userId;

    @Column(length = 100)
    private String userName;

    @Column(length = 100)
    private String ipAddress;

    @Column(length = 500)
    private String userAgent;

    /** 같은 원인의 오류를 묶기 위한 해시(예외 타입 + 메시지 + 최상단 스택 + 요청 경로) */
    @Column(length = 64)
    private String fingerprint;

    /** 처리 메모 */
    @Column(columnDefinition = "TEXT")
    private String note;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "handled_by")
    private User handledBy;

    private LocalDateTime handledAt;

    @Builder
    public ErrorLog(Level level, Source source, String exceptionType, String message, String stackTrace,
                    String httpMethod, String requestUri, Integer httpStatus,
                    Long userId, String userName, String ipAddress, String userAgent,
                    String fingerprint, LocalDateTime occurredAt) {
        this.level = level != null ? level : Level.ERROR;
        this.source = source != null ? source : Source.BACKEND;
        this.status = Status.NEW;
        this.exceptionType = exceptionType;
        this.message = message;
        this.stackTrace = stackTrace;
        this.httpMethod = httpMethod;
        this.requestUri = requestUri;
        this.httpStatus = httpStatus;
        this.userId = userId;
        this.userName = userName;
        this.ipAddress = ipAddress;
        this.userAgent = userAgent;
        this.fingerprint = fingerprint;
        if (occurredAt != null) this.occurredAt = occurredAt;
    }

    /** 처리 상태·메모를 갱신한다. 상태가 NEW 로 돌아가면 처리 이력은 지운다. */
    public void handle(Status status, String note, User handler) {
        this.status = status != null ? status : this.status;
        this.note = note;
        if (this.status == Status.NEW) {
            this.handledBy = null;
            this.handledAt = null;
        } else {
            this.handledBy = handler;
            this.handledAt = LocalDateTime.now();
        }
    }
}
