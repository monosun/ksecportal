package com.monosun.secportal.common.net;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.math.BigInteger;
import java.net.InetAddress;
import java.util.List;

/**
 * 요청자 IP 판별과 IP 패턴(정확 일치·CIDR) 매칭.
 *
 * <p>nginx 는 {@code X-Real-IP} 를 접속 소켓 주소($remote_addr)로 <b>덮어써서</b> 전달하므로 클라이언트가 위조할 수 없다.
 * 반면 {@code X-Forwarded-For} 는 nginx 가 클라이언트가 보낸 값 뒤에 덧붙이는($proxy_add_x_forwarded_for) 헤더라
 * 첫 번째 값을 그대로 믿으면 감사로그·공격 탐지에서 출발지 IP 를 위조할 수 있다. 그래서 X-Real-IP 를 우선한다.
 * (백엔드 포트는 호스트에 노출하지 않으므로 nginx 를 거치지 않은 요청은 들어오지 않는다.)
 */
public final class ClientIp {

    private ClientIp() {}

    public static String resolve(HttpServletRequest req) {
        if (req == null) return null;
        String ip = req.getHeader("X-Real-IP");
        if (ip != null && !ip.isBlank()) return truncate(ip.trim());
        return truncate(req.getRemoteAddr());
    }

    /** 현재 스레드에 바인딩된 HTTP 요청의 IP (요청 컨텍스트 밖이면 null). */
    public static String current() {
        try {
            if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attrs) {
                return resolve(attrs.getRequest());
            }
        } catch (Exception ignored) {
            // 요청 컨텍스트가 없는 스케줄러·비동기 스레드
        }
        return null;
    }

    /** 현재 요청의 User-Agent (요청 컨텍스트 밖이면 null). */
    public static String currentUserAgent() {
        try {
            if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attrs) {
                return attrs.getRequest().getHeader("User-Agent");
            }
        } catch (Exception ignored) {
            // 요청 컨텍스트 없음
        }
        return null;
    }

    /** ip 가 패턴 목록(정확한 IP 또는 CIDR, 예: 10.0.0.0/8) 중 하나에 해당하는지. */
    public static boolean matchesAny(String ip, List<String> patterns) {
        if (ip == null || patterns == null) return false;
        for (String p : patterns) {
            if (p == null || p.isBlank()) continue;
            if (matches(ip, p.trim())) return true;
        }
        return false;
    }

    static boolean matches(String ip, String pattern) {
        if (!pattern.contains("/")) return pattern.equalsIgnoreCase(ip);
        try {
            String[] parts = pattern.split("/", 2);
            byte[] net = literalToBytes(parts[0]);
            byte[] addr = literalToBytes(ip);
            if (net == null || addr == null || net.length != addr.length) return false;
            int prefix = Integer.parseInt(parts[1]);
            int bits = net.length * 8;
            if (prefix < 0 || prefix > bits) return false;
            BigInteger mask = prefix == 0 ? BigInteger.ZERO
                    : BigInteger.ONE.shiftLeft(prefix).subtract(BigInteger.ONE).shiftLeft(bits - prefix);
            return new BigInteger(1, net).and(mask).equals(new BigInteger(1, addr).and(mask));
        } catch (Exception e) {
            return false;
        }
    }

    /** IP 리터럴만 바이트로 변환한다 — 호스트명이면 DNS 조회를 하지 않도록 null 을 돌려준다. */
    private static byte[] literalToBytes(String s) {
        if (s == null || s.isBlank()) return null;
        String v = s.trim();
        boolean ipv4 = v.matches("\\d{1,3}(\\.\\d{1,3}){3}");
        boolean ipv6 = v.contains(":") && v.matches("[0-9a-fA-F:.]+");
        if (!ipv4 && !ipv6) return null;
        try {
            return InetAddress.getByName(v).getAddress();
        } catch (Exception e) {
            return null;
        }
    }

    private static String truncate(String s) {
        if (s == null) return null;
        return s.length() > 64 ? s.substring(0, 64) : s;
    }
}
