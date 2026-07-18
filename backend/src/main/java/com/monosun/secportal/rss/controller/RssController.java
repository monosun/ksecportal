package com.monosun.secportal.rss.controller;

import com.monosun.secportal.common.response.ApiResponse;
import com.monosun.secportal.rss.dto.RssItemDto;
import com.monosun.secportal.rss.service.RssService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/rss")
@RequiredArgsConstructor
public class RssController {

    private final RssService rssService;

    /** days 를 주면 해당 조회 기간으로 필터링, 없으면 설정관리의 rss.days 사용 */
    @GetMapping("/krcert")
    public ApiResponse<List<RssItemDto>> krcert(@RequestParam(required = false) Integer days) {
        return ApiResponse.ok(rssService.fetchKrcert(days));
    }
}
