package com.monosun.secportal.detection.service;

import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 * IP 별 최근 요청을 메모리에 보관하는 슬라이딩 윈도.
 *
 * <p>모든 API 요청을 DB 에 쓰지 않고(부하·용량) 최근 몇 분치만 메모리에 두고 1분마다 행위 분석에 쓴다.
 * 메모리 상한: IP 당 {@value #MAX_HITS_PER_IP}건, 전체 {@value #MAX_IPS}개 IP. 재기동하면 비워진다.
 */
@Component
public class RequestTracker {

    static final int MAX_HITS_PER_IP = 2000;
    static final int MAX_IPS = 20_000;
    private static final int MAX_LOGIN_FAILS = 500;

    private static final Pattern NUMERIC = Pattern.compile("/\\d+(?=/|$)");
    private static final Pattern HEXISH = Pattern.compile("/[0-9a-fA-F-]{16,}(?=/|$)");

    public record Hit(long ts, String method, String path, int status) {}

    public record LoginFail(long ts, String email) {}

    /** 분석용 복사본 */
    public record IpSnapshot(String ip, List<Hit> hits, int rateLimitedReports,
                             String lastUser, String lastUserAgent) {}

    static final class IpState {
        final ArrayDeque<Hit> hits = new ArrayDeque<>();
        final ArrayDeque<Long> rateLimited = new ArrayDeque<>();
        final ArrayDeque<LoginFail> loginFails = new ArrayDeque<>();
        volatile long lastSeen;
        volatile String lastUser;
        volatile String lastUserAgent;
        volatile long loginBlockedUntil;
    }

    private final ConcurrentHashMap<String, IpState> states = new ConcurrentHashMap<>();

    /** /assets/123/files/9 → /assets/{id}/files/{id} — 같은 기능 호출을 하나의 경로로 센다 */
    public static String normalize(String path) {
        if (path == null) return "";
        String p = path.length() > 300 ? path.substring(0, 300) : path;
        p = HEXISH.matcher(p).replaceAll("/{id}");
        return NUMERIC.matcher(p).replaceAll("/{id}");
    }

    public void record(String ip, String method, String path, int status, String user, String userAgent) {
        if (ip == null) return;
        IpState s = state(ip);
        long now = System.currentTimeMillis();
        synchronized (s) {
            s.hits.addLast(new Hit(now, method, normalize(path), status));
            while (s.hits.size() > MAX_HITS_PER_IP) s.hits.pollFirst();
        }
        s.lastSeen = now;
        if (user != null) s.lastUser = user;
        if (userAgent != null) s.lastUserAgent = userAgent;
    }

    /** nginx 속도 제한(429)에 걸린 요청 — nginx 가 일부(표본)만 보고하므로 건수는 표본 기준이다 */
    public void recordRateLimited(String ip, String userAgent) {
        if (ip == null) return;
        IpState s = state(ip);
        long now = System.currentTimeMillis();
        synchronized (s) {
            s.rateLimited.addLast(now);
            while (s.rateLimited.size() > MAX_HITS_PER_IP) s.rateLimited.pollFirst();
        }
        s.lastSeen = now;
        if (userAgent != null) s.lastUserAgent = userAgent;
    }

    /** 로그인 실패를 기록하고 윈도 안의 [실패 건수, 서로 다른 계정 수] 를 돌려준다. */
    public int[] recordLoginFailure(String ip, String email, long windowMs) {
        if (ip == null) return new int[]{0, 0};
        IpState s = state(ip);
        long now = System.currentTimeMillis();
        synchronized (s) {
            s.loginFails.addLast(new LoginFail(now, email == null ? "" : email.toLowerCase(Locale.ROOT)));
            while (s.loginFails.size() > MAX_LOGIN_FAILS) s.loginFails.pollFirst();
            while (!s.loginFails.isEmpty() && s.loginFails.peekFirst().ts() < now - windowMs) s.loginFails.pollFirst();
            Set<String> distinct = new HashSet<>();
            s.loginFails.forEach(f -> distinct.add(f.email()));
            s.lastSeen = now;
            return new int[]{s.loginFails.size(), distinct.size()};
        }
    }

    public List<String> recentLoginAccounts(String ip) {
        IpState s = states.get(ip);
        if (s == null) return List.of();
        synchronized (s) {
            return s.loginFails.stream().map(LoginFail::email).distinct().limit(20).toList();
        }
    }

    public void blockLogin(String ip, long untilMs) {
        if (ip == null) return;
        state(ip).loginBlockedUntil = untilMs;
    }

    /** IP 로그인 일시 차단 남은 시간(ms). 차단 중이 아니면 0. */
    public long loginBlockedRemainingMs(String ip) {
        if (ip == null) return 0;
        IpState s = states.get(ip);
        if (s == null) return 0;
        long remain = s.loginBlockedUntil - System.currentTimeMillis();
        return Math.max(0, remain);
    }

    public void clearLoginBlock(String ip) {
        IpState s = states.get(ip);
        if (s != null) {
            s.loginBlockedUntil = 0;
            synchronized (s) {
                s.loginFails.clear();
            }
        }
    }

    public List<IpSnapshot> snapshot(long windowMs) {
        long from = System.currentTimeMillis() - windowMs;
        List<IpSnapshot> out = new ArrayList<>();
        states.forEach((ip, s) -> {
            if (s.lastSeen < from) return;
            List<Hit> hits;
            int rl;
            synchronized (s) {
                hits = s.hits.stream().filter(h -> h.ts() >= from).toList();
                rl = (int) s.rateLimited.stream().filter(t -> t >= from).count();
            }
            if (!hits.isEmpty() || rl > 0) out.add(new IpSnapshot(ip, hits, rl, s.lastUser, s.lastUserAgent));
        });
        return out;
    }

    /** 오래된 IP 정리 + IP 수 상한 유지 */
    public void evict(long keepMs) {
        long now = System.currentTimeMillis();
        long from = now - keepMs;
        states.entrySet().removeIf(e -> e.getValue().lastSeen < from && e.getValue().loginBlockedUntil < now);
        if (states.size() > MAX_IPS) {
            states.entrySet().stream()
                    .sorted(Comparator.comparingLong(e -> e.getValue().lastSeen))
                    .limit(states.size() - MAX_IPS)
                    .map(Map.Entry::getKey)
                    .toList()
                    .forEach(states::remove);
        }
    }

    public int trackedIpCount() {
        return states.size();
    }

    private IpState state(String ip) {
        return states.computeIfAbsent(ip, k -> new IpState());
    }
}
