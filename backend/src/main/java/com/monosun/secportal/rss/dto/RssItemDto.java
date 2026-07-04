package com.monosun.secportal.rss.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class RssItemDto {
    private String title;
    private String link;
    private String description;
    private String pubDate;
    private String category; // "vuln" or "notice"
}
