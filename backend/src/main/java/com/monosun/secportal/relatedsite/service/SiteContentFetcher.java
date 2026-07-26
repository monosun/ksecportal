package com.monosun.secportal.relatedsite.service;

import com.monosun.secportal.relatedsite.entity.RelatedSite;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 관련 사이트의 내용을 실제로 가져오는 부분.
 *
 * 1) 등록된 피드(RSS/Atom) 주소가 있으면 그 목록을 파싱한다.
 * 2) 없으면 홈페이지 HTML 에서 &lt;link rel="alternate"&gt; 피드를 자동 탐색해 다시 시도한다.
 * 3) 피드를 못 찾으면 og:description / meta description 을 사이트 소개문으로 가져온다.
 *
 * 폐쇄망·방화벽 환경에서는 외부 접속이 막혀 있을 수 있으므로 실패해도 예외를 던지지 않고
 * 상태(ERROR)와 사유만 담아 돌려준다 — 화면에는 링크만 보여주면 되기 때문이다.
 */
@Slf4j
@Component
public class SiteContentFetcher {

    /** 사이트당 보관·표시할 최대 게시물 수 */
    public static final int MAX_ITEMS = 5;

    private static final String USER_AGENT =
            "Mozilla/5.0 (compatible; KSecPortal/1.0; +https://localhost) RelatedSiteBot";

    private static final Pattern XML_ENCODING =
            Pattern.compile("<\\?xml[^>]*encoding=[\"']([^\"']+)[\"']", Pattern.CASE_INSENSITIVE);
    private static final Pattern HTML_CHARSET =
            Pattern.compile("charset=[\"']?([a-zA-Z0-9_\\-]+)", Pattern.CASE_INSENSITIVE);

