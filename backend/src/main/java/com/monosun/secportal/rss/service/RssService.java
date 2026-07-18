package com.monosun.secportal.rss.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.monosun.secportal.rss.dto.RssItemDto;
import com.monosun.secportal.setting.service.AppSettingService;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

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

    public List<RssItemDto> fetchKrcert() {
        return fetchKrcert(null);
    }

    /** daysOverride 가 있으면 그 기간(1~366일)으로, 없으면 설정관리의 rss.days 로 필터링 */
    public List<RssItemDto> fetchKrcert(Integer daysOverride) {
        int days = (daysOverride != null)
                ? Math.max(1, Math.min(366, daysOverride))
                : resolveDays();
        LocalDate cutoff = LocalDate.now().minusDays(days);
        List<Map<String, String>> feeds = resolveFeeds();

        List<RssItemDto> result = new ArrayList<>();
        for (Map<String, String> feed : feeds) {
            String url = feed.get("url");
            String category = feed.getOrDefault("category", "other");
            if (url != null && !url.isBlank()) {
                result.addAll(fetchFeed(url, category, cutoff));
            }
        }
        result.sort(Comparator.comparing(RssItemDto::getPubDate).reversed());
        return result;
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

    private List<RssItemDto> fetchFeed(String url, String category, LocalDate cutoff) {
        try {
            String xml = restTemplate.getForObject(url, String.class);
            return parseRss(xml, category, cutoff);
        } catch (Exception e) {
            return List.of();
        }
    }

    private List<RssItemDto> parseRss(String xml, String category, LocalDate cutoff) {
        List<RssItemDto> items = new ArrayList<>();
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new InputSource(new StringReader(xml)));

            NodeList nodeList = doc.getElementsByTagName("item");
            for (int i = 0; i < nodeList.getLength(); i++) {
                Element item = (Element) nodeList.item(i);
                String pubDate = text(item, "pubDate");
                if (cutoff != null && !isWithinCutoff(pubDate, cutoff)) continue;

                items.add(RssItemDto.builder()
                        .title(text(item, "title"))
                        .link(text(item, "link"))
                        .description(truncate(text(item, "description"), 200))
                        .pubDate(pubDate)
                        .category(category)
                        .build());
            }
        } catch (Exception ignored) {}
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
}
