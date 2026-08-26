package com.monosun.secportal.errorlog.service;

import com.monosun.secportal.auth.entity.User;
import com.monosun.secportal.common.excel.ExcelWriter;
import com.monosun.secportal.common.excel.ExportSupport;
import com.monosun.secportal.common.exception.ResourceNotFoundException;
import com.monosun.secportal.errorlog.dto.ErrorLogDto;
import com.monosun.secportal.errorlog.entity.ErrorLog;
import com.monosun.secportal.errorlog.repository.ErrorLogRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Sheet;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;

/**
 * 프로그램 처리 중 발생한 오류를 적재·조회·정리한다.
 * 기록은 어떤 경우에도 원래 요청을 방해하면 안 되므로 별도 트랜잭션에서 수행하고 예외를 삼킨다.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ErrorLogService {

    private static final int MSG_MAX = 4000;
    private static final int STACK_MAX = 20000;

    private final ErrorLogRepository errorLogRepository;

    /** 보관 기간(일). 지난 로그는 매일 새벽 자동 삭제한다. */
    @Value("${errorlog.retention-days:90}")
    private int retentionDays;

    // ── 기록 ────────────────────────────────────────────────────────────────

    /** 백엔드 예외 기록(전역 예외 처리기에서 호출) */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(ErrorLog.Level level, Throwable t, Integer httpStatus) {
        try {
            HttpServletRequest req = currentRequest();
            String stack = stackTrace(t);
            String type = t != null ? t.getClass().getName() : null;
            String message = t != null ? t.getMessage() : null;
            String uri = req != null ? req.getRequestURI() : null;
            User user = currentUser();

            errorLogRepository.save(ErrorLog.builder()
                    .level(level)
                    .source(ErrorLog.Source.BACKEND)
                    .exceptionType(type)
                    .message(cut(message, MSG_MAX))
                    .stackTrace(cut(stack, STACK_MAX))
                    .httpMethod(req != null ? req.getMethod() : null)
                    .requestUri(cut(withQuery(req, uri), 500))
                    .httpStatus(httpStatus)
                    .userId(user != null ? user.getId() : null)
                    .userName(user != null ? user.getName() : "System")
                    .ipAddress(extractIp(req))
                    .userAgent(req != null ? cut(req.getHeader("User-Agent"), 500) : null)
                    .fingerprint(fingerprint(type, message, firstFrame(stack), uri))
                    .build());
        } catch (Exception e) {
            log.warn("오류 로그 저장 실패: {}", e.getMessage());
        }
    }

    /** 스케줄러·배치 등 요청 밖에서 발생한 오류 기록 */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordJobFailure(String jobName, Throwable t) {
        try {
            String stack = stackTrace(t);
            String type = t != null ? t.getClass().getName() : null;
            String message = t != null ? t.getMessage() : null;
            errorLogRepository.save(ErrorLog.builder()
                    .level(ErrorLog.Level.ERROR)
                    .source(ErrorLog.Source.SCHEDULER)
                    .exceptionType(type)
                    .message(cut(message, MSG_MAX))
                    .stackTrace(cut(stack, STACK_MAX))
                    .requestUri(cut(jobName, 500))
                    .userName("System")
                    .fingerprint(fingerprint(type, message, firstFrame(stack), jobName))
                    .build());
        } catch (Exception e) {
            log.warn("오류 로그 저장 실패: {}", e.getMessage());
        }
    }

    /** 화면(JS)에서 올라온 오류 기록 */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordClient(ErrorLogDto.ClientReport report) {
        try {
            HttpServletRequest req = currentRequest();
            User user = currentUser();
            String type = report.getName() != null && !report.getName().isBlank() ? report.getName() : "Error";
            errorLogRepository.save(ErrorLog.builder()
                    .level(ErrorLog.Level.ERROR)
                    .source(ErrorLog.Source.FRONTEND)
                    .exceptionType(cut(type, 255))
                    .message(cut(report.getMessage(), MSG_MAX))
                    .stackTrace(cut(report.getStack(), STACK_MAX))
                    .requestUri(cut(report.getUrl(), 500))
                    .userId(user != null ? user.getId() : null)
                    .userName(user != null ? user.getName() : "Anonymous")
                    .ipAddress(extractIp(req))
                    .userAgent(req != null ? cut(req.getHeader("User-Agent"), 500) : null)
                    .fingerprint(fingerprint(type, report.getMessage(), firstFrame(report.getStack()), report.getUrl()))
                    .build());
        } catch (Exception e) {
            log.warn("화면 오류 로그 저장 실패: {}", e.getMessage());
        }
    }

    // ── 조회 ────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Page<ErrorLogDto.Response> list(ErrorLog.Level level, ErrorLog.Source source, ErrorLog.Status status,
                                           String keyword, LocalDateTime dateFrom, LocalDateTime dateTo,
                                           Pageable pageable) {
        return errorLogRepository.search(level, source, status, blankToNull(keyword), dateFrom, dateTo, pageable)
                .map(ErrorLogDto.Response::from);
    }

    @Transactional(readOnly = true)
    public ErrorLogDto.Detail get(Long id) {
        return ErrorLogDto.Detail.from(find(id));
    }

    @Transactional(readOnly = true)
    public ErrorLogDto.Stats stats() {
        LocalDateTime day = LocalDateTime.now().minusDays(1);
        LocalDateTime week = LocalDateTime.now().minusDays(7);
        List<ErrorLogDto.TopItem> top = errorLogRepository.topRepeated(week, PageRequest.of(0, 5)).stream()
                .map(r -> ErrorLogDto.TopItem.builder()
                        .exceptionType((String) r[0])
                        .requestUri((String) r[1])
                        .count(((Number) r[2]).longValue())
                        .lastOccurredAt((LocalDateTime) r[3])
                        .build())
                .toList();

        return ErrorLogDto.Stats.builder()
                .total(errorLogRepository.count())
                .newCount(errorLogRepository.countByStatus(ErrorLog.Status.NEW))
                .inProgress(errorLogRepository.countByStatus(ErrorLog.Status.IN_PROGRESS))
                .resolved(errorLogRepository.countByStatus(ErrorLog.Status.RESOLVED))
                .ignored(errorLogRepository.countByStatus(ErrorLog.Status.IGNORED))
                .last24h(errorLogRepository.countByOccurredAtAfter(day))
                .last7d(errorLogRepository.countByOccurredAtAfter(week))
                .errorLast24h(errorLogRepository.countByLevelAndOccurredAtAfter(ErrorLog.Level.ERROR, day))
                .top(top)
                .build();
    }

    // ── 처리·삭제 ────────────────────────────────────────────────────────────

    @Transactional
    public ErrorLogDto.Response handle(Long id, ErrorLogDto.HandleRequest req, User handler) {
        ErrorLog e = find(id);
        e.handle(req.getStatus(), cut(req.getNote(), MSG_MAX), handler);
        return ErrorLogDto.Response.from(e);
    }

    @Transactional
    public void delete(Long id) {
        errorLogRepository.delete(find(id));
    }

    /** 지정한 일수보다 오래된 로그 삭제 (0이면 전체 삭제) */
    @Transactional
    public long purgeOlderThan(int days) {
        LocalDateTime before = days <= 0 ? LocalDateTime.now() : LocalDateTime.now().minusDays(days);
        return errorLogRepository.deleteByOccurredAtBefore(before);
    }

    /** 처리완료·무시로 정리된 로그 삭제 */
    @Transactional
    public long purgeHandled() {
        return errorLogRepository.deleteByStatusIn(List.of(ErrorLog.Status.RESOLVED, ErrorLog.Status.IGNORED));
    }

    /** 보관 기간이 지난 로그 자동 정리 */
    @Scheduled(cron = "${errorlog.purge-cron:0 40 4 * * *}")
    @Transactional
    public void autoPurge() {
        if (retentionDays <= 0) return;
        long removed = errorLogRepository.deleteByOccurredAtBefore(LocalDateTime.now().minusDays(retentionDays));
        if (removed > 0) log.info("보관 기간({}일)이 지난 오류 로그 {}건을 정리했습니다.", retentionDays, removed);
    }

    // ── 내려받기 ─────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public byte[] exportExcel(ErrorLog.Level level, ErrorLog.Source source, ErrorLog.Status status,
                              String keyword, LocalDateTime dateFrom, LocalDateTime dateTo) throws IOException {
        List<ErrorLog> logs = errorLogRepository.searchAll(level, source, status, blankToNull(keyword),
                dateFrom, dateTo, PageRequest.of(0, 5000));

        try (ExcelWriter xw = new ExcelWriter()) {
            Sheet sheet = xw.sheet("에러 로그");
            int r = xw.title(sheet, 0, "에러 로그", 10);
            r++;
            r = xw.meta(sheet, r, new String[][]{
                    {"조회 조건", filterSummary(level, source, status, keyword, dateFrom, dateTo)},
                    {"건수", String.valueOf(logs.size())},
                    {"내려받은 시각", ExportSupport.now()},
            });
            r++;
            r = xw.header(sheet, r, new String[]{
                    "No", "발생일시", "등급", "발생위치", "처리상태", "오류 종류",
                    "메시지", "요청", "사용자", "IP", "처리메모"});
            int seq = 1;
            for (ErrorLog e : logs) {
                r = xw.row(sheet, r, new Object[]{
                        seq++,
                        ExportSupport.dt(e.getOccurredAt()),
                        e.getLevel() != null ? e.getLevel().name() : "-",
                        sourceLabel(e.getSource()),
                        statusLabel(e.getStatus()),
                        e.getExceptionType(),
                        e.getMessage(),
                        (e.getHttpMethod() != null ? e.getHttpMethod() + " " : "") + nvl(e.getRequestUri()),
                        nvl(e.getUserName()),
                        nvl(e.getIpAddress()),
                        nvl(e.getNote()),
                }, 0, 2, 3, 4);
            }
            xw.widths(sheet, 6, 20, 8, 12, 12, 32, 50, 36, 14, 16, 30);
            return xw.toBytes();
        }
    }

    public static String statusLabel(ErrorLog.Status s) {
        if (s == null) return "-";
        return switch (s) {
            case NEW -> "미확인";
            case IN_PROGRESS -> "확인중";
            case RESOLVED -> "처리완료";
            case IGNORED -> "무시";
        };
    }

    public static String sourceLabel(ErrorLog.Source s) {
        if (s == null) return "-";
        return switch (s) {
            case BACKEND -> "서버";
            case FRONTEND -> "화면";
            case SCHEDULER -> "스케줄러";
        };
    }

    // ── 내부 헬퍼 ────────────────────────────────────────────────────────────

    private ErrorLog find(Long id) {
        return errorLogRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("오류 로그", id));
    }

    private String filterSummary(ErrorLog.Level level, ErrorLog.Source source, ErrorLog.Status status,
                                 String keyword, LocalDateTime dateFrom, LocalDateTime dateTo) {
        StringBuilder sb = new StringBuilder();
        if (level != null) sb.append("등급=").append(level.name()).append(" ");
        if (source != null) sb.append("위치=").append(sourceLabel(source)).append(" ");
        if (status != null) sb.append("상태=").append(statusLabel(status)).append(" ");
        if (keyword != null && !keyword.isBlank()) sb.append("검색어=").append(keyword).append(" ");
        if (dateFrom != null) sb.append("시작=").append(ExportSupport.dt(dateFrom)).append(" ");
        if (dateTo != null) sb.append("종료=").append(ExportSupport.dt(dateTo));
        return sb.length() == 0 ? "전체" : sb.toString().trim();
    }

    private static String nvl(String v) {
        return v != null ? v : "-";
    }

    private static String blankToNull(String v) {
        return v == null || v.isBlank() ? null : v.trim();
    }

    private static String cut(String v, int max) {
        if (v == null) return null;
        return v.length() <= max ? v : v.substring(0, max) + "…(생략)";
    }

    private static String withQuery(HttpServletRequest req, String uri) {
        if (req == null) return uri;
        String q = req.getQueryString();
        return q == null || q.isBlank() ? uri : uri + "?" + q;
    }

    private static String stackTrace(Throwable t) {
        if (t == null) return null;
        StringWriter sw = new StringWriter();
        t.printStackTrace(new PrintWriter(sw));
        return sw.toString();
    }

    private static String firstFrame(String stack) {
        if (stack == null) return null;
        for (String line : stack.split("\\R")) {
            String s = line.trim();
            if (s.startsWith("at ")) return s;
        }
        return null;
    }

    /**
     * 같은 원인의 오류를 묶기 위한 해시.
     * 메시지에 섞인 ID·타임스탬프 같은 숫자는 지워서 같은 오류가 흩어지지 않게 한다.
     */
    private static String fingerprint(String type, String message, String frame, String uri) {
        String normalized = (message == null ? "" : message).replaceAll("\\d+", "#");
        String base = nvl(type) + "|" + normalized + "|" + nvl(frame) + "|" + nvl(uri);
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(base.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (Exception e) {
            return String.valueOf(base.hashCode());
        }
    }

    private static User currentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof User user) return user;
        return null;
    }

    private static HttpServletRequest currentRequest() {
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            return attrs != null ? attrs.getRequest() : null;
        } catch (Exception e) {
            return null;
        }
    }

    private static String extractIp(HttpServletRequest req) {
        if (req == null) return null;
        try {
            String ip = req.getHeader("X-Forwarded-For");
            if (ip != null && !ip.isBlank()) return ip.split(",")[0].trim();
            ip = req.getHeader("X-Real-IP");
            if (ip != null && !ip.isBlank()) return ip.trim();
            return req.getRemoteAddr();
        } catch (Exception e) {
            return null;
        }
    }
}
