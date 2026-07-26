package com.monosun.secportal.relatedsite;

import java.util.List;

/**
 * 관련 사이트 기본 목록 — 보안·개인정보 실무에서 자주 찾는 국내외 기관·정보 사이트.
 *
 * feedUrl 을 비워두면 홈페이지에서 RSS/Atom 링크를 자동 탐색하고,
 * 그래도 없으면 사이트 소개문(og 메타)만 가져온다.
 */
public final class RelatedSiteDefaults {

    private RelatedSiteDefaults() {}

    public record Row(String name, String url, String feedUrl, String category,
                      String description, int sortOrder) {}

    public static final List<Row> SITES = List.of(
            // ── 침해사고·취약점 정보 ──────────────────────────────────────
            new Row("KISA 인터넷 보호나라&KrCERT", "https://www.boho.or.kr",
                    null, "침해사고·취약점",
                    "보안공지·침해사고 예방 정보와 신고 창구를 제공하는 KISA 종합 포털", 10),
            new Row("KISA 취약점 정보포털(KNVD) 보안공지", "https://knvd.krcert.or.kr",
                    "https://knvd.krcert.or.kr/rss/security/notice", "침해사고·취약점",
                    "국내 보안공지 — 제품 취약점 패치·긴급 대응 권고", 20),
            new Row("KISA 취약점 정보포털(KNVD) 취약점 정보", "https://knvd.krcert.or.kr",
                    "https://knvd.krcert.or.kr/rss/security/info", "침해사고·취약점",
                    "국내외 신규 취약점(CVE) 상세 정보", 30),
            new Row("미국 CISA 보안 권고", "https://www.cisa.gov/news-events/cybersecurity-advisories",
                    "https://www.cisa.gov/cybersecurity-advisories/all.xml", "침해사고·취약점",
                    "미국 CISA 의 취약점·위협 권고 (KEV 포함)", 40),
            new Row("NVD (미국 국가 취약점 DB)", "https://nvd.nist.gov",
                    null, "침해사고·취약점",
                    "CVE 취약점 상세·CVSS 점수 조회", 50),

            // ── 유관기관·정책 ────────────────────────────────────────────
            new Row("한국인터넷진흥원(KISA)", "https://www.kisa.or.kr",
                    null, "유관기관",
                    "정보보호·개인정보보호 정책, ISMS-P 인증제도 운영기관", 60),
            new Row("개인정보보호위원회", "https://www.pipc.go.kr",
                    null, "유관기관",
                    "개인정보보호법 소관 부처 — 고시·해설서·처분 사례 공개", 70),
            new Row("개인정보 포털", "https://www.privacy.go.kr",
                    null, "유관기관",
                    "개인정보 처리방침·자기결정권 행사 등 국민 대상 개인정보 종합 포털", 80),
            new Row("국가정보원 국가사이버안보센터", "https://www.ncsc.go.kr",
                    null, "유관기관",
                    "국가 사이버 위협 정보·보안 가이드라인", 90),
            new Row("금융보안원", "https://www.fsec.or.kr",
                    null, "유관기관",
                    "금융권 보안 가이드·취약점 정보 (금융 분야 참고)", 100),

            // ── 법령·인증 ────────────────────────────────────────────────
            new Row("국가법령정보센터", "https://www.law.go.kr",
                    null, "법령·인증",
                    "개인정보보호법·정보통신망법 등 법령·시행령·행정규칙 원문", 110),
            new Row("개인정보보호 종합포털(고시·해설서)", "https://www.pipc.go.kr/np/cop/bbs/selectBoardList.do?bbsId=BS074&mCode=D010030000",
                    null, "법령·인증",
                    "개인정보보호위원회 법령·지침·고시 자료실", 130),

            // ── 보안 뉴스·동향 ───────────────────────────────────────────
            new Row("보안뉴스", "https://www.boannews.com",
                    "https://www.boannews.com/media/news_rss.xml", "보안 동향",
                    "국내 보안 전문 매체 — 사고·정책·기술 동향", 140),
            new Row("데일리시큐", "https://www.dailysecu.com",
                    "https://www.dailysecu.com/rss/allArticle.xml", "보안 동향",
                    "국내 보안·개인정보 전문 매체", 150),
            new Row("OWASP", "https://owasp.org",
                    null, "보안 동향",
                    "웹 애플리케이션 보안 표준 — OWASP Top 10·개발 보안 가이드", 160)
    );
}
