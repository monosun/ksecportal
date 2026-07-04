package com.monosun.secportal.rss.controller;

import com.monosun.secportal.common.response.ApiResponse;
import com.monosun.secportal.rss.dto.RssItemDto;
import com.monosun.secportal.rss.service.RssService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/rss")
@RequiredArgsConstructor
public class RssController {

    private final RssService rssService;

    @GetMapping("/krcert")
    public ApiResponse<List<RssItemDto>> krcert() {
        return ApiResponse.ok(rssService.fetchKrcert());
    }
}