    private static final List<DateTimeFormatter> DATE_FMTS = List.of(
            DateTimeFormatter.RFC_1123_DATE_TIME,
            DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss Z", Locale.ENGLISH),
            DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.ENGLISH),
            // 연도를 두 자리로 쓰는 RFC-822 피드 (예: CISA — "Thu, 23 Jul 26 12:00:00 +0000")
            DateTimeFormatter.ofPattern("EEE, dd MMM yy HH:mm:ss Z", Locale.ENGLISH),
            DateTimeFormatter.ISO_OFFSET_DATE_TIME,
            DateTimeFormatter.ISO_ZONED_DATE_TIME
    );

    private final RestTemplate restTemplate;

    public SiteContentFetcher(RestTemplateBuilder builder) {
        this.restTemplate = builder
                .setConnectTimeout(Duration.ofSeconds(5))
                .setReadTimeout(Duration.ofSeconds(10))
                .build();
    }

    // ── 결과 모델 ──────────────────────────────────────────────────────────

    @Getter
    @Builder
    public static class FetchedItem {
        private String title;
        private String link;
        private String summary;
        private String publishedText;
        private LocalDateTime publishedAt;
    }

    @Getter
    @Builder
    public static class FetchResult {
        private RelatedSite.FetchStatus status;
        private String message;
        /** 피드가 없을 때 가져온 사이트 소개문 */
        private String summary;
        /** 자동 탐색으로 찾아낸 피드 주소 (등록값이 비어 있을 때만 채워진다) */
        private String discoveredFeedUrl;
        @Builder.Default
        private List<FetchedItem> items = List.of();
    }

    // ── 수집 ──────────────────────────────────────────────────────────────

    public FetchResult fetch(RelatedSite site) {
        String feedUrl = trimToNull(site.getFeedUrl());

        if (feedUrl != null) {
            FetchResult feed = fetchFeed(feedUrl, null);
            // 등록된 피드가 실패하면 홈페이지 쪽으로 한 번 더 시도한다 (주소가 바뀐 경우 대비)
            if (feed.getStatus() == RelatedSite.FetchStatus.FEED) return feed;
            FetchResult page = fetchHomepage(site.getUrl());
            return page.getStatus() == RelatedSite.FetchStatus.ERROR ? feed : page;
        }
        return fetchHomepage(site.getUrl());
    }

    private FetchResult fetchFeed(String feedUrl, String discovered) {
        try {
            String xml = get(feedUrl);
            List<FetchedItem> items = parseFeed(xml);
            if (items.isEmpty()) {
                return FetchResult.builder()
                        .status(RelatedSite.FetchStatus.EMPTY)
                        .message("피드에서 읽어올 게시물이 없습니다.")
                        .discoveredFeedUrl(discovered)
                        .build();
            }
            return FetchResult.builder()
                    .status(RelatedSite.FetchStatus.FEED)
                    .discoveredFeedUrl(discovered)
                    .items(items.size() > MAX_ITEMS ? items.subList(0, MAX_ITEMS) : items)
                    .build();
        } catch (Exception e) {
            return FetchResult.builder()
                    .status(RelatedSite.FetchStatus.ERROR)
                    .message("게시물 목록을 가져오지 못했습니다: " + reason(e))
                    .build();
        }
    }

    private FetchResult fetchHomepage(String url) {
        return fetchHomepage(url, 0);
    }

    /**
     * 홈페이지에서 피드 링크를 찾아 목록을 가져오고, 없으면 소개문만 가져온다.
     * 내용 없이 &lt;meta http-equiv="refresh"&gt; 로 넘기기만 하는 첫 화면(국내 기관 사이트에 흔하다)은
     * 한 번까지 따라간다.
     */
    private FetchResult fetchHomepage(String url, int hop) {
        String html;
        try {
            html = get(url);
        } catch (Exception e) {
            return FetchResult.builder()
                    .status(RelatedSite.FetchStatus.ERROR)
                    .message("사이트에 접속하지 못했습니다: " + reason(e))
                    .build();
        }

        Document doc = Jsoup.parse(html, url);

        String discovered = doc.select("link[rel~=(?i)alternate][type~=(?i)(rss|atom)\\+xml]")
                .stream()
                .map(e -> e.absUrl("href"))
                .filter(h -> h != null && !h.isBlank())
                .findFirst().orElse(null);

        if (discovered != null) {
            FetchResult feed = fetchFeed(discovered, discovered);
            if (feed.getStatus() == RelatedSite.FetchStatus.FEED) return feed;
        }

        String summary = firstNonBlank(
                doc.select("meta[property=og:description]").attr("content"),
                doc.select("meta[name=description]").attr("content"),
                doc.select("meta[property=og:title]").attr("content"),
                doc.title());
        summary = truncate(collapse(summary), 300);

        if ((summary == null || summary.isBlank()) && hop == 0) {
            String next = metaRefreshUrl(doc, url);
            if (next != null && !next.equalsIgnoreCase(url)) return fetchHomepage(next, 1);
        }

        if (summary == null || summary.isBlank()) {
            return FetchResult.builder()
                    .status(RelatedSite.FetchStatus.EMPTY)
                    .message("사이트에서 가져올 소개 내용을 찾지 못했습니다.")
                    .build();
        }
        return FetchResult.builder()
                .status(RelatedSite.FetchStatus.SUMMARY)
                .message("게시물 피드가 없어 사이트 소개 내용만 가져왔습니다.")
                .summary(summary)
                .build();
    }

    /** &lt;meta http-equiv="refresh" content="0;url=/main.html"&gt; 의 이동 주소를 절대주소로 돌려준다 */
    private String metaRefreshUrl(Document doc, String baseUrl) {
        for (org.jsoup.nodes.Element meta : doc.select("meta[http-equiv]")) {
            if (!"refresh".equalsIgnoreCase(meta.attr("http-equiv").trim())) continue;
            Matcher m = Pattern.compile("url\\s*=\\s*['\"]?([^'\";]+)", Pattern.CASE_INSENSITIVE)
                    .matcher(meta.attr("content"));
            if (!m.find()) continue;
            try {
                return java.net.URI.create(baseUrl).resolve(m.group(1).trim()).toString();
            } catch (Exception ignored) {}
        }
        return null;
    }

    // ── HTTP ──────────────────────────────────────────────────────────────

    /** 응답을 바이트로 받아 문서에 선언된 인코딩(EUC-KR 등)까지 감안해 문자열로 만든다 */
    private String get(String url) {
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.USER_AGENT, USER_AGENT);
        headers.set(HttpHeaders.ACCEPT,
                "application/rss+xml, application/atom+xml, application/xml;q=0.9, text/html;q=0.8, */*;q=0.5");

        ResponseEntity<byte[]> res =
                restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(headers), byte[].class);
        byte[] body = res.getBody();
        if (body == null || body.length == 0) throw new IllegalStateException("빈 응답");

        Charset charset = null;
        if (res.getHeaders().getContentType() != null) {
            charset = res.getHeaders().getContentType().getCharset();
        }
        if (charset == null) charset = detectCharset(new String(body, StandardCharsets.ISO_8859_1));
        return new String(body, charset);
    }

    /** 헤더에 charset 이 없을 때 XML 선언·meta 태그에서 인코딩을 찾는다 */
    private Charset detectCharset(String raw) {
        String head = raw.length() > 2048 ? raw.substring(0, 2048) : raw;
        for (Pattern p : List.of(XML_ENCODING, HTML_CHARSET)) {
            Matcher m = p.matcher(head);
            if (m.find()) {
                try {
                    return Charset.forName(m.group(1).trim());
                } catch (Exception ignored) {}
            }
        }
        return StandardCharsets.UTF_8;
    }

    // ── 피드 파싱 ─────────────────────────────────────────────────────────

    /** RSS 2.0(item) 과 Atom(entry) 을 모두 처리한다 */
    private List<FetchedItem> parseFeed(String xml) throws Exception {
        org.w3c.dom.Document doc = secureBuilder().parse(new InputSource(new StringReader(xml.trim())));

        List<FetchedItem> items = new ArrayList<>();
        collect(doc.getElementsByTagName("item"), items, false);
        if (items.isEmpty()) collect(doc.getElementsByTagName("entry"), items, true);
        return items;
    }

    private void collect(NodeList nodes, List<FetchedItem> out, boolean atom) {
        for (int i = 0; i < nodes.getLength() && out.size() < MAX_ITEMS; i++) {
            if (!(nodes.item(i) instanceof Element el)) continue;

            String title = stripHtml(text(el, "title"));
            if (title == null || title.isBlank()) continue;

            String link = atom ? atomLink(el) : text(el, "link");
            String summary = truncate(stripHtml(firstNonBlank(
                    text(el, "description"), text(el, "summary"), text(el, "content"))), 200);
            if (sameText(title, summary)) summary = null;

            // dc:date 는 태그명 그대로 찾아야 한다 (보안뉴스 등 국내 언론 피드가 쓰는 형식)
            String published = firstNonBlank(
                    text(el, "pubDate"), text(el, "dc:date"), text(el, "published"),
                    text(el, "updated"), text(el, "date"));

            out.add(FetchedItem.builder()
                    .title(truncate(title, 490))
                    .link(truncate(link, 990))
                    .summary(summary)
                    .publishedText(truncate(published, 90))
                    .publishedAt(parseDate(published))
                    .build());
        }
    }

    private String atomLink(Element el) {
        NodeList links = el.getElementsByTagName("link");
        for (int i = 0; i < links.getLength(); i++) {
            if (links.item(i) instanceof Element l) {
                String rel = l.getAttribute("rel");
                if (rel == null || rel.isBlank() || "alternate".equalsIgnoreCase(rel)) {
                    String href = l.getAttribute("href");
                    if (href != null && !href.isBlank()) return href;
                }
            }
        }
        return text(el, "id");
    }

    /** XXE 방어 (CWE-611) — DOCTYPE 자체를 막고 외부 엔티티·스키마 접근 경로를 모두 차단한다 */
    private DocumentBuilder secureBuilder() throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();   // sast:ignore 바로 아래에서 DOCTYPE·외부 엔티티를 모두 차단한다
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
        factory.setXIncludeAware(false);
        factory.setExpandEntityReferences(false);
        DocumentBuilder builder = factory.newDocumentBuilder();
        builder.setEntityResolver((publicId, systemId) -> new InputSource(new StringReader("")));
        return builder;
    }

    // ── 문자열 유틸 ───────────────────────────────────────────────────────

    private String text(Element el, String tag) {
        NodeList nl = el.getElementsByTagName(tag);
        if (nl.getLength() == 0) return "";
        return nl.item(0).getTextContent() == null ? "" : nl.item(0).getTextContent().trim();
    }

    private LocalDateTime parseDate(String raw) {
        if (raw == null || raw.isBlank()) return null;
        String normalized = raw.trim().replace(" KST", " +0900").replace(" JST", " +0900");
        for (DateTimeFormatter fmt : DATE_FMTS) {
            try {
                return ZonedDateTime.parse(normalized, fmt)
                        .withZoneSameInstant(ZoneId.of("Asia/Seoul")).toLocalDateTime();
            } catch (Exception ignored) {}
        }
        // "2026-07-27" / "2026-07-27 13:00:00" 처럼 표준 형식이 아닌 경우
        Matcher m = Pattern.compile("(\\d{4})[-./](\\d{1,2})[-./](\\d{1,2})").matcher(normalized);
        if (m.find()) {
            try {
                return LocalDateTime.of(Integer.parseInt(m.group(1)),
                        Integer.parseInt(m.group(2)), Integer.parseInt(m.group(3)), 0, 0);
            } catch (Exception ignored) {}
        }
        return null;
    }

    private String stripHtml(String s) {
        if (s == null || s.isBlank()) return s;
        return collapse(Jsoup.parse(s).text());
    }

    private String collapse(String s) {
        return s == null ? null : s.replaceAll("\\s+", " ").trim();
    }

    private boolean sameText(String a, String b) {
        if (a == null || b == null || b.isBlank()) return false;
        return key(a).equals(key(b));
    }

    private String key(String s) {
        return s.replaceAll("[^\\p{L}\\p{N}]", "").toLowerCase();
    }

    private String truncate(String s, int max) {
        if (s == null || s.length() <= max) return s;
        return s.substring(0, max) + "…";
    }

    private String firstNonBlank(String... values) {
        for (String v : values) {
            if (v != null && !v.isBlank()) return v.trim();
        }
        return null;
    }

    private String trimToNull(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }

    private String reason(Exception e) {
        String msg = e.getMessage();
        if (msg == null || msg.isBlank()) return e.getClass().getSimpleName();
        return msg.length() > 200 ? msg.substring(0, 200) : msg;
    }
}
