package com.monosun.secportal.detection.web;

import com.monosun.secportal.detection.service.DetectionSettingsService;
import com.monosun.secportal.detection.service.HoneyDataService;
import jakarta.annotation.PreDestroy;
import jakarta.servlet.AsyncContext;
import jakarta.servlet.AsyncEvent;
import jakarta.servlet.AsyncListener;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 타르핏(tarpit). 차단된 IP 를 바로 403 으로 끊지 않고, 무작위로 지연한 뒤 그럴듯한 미끼 데이터를 돌려준다 —
 * 자동화 도구·AI 에이전트가 차단 사실을 알아채지 못한 채 시간과 토큰을 소모하게 하고,
 * 미끼 데이터의 서명 ID 로 수집 데이터의 재사용까지 추적한다.
 *
 * <p>지연은 서블릿 비동기 처리로 구현해 대기 중에 요청 스레드를 점유하지 않는다. IP 별·전체 동시 대기 수에
 * 상한을 두어, 차단된 IP 가 동시 연결을 쏟아내도 서버 자원을 고갈시키지 못한다(상한 초과분은 지연 없이 503).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TarpitResponder {

    private static final int MAX_PER_IP = 4;
    private static final int MAX_TOTAL = 200;
    private static final String ERROR_BODY =
            "{\"success\":false,\"message\":\"일시적인 오류가 발생했습니다. 잠시 후 다시 시도하세요.\",\"data\":null}";
    private static final String LOGIN_FAIL_BODY =
            "{\"success\":false,\"message\":\"이메일 또는 비밀번호가 올바르지 않습니다\",\"data\":null}";
    private static final String NOT_FOUND_BODY =
            "{\"success\":false,\"message\":\"요청한 리소스를 찾을 수 없습니다.\",\"data\":null}";

    private final DetectionSettingsService settingsService;
    private final HoneyDataService honeyDataService;

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2, r -> {
        Thread t = new Thread(r, "tarpit");
        t.setDaemon(true);
        return t;
    });
    private final Map<String, AtomicInteger> perIp = new ConcurrentHashMap<>();
    private final AtomicInteger total = new AtomicInteger();
    private final AtomicInteger served = new AtomicInteger();

    /** 기동 이후 타르핏으로 응답한 건수(요약 화면용) */
    public int servedCount() {
        return served.get();
    }

    public void respond(HttpServletRequest req, HttpServletResponse res, String ip, String path) throws IOException {
        DetectionSettingsService.Settings s = settingsService.get();
        AtomicInteger mine = perIp.computeIfAbsent(ip, k -> new AtomicInteger());
        int ipCount = mine.incrementAndGet();
        int totalCount = total.incrementAndGet();
        if (!req.isAsyncSupported() || ipCount > MAX_PER_IP || totalCount > MAX_TOTAL) {
            release(ip, mine);
            write(res, 503, ERROR_BODY);
            return;
        }
        // 응답 내용은 요청 스레드에서 미리 만든다(요청 객체는 비동기 완료 후 재사용될 수 있음)
        int status;
        String body;
        String method = req.getMethod();
        if (path.startsWith("/auth/login") || path.startsWith("/auth/mfa")) {
            status = 401;
            body = LOGIN_FAIL_BODY;
        } else if (!"GET".equals(method)) {
            status = 503;
            body = ERROR_BODY;
        } else if (path.contains("/files") || path.contains("/download") || path.contains("/export")
                || path.contains("/preview")) {
            status = 404;
            body = NOT_FOUND_BODY;
        } else {
            status = 200;
            body = honeyDataService.decoyJson(ip, path, req.getQueryString());
        }
        int min = s.tarpitMinSeconds() * 1000;
        int max = Math.max(min, s.tarpitMaxSeconds() * 1000);
        long delay = min + ThreadLocalRandom.current().nextLong(max - min + 1);

        AsyncContext ac = req.startAsync();
        ac.setTimeout(delay + 10_000);
        AtomicBoolean done = new AtomicBoolean();
        Runnable finish = () -> {
            if (!done.compareAndSet(false, true)) return;
            try {
                write((HttpServletResponse) ac.getResponse(), status, body);
                served.incrementAndGet();
            } catch (Exception e) {
                log.debug("[탐지] 타르핏 응답 실패: {}", e.getMessage());
            } finally {
                release(ip, mine);
                try {
                    ac.complete();
                } catch (Exception ignored) {
                    // 클라이언트가 이미 끊은 경우
                }
            }
        };
        ac.addListener(new AsyncListener() {
            @Override public void onComplete(AsyncEvent e) { }
            @Override public void onTimeout(AsyncEvent e) { finish.run(); }
            @Override public void onError(AsyncEvent e) {
                if (done.compareAndSet(false, true)) release(ip, mine);
            }
            @Override public void onStartAsync(AsyncEvent e) { }
        });
        scheduler.schedule(finish, delay, TimeUnit.MILLISECONDS);
    }

    private void release(String ip, AtomicInteger mine) {
        total.decrementAndGet();
        if (mine.decrementAndGet() <= 0) perIp.remove(ip, mine);
    }

    private static void write(HttpServletResponse res, int status, String body) throws IOException {
        res.setStatus(status);
        res.setContentType("application/json;charset=UTF-8");
        res.setHeader("Cache-Control", "no-store");
        byte[] b = body.getBytes(StandardCharsets.UTF_8);
        res.setContentLength(b.length);
        res.getOutputStream().write(b);
        res.flushBuffer();
    }

    @PreDestroy
    void shutdown() {
        scheduler.shutdownNow();
    }
}
