package com.monosun.secportal.rss.controller;

import com.monosun.secportal.common.response.ApiResponse;
import com.monosun.secportal.rss.dto.RssResultDto;
import com.monosun.secportal.rss.service.RssService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/rss")
@RequiredArgsConstructor
public class RssController {

    private final RssService rssService;

    /**
     * days 를 주면 해당 조회 기간으로 필터링, 없으면 설정관리의 rss.days 사용.
     * 응답에는 가져온 게시물({@code items})과 함께 실패한 피드의 사유({@code errors})가 담긴다.
     */
    @GetMapping("/krcert")
    public ApiResponse<RssResultDto> krcert(@RequestParam(required = false) Integer days) {
        return ApiResponse.ok(rssService.fetchKrcert(days));
    }
}
