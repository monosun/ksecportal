package com.monosun.secportal.detection.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 미끼(디코이) 데이터. 차단된 IP 에게 실제 데이터 대신 그럴듯한 가짜 목록·상세를 돌려주고,
 * 그 안의 레코드 ID 는 서버 비밀 키로 서명해 둔다.
 *
 * <p>ID 형식: {@code BASE + r * 1000 + check(r)} — r 은 0 ~ 999,999, check 는 HMAC(secret, r) 의 하위 3자리.
 * 실제 레코드 ID(자동 증가)와 겹치지 않는 대역이고, 상태 없이(재기동·다중 인스턴스 무관) 검증된다.
 * 이 ID 가 다른 IP·계정의 요청 경로나 쿼리에 나타나면 미끼 데이터를 수집해 재사용한 것이다.
 */
@Service
@RequiredArgsConstructor
public class HoneyDataService {

    static final long BASE = 7_100_000_000L;
    private static final long SPAN = 1_000_000L * 1000L;
    private static final Pattern NUMBER = Pattern.compile("(?<![0-9])[78][0-9]{9}(?![0-9])");
    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    private final DetectionSettingsService settingsService;
    private final ObjectMapper objectMapper;

    // ── 서명 ID ─────────────────────────────────────────────

    long honeyId(String secret, long r) {
        return BASE + r * 1000 + check(secret, r);
    }

    public boolean isHoneyId(long id) {
        String secret = settingsService.get().honeyDataSecret();
        if (secret == null || id < BASE || id >= BASE + SPAN) return false;
        long r = (id - BASE) / 1000;
        return (id - BASE) % 1000 == check(secret, r);
    }

    /** 경로·쿼리 문자열에서 서명이 맞는 미끼 ID 를 찾는다(없으면 null) */
    public Long findHoneyId(String... parts) {
        for (String p : parts) {
            if (p == null || p.length() < 10) continue;
            Matcher m = NUMBER.matcher(p);
            while (m.find()) {
                long id = Long.parseLong(m.group());
                if (isHoneyId(id)) return id;
            }
        }
        return null;
    }

