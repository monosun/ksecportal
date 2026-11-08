package com.monosun.secportal.detection.service;

import com.monosun.secportal.detection.dto.DetectionDto;
import com.monosun.secportal.detection.entity.DetectionEvent.Category;
import com.monosun.secportal.detection.entity.DetectionEvent.Severity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Pattern;

/**
 * 행위 기반 자동화·AI 공격 탐지. 1분마다 IP 별 최근 요청(윈도)을 점수화한다.
 *
 * <p>"AI 가 보냈다"는 사실은 판별할 수 없으므로 자동화 도구·AI 에이전트에서 나타나는 행위 특징을 본다:
 * 사람이 낼 수 없는 속도, 기계적으로 균일한 요청 간격, 대량 경로 열거(404·403), 오류 직후의 즉각적인 재시도,
 * 속도 제한 초과, 도구·AI 크롤러 User-Agent. 각 신호의 점수 합이 임계값을 넘으면 이벤트를 만든다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BehaviorAnalyzer {

    /** nginx 는 속도 제한 초과 요청 중 약 10%만 백엔드에 보고한다(nginx-http.conf split_clients) */
    static final int RATE_LIMIT_SAMPLE_FACTOR = 10;

    static final Pattern AI_UA = Pattern.compile(
            "GPTBot|ChatGPT-User|OAI-SearchBot|ClaudeBot|Claude-User|Claude-Web|anthropic-ai|PerplexityBot|"
                    + "Perplexity-User|Google-Extended|Bytespider|CCBot|Amazonbot|Applebot-Extended|cohere-ai|"
                    + "Diffbot|YouBot|meta-externalagent|MistralAI-User|DuckAssistBot",
            Pattern.CASE_INSENSITIVE);
    static final Pattern TOOL_UA = Pattern.compile(
            "curl|wget|python-requests|python-urllib|aiohttp|httpx|Go-http-client|okhttp|Java/|Apache-HttpClient|"
                    + "libwww-perl|node-fetch|axios/|undici|Scrapy|HeadlessChrome|PhantomJS|Playwright|Puppeteer|"
                    + "sqlmap|nikto|nmap|masscan|zgrab|nuclei|ffuf|gobuster|dirbuster|feroxbuster|wfuzz|Burp|OWASP ZAP|Hydra",
            Pattern.CASE_INSENSITIVE);

    private final RequestTracker tracker;
    private final DetectionSettingsService settingsService;
    private final DetectionSignals signals;
    private final IpBlockService ipBlockService;

    private volatile List<DetectionDto.IpScore> lastScores = List.of();
    private volatile LocalDateTime lastRunAt;

    public List<DetectionDto.IpScore> lastScores() {
        return lastScores;
    }

    public LocalDateTime lastRunAt() {
        return lastRunAt;
    }

    public int trackedIps() {
        return tracker.trackedIpCount();
    }

    @Scheduled(fixedDelay = 60_000, initialDelay = 60_000)
    public void analyze() {
        DetectionSettingsService.Settings s = settingsService.get();
        long windowMs = s.windowMinutes() * 60_000L;
        try {
            if (!s.enabled()) {
                lastScores = List.of();
                return;
            }
            List<DetectionDto.IpScore> scores = new ArrayList<>();
            for (RequestTracker.IpSnapshot snap : tracker.snapshot(windowMs)) {
                DetectionDto.IpScore score = score(snap, windowMs);
                if (score.getScore() <= 0) continue;
                score.setBlocked(ipBlockService.isBlocked(snap.ip()));
                score.setWhitelisted(ipBlockService.isWhitelisted(snap.ip()));
                scores.add(score);
                if (score.getScore() >= s.scoreThreshold()) raise(score, s);
            }
            scores.sort(Comparator.comparingInt(DetectionDto.IpScore::getScore).reversed());
            lastScores = scores.size() > 100 ? List.copyOf(scores.subList(0, 100)) : List.copyOf(scores);
            lastRunAt = LocalDateTime.now();
        } catch (Exception e) {
            log.warn("[탐지] 행위 분석 실패: {}", e.getMessage());
        } finally {
            tracker.evict(Math.max(windowMs, s.loginWindowMinutes() * 60_000L) * 2);
        }
    }

    private void raise(DetectionDto.IpScore sc, DetectionSettingsService.Settings s) {
        boolean aiUa = sc.getUserAgent() != null && AI_UA.matcher(sc.getUserAgent()).find();
        Severity sev = sc.getScore() >= s.scoreThreshold() * 2 || aiUa ? Severity.HIGH : Severity.MEDIUM;
        StringBuilder detail = new StringBuilder();
        detail.append("최근 ").append(s.windowMinutes()).append("분 행위 점수 ").append(sc.getScore())
                .append("점 (임계값 ").append(s.scoreThreshold()).append("점)\n\n탐지 근거:\n");
        sc.getReasons().forEach(r -> detail.append(" - ").append(r).append('\n'));
        detail.append("\n요청 ").append(sc.getRequests()).append("건 · 서로 다른 API ").append(sc.getDistinctPaths())
                .append("개 · 404 ").append(sc.getNotFound()).append("건 · 401/403 ").append(sc.getDenied())
                .append("건 · 속도제한 초과(추정) ").append(sc.getRateLimited()).append("건");
        if (!sc.getSamplePaths().isEmpty()) {
            detail.append("\n\n요청 경로 예시:\n");
            sc.getSamplePaths().forEach(p -> detail.append(" - ").append(p).append('\n'));
        }
        signals.fire(DetectionEventService.Signal.builder()
                .category(Category.BEHAVIOR).severity(sev).ip(sc.getIp())
                .title((aiUa ? "AI 크롤러·에이전트 " : "자동화 도구 의심 ") + "행위 — 점수 " + sc.getScore())
                .detail(detail.toString())
                .userAgent(sc.getUserAgent()).user(sc.getLastUser()).score(sc.getScore())
                .build());
    }

    DetectionDto.IpScore score(RequestTracker.IpSnapshot snap, long windowMs) {
        List<RequestTracker.Hit> hits = new ArrayList<>(snap.hits());
        hits.sort(Comparator.comparingLong(RequestTracker.Hit::ts));
        int n = hits.size();
        int notFound = 0, denied = 0, serverErr = 0;
        Set<String> distinct = new LinkedHashSet<>();
        Set<String> notFoundPaths = new LinkedHashSet<>();
        for (RequestTracker.Hit h : hits) {
            if (h.status() == 404) { notFound++; notFoundPaths.add(h.method() + " " + h.path()); }
            if (h.status() == 401 || h.status() == 403) denied++;
            if (h.status() >= 500) serverErr++;
            distinct.add(h.method() + " " + h.path());
        }
        List<String> reasons = new ArrayList<>();
        int score = 0;

        if (notFound >= 10 && notFound >= n * 0.2) {
            int p = Math.min(35, 10 + notFound / 2);
            score += p;
            reasons.add("존재하지 않는 경로 요청(404) " + notFound + "건 — 경로 열거 [+" + p + "]");
        }
        if (denied >= 5) {
            int p = Math.min(30, 5 + denied * 2);
            score += p;
            reasons.add("인증·권한 거부(401/403) " + denied + "건 — 권한 우회 시도 [+" + p + "]");
        }
        if (distinct.size() >= 80) {
            score += 25;
            reasons.add("서로 다른 API " + distinct.size() + "개 호출 — 대량 열거 [+25]");
        } else if (distinct.size() >= 40) {
            score += 15;
            reasons.add("서로 다른 API " + distinct.size() + "개 호출 [+15]");
        }

        // 요청 간격 균일성: 브라우저의 동시 요청(100ms 미만 간격)은 제외하고 본다. 한두 API 만 주기 호출하는 폴링은 제외.
        List<Long> gaps = new ArrayList<>();
        for (int i = 1; i < n; i++) {
            long g = hits.get(i).ts() - hits.get(i - 1).ts();
            if (g >= 100) gaps.add(g);
        }
        if (gaps.size() >= 20 && distinct.size() >= 5) {
            double mean = gaps.stream().mapToLong(Long::longValue).average().orElse(0);
            double var = gaps.stream().mapToDouble(g -> (g - mean) * (g - mean)).average().orElse(0);
            double cv = mean > 0 ? Math.sqrt(var) / mean : 1;
            if (cv < 0.2) {
                score += 25;
                reasons.add(String.format("요청 간격이 기계적으로 균일(변동계수 %.2f, 평균 %.1f초) [+25]", cv, mean / 1000));
            } else if (cv < 0.35) {
                score += 12;
                reasons.add(String.format("요청 간격이 매우 일정(변동계수 %.2f) [+12]", cv));
            }
        }

        double rps = n / (windowMs / 1000.0);
        if (rps >= 3 && distinct.size() >= 20) {
            score += 15;
            reasons.add(String.format("윈도 전체에 걸쳐 초당 %.1f건 지속 요청 [+15]", rps));
        }

        int fastRetry = 0;
        for (int i = 1; i < n; i++) {
            RequestTracker.Hit prev = hits.get(i - 1);
            RequestTracker.Hit cur = hits.get(i);
            if (prev.status() >= 400 && prev.status() < 500 && cur.ts() - prev.ts() <= 1500
                    && cur.ts() - prev.ts() >= 100) fastRetry++;
        }
        if (fastRetry >= 30) {
            score += 25;
            reasons.add("오류 응답 직후 1.5초 안에 재요청 " + fastRetry + "회 — 응답을 읽고 즉시 바꿔 시도 [+25]");
        } else if (fastRetry >= 10) {
            score += 15;
            reasons.add("오류 응답 직후 1.5초 안에 재요청 " + fastRetry + "회 [+15]");
        }

        int rlEst = snap.rateLimitedReports() * RATE_LIMIT_SAMPLE_FACTOR;
        if (rlEst >= 200) {
            score += 40;
            reasons.add("속도 제한 초과 약 " + rlEst + "건 [+40]");
        } else if (rlEst >= 50) {
            score += 25;
            reasons.add("속도 제한 초과 약 " + rlEst + "건 [+25]");
        } else if (rlEst > 0) {
            score += 10;
            reasons.add("속도 제한 초과 약 " + rlEst + "건 [+10]");
        }

        if (serverErr >= 10) {
            score += 10;
            reasons.add("서버 오류(5xx) " + serverErr + "건 유발 — 비정상 입력(퍼징) 의심 [+10]");
        }

        String ua = snap.lastUserAgent();
        if (score > 0 || n >= 10) {
            if (ua == null || ua.isBlank()) {
                score += 15;
                reasons.add("User-Agent 없음 [+15]");
            } else if (AI_UA.matcher(ua).find()) {
                score += 25;
                reasons.add("AI 크롤러·에이전트 User-Agent [+25]");
            } else if (TOOL_UA.matcher(ua).find()) {
                score += 15;
                reasons.add("스크립트·공격 도구 User-Agent [+15]");
            }
        }

        DetectionDto.IpScore out = new DetectionDto.IpScore();
        out.setIp(snap.ip());
        out.setScore(score);
        out.setReasons(reasons);
        out.setRequests(n);
        out.setDistinctPaths(distinct.size());
        out.setNotFound(notFound);
        out.setDenied(denied);
        out.setRateLimited(rlEst);
        out.setUserAgent(ua);
        out.setLastUser(snap.lastUser());
        List<String> samples = new ArrayList<>(notFoundPaths.isEmpty() ? distinct : notFoundPaths);
        out.setSamplePaths(samples.size() > 15 ? samples.subList(0, 15) : samples);
        return out;
    }
}
