package com.monosun.secportal.common.net;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.Locale;

/**
 * 서버가 사용자·외부 문서가 지정한 URL 로 요청을 보내기 전에 검사한다 (SSRF 방어, CWE-918).
 *
 * <ul>
 *   <li>http / https 만 허용하고, URL 에 계정정보(user:pass@)가 있으면 거부한다.</li>
 *   <li>호스트를 DNS 로 풀어 <b>모든</b> 주소를 검사한다 — 하나라도 막힌 대역이면 거부.</li>
 *   <li>루프백·링크로컬(클라우드 메타데이터 169.254.169.254 포함)·any-local·멀티캐스트는 항상 거부.</li>
 *   <li>사설망(10/8, 172.16/12, 192.168/16, 100.64/10, fc00::/7)은 {@code allowPrivateNetwork} 일 때만 허용
 *       — 폐쇄망에서 사내 사이트를 등록하는 설치를 위한 선택 사항.</li>
 * </ul>
 *
 * <p>리다이렉트는 호출 측에서 자동 추적을 끄고 매 홉마다 이 검사를 다시 거쳐야 한다.
 * 검사와 실제 접속 사이에 DNS 응답이 바뀌는 DNS rebinding 은 이 검사만으로 완전히 막지 못한다.
 */
public final class OutboundUrlGuard {

    private OutboundUrlGuard() {}

    public static class BlockedUrlException extends RuntimeException {
        public BlockedUrlException(String message) { super(message); }
    }

    /** 검사를 통과한 정규화된 URI 를 돌려준다. 통과하지 못하면 {@link BlockedUrlException}. */
    public static URI validate(String url, boolean allowPrivateNetwork) {
        if (url == null || url.isBlank()) throw new BlockedUrlException("주소가 비어 있습니다.");
        URI uri;
        try {
            uri = URI.create(url.trim()).normalize();
        } catch (IllegalArgumentException e) {
            throw new BlockedUrlException("올바른 주소 형식이 아닙니다.");
        }
        String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(Locale.ROOT);
        if (!scheme.equals("http") && !scheme.equals("https")) {
            throw new BlockedUrlException("http/https 주소만 사용할 수 있습니다.");
        }
        if (uri.getRawUserInfo() != null) {
            throw new BlockedUrlException("계정 정보가 포함된 주소는 사용할 수 없습니다.");
        }
        String host = uri.getHost();
        if (host == null || host.isBlank()) throw new BlockedUrlException("호스트가 없는 주소입니다.");

        InetAddress[] addresses;
        try {
            addresses = InetAddress.getAllByName(host);
        } catch (UnknownHostException e) {
            throw new BlockedUrlException("호스트를 찾을 수 없습니다: " + host);
        }
        for (InetAddress addr : addresses) {
            String why = blockedReason(addr, allowPrivateNetwork);
            if (why != null) {
                throw new BlockedUrlException("내부망 주소로의 접속은 허용되지 않습니다 (" + why + ").");
            }
        }
        return uri;
    }

    /** 막아야 하는 주소면 사유, 허용이면 null */
    static String blockedReason(InetAddress addr, boolean allowPrivateNetwork) {
        if (addr instanceof Inet6Address v6) {
            byte[] b = v6.getAddress();
            // IPv4-mapped(::ffff:a.b.c.d) 는 내장된 IPv4 로 판단
            if (isIpv4Mapped(b)) {
                try {
                    return blockedReason(InetAddress.getByAddress(new byte[]{b[12], b[13], b[14], b[15]}),
                            allowPrivateNetwork);
                } catch (UnknownHostException e) {
                    return "잘못된 주소";
                }
            }
        }
        if (addr.isLoopbackAddress()) return "루프백";
        if (addr.isAnyLocalAddress()) return "any-local";
        if (addr.isLinkLocalAddress()) return "링크로컬·메타데이터";
        if (addr.isMulticastAddress()) return "멀티캐스트";

        byte[] b = addr.getAddress();
        if (addr instanceof Inet4Address) {
            int o0 = b[0] & 0xff, o1 = b[1] & 0xff;
            if (o0 == 0) return "0.0.0.0/8";
            if (o0 >= 240) return "예약 대역";
            boolean privateRange = addr.isSiteLocalAddress()             // 10/8, 172.16/12, 192.168/16
                    || (o0 == 100 && o1 >= 64 && o1 <= 127);              // 100.64/10 (CGNAT)
            if (privateRange && !allowPrivateNetwork) return "사설망";
        } else {
            boolean uniqueLocal = (b[0] & 0xfe) == 0xfc;                  // fc00::/7
            if ((uniqueLocal || addr.isSiteLocalAddress()) && !allowPrivateNetwork) return "사설망";
        }
        return null;
    }

    private static boolean isIpv4Mapped(byte[] b) {
        if (b.length != 16) return false;
        for (int i = 0; i < 10; i++) if (b[i] != 0) return false;
        return (b[10] & 0xff) == 0xff && (b[11] & 0xff) == 0xff;
    }
}