    private static long check(String secret, long r) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] h = mac.doFinal(Long.toString(r).getBytes(StandardCharsets.UTF_8));
            return (((h[0] & 0xFF) << 16 | (h[1] & 0xFF) << 8 | (h[2] & 0xFF)) % 1000);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    // ── 미끼 응답 ───────────────────────────────────────────

    /**
     * 요청 경로에 맞는 미끼 응답 본문(ApiResponse 형식 JSON). 같은 IP·경로·페이지에는 항상 같은 내용을 준다 —
     * 다시 요청해 비교해도 가짜인지 드러나지 않도록.
     */
    public String decoyJson(String ip, String path, String query) {
        DetectionSettingsService.Settings s = settingsService.get();
        String secret = s.honeyDataSecret() == null ? "fallback" : s.honeyDataSecret();
        int page = intParam(query, "page", 0);
        Random rnd = new Random(Objects.hash(ip, path, page, secret));
        String noun = noun(path);
        Object data;
        String last = path.substring(path.lastIndexOf('/') + 1);
        if (last.matches("[0-9]+")) {
            data = record(rnd, secret, noun, s.honeyAccounts());
        } else {
            int size = s.decoyPageSize();
            List<Map<String, Object>> content = new ArrayList<>(size);
            for (int i = 0; i < size; i++) content.add(record(rnd, secret, noun, s.honeyAccounts()));
            long total = 3_000L + rnd.nextInt(9_000);
            Map<String, Object> pageObj = new LinkedHashMap<>();
            pageObj.put("content", content);
            pageObj.put("totalElements", total);
            pageObj.put("totalPages", (total + size - 1) / size);
            pageObj.put("size", size);
            pageObj.put("number", page);
            pageObj.put("numberOfElements", size);
            pageObj.put("first", page == 0);
            pageObj.put("last", false);
            pageObj.put("empty", false);
            data = pageObj;
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("success", true);
        body.put("message", "OK");
        body.put("data", data);
        try {
            return objectMapper.writeValueAsString(body);
        } catch (Exception e) {
            return "{\"success\":true,\"message\":\"OK\",\"data\":[]}";
        }
    }

    private Map<String, Object> record(Random rnd, String secret, String noun, List<String> honeyAccounts) {
        String dept = pick(rnd, DEPTS);
        String owner = pick(rnd, SURNAMES) + pick(rnd, GIVEN);
        String system = pick(rnd, SYSTEMS);
        LocalDateTime created = LocalDateTime.of(2024, 1, 1, 9, 0)
                .plusDays(rnd.nextInt(1000)).plusMinutes(rnd.nextInt(600));
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", honeyId(secret, rnd.nextInt(1_000_000)));
        m.put("code", "KSP-"
                + (2024 + rnd.nextInt(3)) + "-" + String.format("%04d", rnd.nextInt(10_000)));
        m.put("name", system + " " + pick(rnd, SUFFIXES));
        m.put("category", pick(rnd, CATEGORIES));
        m.put("status", pick(rnd, STATUSES));
        m.put("department", dept);
        m.put("ownerName", owner);
        m.put("ownerEmail", !honeyAccounts.isEmpty() && rnd.nextInt(8) == 0
                ? honeyAccounts.get(rnd.nextInt(honeyAccounts.size()))
                : "u" + (10_000 + rnd.nextInt(90_000)) + "@ksecportal.local");
        m.put("ipAddress", "10." + (10 + rnd.nextInt(40)) + "." + rnd.nextInt(256) + "." + (2 + rnd.nextInt(250)));
        m.put("hostname", system.toLowerCase(Locale.ROOT).replaceAll("[^a-z]", "") + "-" + pick(rnd, ENVS)
                + "-" + String.format("%02d", 1 + rnd.nextInt(20)));
        m.put("confidentiality", 1 + rnd.nextInt(3));
        m.put("integrity", 1 + rnd.nextInt(3));
        m.put("availability", 1 + rnd.nextInt(3));
        m.put("description", paragraph(rnd, system, dept));
        m.put("remarks", paragraph(rnd, system, dept));
        m.put("dueDate", LocalDate.of(2026, 1, 1).plusDays(rnd.nextInt(500)).toString());
        m.put("createdAt", created.format(TS));
        m.put("updatedAt", created.plusDays(rnd.nextInt(200)).format(TS));
        return m;
    }

    private static String paragraph(Random rnd, String system, String dept) {
        StringBuilder sb = new StringBuilder();
        int n = 3 + rnd.nextInt(4);
        for (int i = 0; i < n; i++) {
            sb.append(pick(rnd, SENTENCES).replace("{s}", system).replace("{d}", dept)).append(' ');
        }
        return sb.toString().trim();
    }

    private static String noun(String path) {
        for (String seg : path.split("/")) {
            if (!seg.isEmpty() && !seg.matches("[0-9]+")) return seg.replaceAll("[^A-Za-z]", "").isEmpty() ? "rec" : seg;
        }
        return "rec";
    }

    private static int intParam(String query, String name, int def) {
        if (query == null) return def;
        for (String kv : query.split("&")) {
            int i = kv.indexOf('=');
            if (i > 0 && kv.substring(0, i).equals(name)) {
                try {
                    return Math.max(0, Math.min(10_000, Integer.parseInt(kv.substring(i + 1))));
                } catch (NumberFormatException e) {
                    return def;
                }
            }
        }
        return def;
    }

    private static String pick(Random rnd, String[] a) {
        return a[rnd.nextInt(a.length)];
    }

    private static final String[] DEPTS = {"정보보호팀", "인프라운영팀", "개발1팀", "개발2팀", "데이터플랫폼팀", "고객지원팀",
            "재무회계팀", "인사총무팀", "마케팅팀", "법무팀", "품질관리팀", "클라우드운영팀"};
    private static final String[] SURNAMES = {"김", "이", "박", "최", "정", "강", "조", "윤", "장", "임", "한", "오", "서", "신"};
    private static final String[] GIVEN = {"민준", "서연", "도윤", "지우", "하준", "서윤", "지호", "하은", "준서", "수아",
            "현우", "지민", "예준", "채원", "건우", "다은"};
    private static final String[] SYSTEMS = {"ERP", "CRM", "Billing", "SSO", "MDM", "DLP", "SIEM", "Groupware", "DataLake",
            "PaymentGW", "HRIS", "VPN", "Bastion", "Kafka", "Redis", "Jenkins", "GitLab", "Nexus", "Vault", "Batch"};
    private static final String[] SUFFIXES = {"운영 서버", "DB 클러스터", "API 게이트웨이", "백업 스토리지", "관리 콘솔",
            "배치 서버", "웹 서버", "연동 모듈", "로그 수집기", "인증 서버"};
    private static final String[] CATEGORIES = {"서버", "데이터베이스", "네트워크", "응용프로그램", "정보보호시스템", "클라우드", "PC", "문서"};
    private static final String[] STATUSES = {"ACTIVE", "ACTIVE", "ACTIVE", "IN_PROGRESS", "PENDING", "REVIEW", "COMPLETED"};
    private static final String[] ENVS = {"prd", "stg", "dev", "dr"};
    private static final String[] SENTENCES = {
            "{s} 시스템은 {d}에서 관리하며 분기별 접근권한 검토 대상이다.",
            "{s} 계정은 SSO 연동 후 로컬 관리자 계정을 비활성화하도록 조치 계획을 수립하였다.",
            "{d} 요청에 따라 {s} 의 로그 보존 기간을 1년으로 상향하고 별도 저장소로 이관하였다.",
            "{s} 운영 서버의 OS 보안 패치는 월 1회 정기 점검 시 적용하며 예외 사항은 승인 후 기록한다.",
            "개인정보가 포함된 {s} 데이터는 컬럼 단위 암호화를 적용하였고 복호화 권한은 최소 인원에게만 부여한다.",
            "{s} 의 외부 연동 구간은 전용선 및 IPSec VPN 으로 구성되어 있으며 방화벽 정책은 {d}에서 승인한다.",
            "{s} 백업은 일 단위 증분, 주 단위 전체 백업으로 수행하고 분기 1회 복구 테스트를 실시한다.",
            "{s} 관리자 접속은 Bastion 경유로만 허용하며 세션 기록을 보관한다.",
            "취약점 점검 결과 {s} 에서 중위험 2건이 확인되어 {d}에서 조치 일정을 협의 중이다.",
            "{s} 서비스 계정의 비밀번호는 Vault 에서 90일 주기로 자동 교체한다.",
            "ISMS-P 2.6.1 항목 증적으로 {s} 의 네트워크 구성도와 접근통제 정책서를 제출하였다.",
            "{d} 담당자 변경에 따라 {s} 의 자산 책임자 및 관리자 정보를 갱신하였다."
    };
}
