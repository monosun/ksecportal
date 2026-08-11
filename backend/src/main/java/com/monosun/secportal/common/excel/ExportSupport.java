package com.monosun.secportal.common.excel;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/** 엑셀 내려받기 응답과 날짜 표기를 공통으로 다룬다. */
public final class ExportSupport {

    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final String XLSX = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    private ExportSupport() { }

    /** 한글 파일명이 깨지지 않도록 RFC 5987 형식으로 내려준다. */
    public static ResponseEntity<byte[]> xlsx(byte[] data, String filename) {
        String encoded = URLEncoder.encode(filename, StandardCharsets.UTF_8).replace("+", "%20");
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(XLSX));
        headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encoded);
        return ResponseEntity.ok().headers(headers).body(data);
    }

    /** 파일명에 쓸 수 없는 문자를 걸러낸다. */
    public static String safeFileName(String name) {
        if (name == null || name.isBlank()) return "export";
        return name.replaceAll("[\\\\/:*?\"<>|]", "_").trim();
    }

    public static String dt(LocalDateTime v) {
        return v != null ? v.format(DATE_TIME) : "-";
    }

    public static String d(LocalDate v) {
        return v != null ? v.format(DATE) : "-";
    }

    public static String now() {
        return LocalDateTime.now().format(DATE_TIME);
    }
}
