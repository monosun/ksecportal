package com.monosun.secportal.errorlog.dto;

import com.monosun.secportal.errorlog.entity.ErrorLog;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

public class ErrorLogDto {

    /** 목록 행 — 스택트레이스는 상세에서만 내려준다. */
    @Getter
    @Builder
    public static class Response {
        private Long id;
        private LocalDateTime occurredAt;
        private String level;
        private String source;
        private String status;
        private String exceptionType;
        private String message;
        private String httpMethod;
        private String requestUri;
        private Integer httpStatus;
        private String userName;
        private String ipAddress;
        private String fingerprint;
        private String note;
        private String handledByName;
        private LocalDateTime handledAt;

        public static Response from(ErrorLog e) {
            return Response.builder()
                    .id(e.getId())
                    .occurredAt(e.getOccurredAt())
                    .level(e.getLevel() != null ? e.getLevel().name() : null)
                    .source(e.getSource() != null ? e.getSource().name() : null)
                    .status(e.getStatus() != null ? e.getStatus().name() : null)
                    .exceptionType(e.getExceptionType())
                    .message(e.getMessage())
                    .httpMethod(e.getHttpMethod())
                    .requestUri(e.getRequestUri())
                    .httpStatus(e.getHttpStatus())
                    .userName(e.getUserName())
                    .ipAddress(e.getIpAddress())
                    .fingerprint(e.getFingerprint())
                    .note(e.getNote())
                    .handledByName(e.getHandledBy() != null ? e.getHandledBy().getName() : null)
                    .handledAt(e.getHandledAt())
                    .build();
        }
    }

    /** 상세 — 스택트레이스·User-Agent 포함 */
    @Getter
    @Builder
    public static class Detail {
        private Response summary;
        private String stackTrace;
        private String userAgent;

        public static Detail from(ErrorLog e) {
            return Detail.builder()
                    .summary(Response.from(e))
                    .stackTrace(e.getStackTrace())
                    .userAgent(e.getUserAgent())
                    .build();
        }
    }

    /** 처리 상태 변경 */
    @Getter
    @Setter
    @NoArgsConstructor
    public static class HandleRequest {
        @NotNull(message = "처리 상태를 선택해주세요.")
        private ErrorLog.Status status;
        private String note;
    }

    /** 화면(JS)에서 올리는 오류 리포트 */
    @Getter
    @Setter
    @NoArgsConstructor
    public static class ClientReport {
        private String name;
        private String message;
        private String stack;
        /** 오류가 난 화면 경로 */
        private String url;
    }

    @Getter
    @Builder
    public static class Stats {
        private long total;
        private long newCount;
        private long inProgress;
        private long resolved;
        private long ignored;
        private long last24h;
        private long last7d;
        private long errorLast24h;
        private List<TopItem> top;
    }

    @Getter
    @Builder
    public static class TopItem {
        private String exceptionType;
        private String requestUri;
        private long count;
        private LocalDateTime lastOccurredAt;
    }
}
