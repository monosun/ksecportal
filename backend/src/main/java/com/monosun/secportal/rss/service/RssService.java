package com.monosun.secportal.rss.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.monosun.secportal.rss.dto.RssItemDto;
import com.monosun.secportal.rss.dto.RssResultDto;
import com.monosun.secportal.setting.service.AppSettingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.net.ssl.SSLException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

@Slf4j
@Service
public class RssService {

    private static final String DEFAULT_FEEDS_JSON =
            "[{\"url\":\"https://knvd.krcert.or.kr/rss/security/info\",\"category\":\"vuln\",\"label\":\"취약점 정보\"}," +
            "{\"url\":\"https://knvd.krcert.or.kr/rss/security/notice\",\"category\":\"notice\",\"label\":\"보안공지\"}]";

    private static final List<DateTimeFormatter> DATE_FMTS = List.of(
            DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss Z", Locale.ENGLISH),
            DateTimeFormatter.RFC_1123_DATE_TIME,
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.ENGLISH)
    );

    private final RestTemplate restTemplate;
    private final AppSettingService settingService;
    private final ObjectMapper objectMapper;

    public RssService(RestTemplateBuilder builder, AppSettingService settingService, ObjectMapper objectMapper) {
        this.restTemplate = builder
                .setConnectTimeout(Duration.ofSeconds(10))
                .setReadTimeout(Duration.ofSeconds(15))
                .build();
        this.settingService = settingService;
        this.objectMapper = objectMapper;
    }

    public RssResultDto fetchKrcert() {
        return fetchKrcert(null);
    }

    /** daysOverride 가 있으면 그 기간(1~366일)으로, 없으면 설정관리의 rss.days 로 필터링 */
    public RssResultDto fetchKrcert(Integer daysOverride) {
        int days = (daysOverride != null)
                ? Math.max(1, Math.min(366, daysOverride))
                : resolveDays();
        LocalDate cutoff = LocalDate.now().minusDays(days);
        List<Map<String, String>> feeds = resolveFeeds();

        List<RssItemDto> result = new ArrayList<>();
        List<RssResultDto.FeedError> errors = new ArrayList<>();
        for (Map<String, String> feed : feeds) {
            String url = feed.get("url");
            if (url == null || url.isBlank()) continue;
            String category = feed.getOrDefault("category", "other");
            String label = feed.getOrDefault("label", category);
            try {
                result.addAll(fetchFeed(url, category, cutoff));
            } catch (Exception e) {
                // 피드 하나가 죽어도 나머지는 보여준다. 다만 조용히 빈 목록을 돌려주면
                // 화면에 "게시물이 없습니다" 로 보이므로 실패 사유를 함께 올린다.
                log.warn("RSS 조회 실패 ({} / {}): {}", label, url, e.toString());
                errors.add(RssResultDto.FeedError.builder()
                        .label(label)
                        .category(category)
                        .url(url)
                        .message(describeFailure(url, e))
                        .build());
            }
        }
        result.sort(Comparator.comparing(RssItemDto::getPubDate).reversed());
        return RssResultDto.builder().items(result).errors(errors).build();
    }

    private int resolveDays() {
        try {
            String val = settingService.getValue("rss.days");
            if (val != null && !val.isBlank()) {
                int d = Integer.parseInt(val.trim());
                return Math.max(1, Math.min(90, d));
            }
        } catch (Exception ignored) {}
        return 7;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, String>> resolveFeeds() {
        try {
            String json = settingService.getValue("rss.feeds");
            if (json != null && !json.isBlank()) {
                return objectMapper.readValue(json, new TypeReference<List<Map<String, String>>>() {});
            }
        } catch (Exception ignored) {}
        try {
            return objectMapper.readValue(DEFAULT_FEEDS_JSON, new TypeReference<List<Map<String, String>>>() {});
        } catch (Exception e) {
            return List.of();
        }
    }

    /** 피드 하나를 읽어 항목으로 바꾼다. 접속·형식 오류는 사유를 알 수 있게 호출부로 던진다. */
    private List<RssItemDto> fetchFeed(String url, String category, LocalDate cutoff) {
        String xml = restTemplate.getForObject(url, String.class);
        if (xml == null || xml.isBlank()) throw new IllegalStateException("응답이 비어 있습니다");
        return parseRss(xml, category, cutoff);
    }

    /** 실패 원인을 사용자가 읽고 조치할 수 있는 한 문장으로 바꾼다 */
    private String describeFailure(String url, Exception e) {
        String host = hostOf(url);
        if (e instanceof HttpStatusCodeException he) {
            return host + " 응답 오류 " + he.getStatusCode().value() + " — 피드 주소를 확인하세요";
        }
        Throwable cause = (e instanceof ResourceAccessException && e.getCause() != null) ? e.getCause() : e;
        if (cause instanceof UnknownHostException) return host + " 주소를 찾을 수 없습니다 (DNS 조회 실패)";
        if (cause instanceof SocketTimeoutException) return host + " 응답 시간이 초과되었습니다";
        if (cause instanceof ConnectException) return host + " 에 접속하지 못했습니다 (연결 거부·차단)";
        if (cause instanceof SSLException) return host + " SSL 연결에 실패했습니다";
        if (e instanceof IllegalStateException) return host + " — " + e.getMessage();
        String msg = cause.getMessage();
        return host + " 조회 실패" + (msg == null || msg.isBlank() ? "" : ": " + msg);
    }

    private String hostOf(String url) {
        try {
            String host = java.net.URI.create(url).getHost();
            return (host != null && !host.isBlank()) ? host : url;
        } catch (Exception e) {
            return url;
        }
    }


    private List<RssItemDto> parseRss(String xml, String category, LocalDate cutoff) {
        List<RssItemDto> items = new ArrayList<>();
        try {
            // XXE 방어 (CWE-611) — DOCTYPE 자체를 막고, 외부 엔티티·스키마 접근 경로를 모두 차단한다.
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();   // sast:ignore 바로 아래에서 DOCTYPE·외부 엔티티를 모두 차단한다
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
            factory.setFeature(javax.xml.XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setAttribute(javax.xml.XMLConstants.ACCESS_EXTERNAL_DTD, "");
            factory.setAttribute(javax.xml.XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
            factory.setXIncludeAware(false);
            factory.setExpandEntityReferences(false);
            DocumentBuilder builder = factory.newDocumentBuilder();
            // 파서가 외부 리소스를 절대 가져오지 않도록 EntityResolver 도 무력화한다.
            builder.setEntityResolver((publicId, systemId) -> new InputSource(new StringReader("")));
            Document doc = builder.parse(new InputSource(new StringReader(xml)));

            NodeList nodeList = doc.getElementsByTagName("item");
            for (int i = 0; i < nodeList.getLength(); i++) {
                Element item = (Element) nodeList.item(i);
                String pubDate = text(item, "pubDate");
                if (cutoff != null && !isWithinCutoff(pubDate, cutoff)) continue;

                String title = stripHtml(text(item, "title"));
                // 뉴스 피드의 요약에는 <a href=…> 같은 마크업이 그대로 들어있어 화면에 노출된다.
                // 태그를 걷어내고 본문만 남긴 뒤 길이를 자른다.
                String description = truncate(stripHtml(text(item, "description")), 200);
                // 요약이 제목과 같은 피드(개인정보 유출 뉴스 등)는 화면에 같은 문장이 두 줄로 찍히므로 비운다.
                if (sameAsTitle(title, description)) description = null;

                items.add(RssItemDto.builder()
                        .title(title)
                        .link(text(item, "link"))
                        .description(description)
                        .pubDate(pubDate)
                        .category(category)
                        .build());
            }
        } catch (Exception e) {
            // XML 이 깨졌거나 RSS 가 아닌 응답(오류 페이지 등)을 받은 경우 —
            // 빈 목록을 돌려주면 화면에 "게시물 없음" 으로 보이므로 사유를 호출부로 올린다.
            throw new IllegalStateException("RSS 형식을 해석하지 못했습니다", e);
        }
        return items;
    }

    private String text(Element el, String tag) {
        NodeList nl = el.getElementsByTagName(tag);
        if (nl.getLength() == 0) return "";
        return nl.item(0).getTextContent().trim();
    }

    private boolean isWithinCutoff(String pubDate, LocalDate cutoff) {
        if (pubDate == null || pubDate.isBlank()) return false;
        String normalized = pubDate
                .replace(" KST", " +0900")
                .replace(" JST", " +0900");
        for (DateTimeFormatter fmt : DATE_FMTS) {
            try {
                ZonedDateTime zdt = ZonedDateTime.parse(normalized, fmt);
                return !zdt.toLocalDate().isBefore(cutoff);
            } catch (DateTimeParseException ignored) {}
        }
        return false;
    }

    private String truncate(String s, int max) {
        if (s == null || s.length() <= max) return s;
        return s.substring(0, max) + "…";
    }

    /**
     * RSS 요약에 섞여 오는 HTML 을 걷어내고 사람이 읽는 문장만 남긴다.
     * (뉴스 피드 description 은 대부분 &lt;a href&gt;·&lt;img&gt;·&lt;font&gt; 마크업을 그대로 담고 있다)
     */
    private String stripHtml(String s) {
        if (s == null || s.isBlank()) return s;
        // 이스케이프된 마크업(&lt;a href=…&gt;)도 걷어내려면 먼저 엔티티를 되돌린 뒤 태그를 제거한다.
        String out = unescape(s)
                .replaceAll("(?is)<(script|style)[^>]*>.*?</\\1>", " ")  // 스크립트·스타일은 내용까지 제거
                .replaceAll("(?is)<br\\s*/?>|</p>|</div>|</li>", " ")     // 줄바꿈 태그는 공백으로
                .replaceAll("(?s)<[^>]+>", "");                            // 나머지 태그 제거
        return unescape(out).replaceAll("\\s+", " ").trim();
    }

    /**
     * 요약이 사실상 제목과 같은지 판단한다.
     * 구글 뉴스 RSS 는 요약이 "제목 + 언론사"(제목의 ' - ' 구분자만 빠진 형태)라서
     * 글자·숫자만 남긴 키로 비교해야 같은 문장임을 알아낼 수 있다.
     * 제목 뒤에 실제 본문 요약이 더 붙은 경우는 정보가 있으므로 남긴다.
     */
    private boolean sameAsTitle(String title, String description) {
        if (title == null || description == null || description.isBlank()) return false;
        String t = compactKey(title);
        String d = compactKey(description);
        if (t.isEmpty() || d.isEmpty()) return false;
        if (t.equals(d)) return true;
        // 한쪽이 다른 쪽을 자른 형태(말줄임 등)도 같은 문장으로 본다
        String shorter = t.length() <= d.length() ? t : d;
        String longer = t.length() <= d.length() ? d : t;
        return longer.startsWith(shorter) && longer.length() - shorter.length() <= 3;
    }

    /** 비교용 키 — 공백·문장부호를 모두 걷어내고 글자와 숫자만 남긴다 */
    private String compactKey(String s) {
        return s.replaceAll("[^\\p{L}\\p{N}]", "").toLowerCase();
    }

    /** 자주 쓰이는 HTML 엔티티만 되돌린다 */
    private String unescape(String s) {
        return s.replace("&nbsp;", " ")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&quot;", "\"")
                .replace("&apos;", "'")
                .replace("&#39;", "'")
                .replace("&#34;", "\"")
                .replace("&amp;", "&");
    }
}
