package com.monosun.secportal.relatedsite.service;

import com.monosun.secportal.relatedsite.dto.RelatedSiteDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 관련 사이트 내용을 하루 한 번 자동으로 가져온다(기본 매일 07:10).
 * 주기는 application.yml 의 relatedsite.refresh-cron (환경변수 RELATED_SITE_CRON) 으로 바꾼다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RelatedSiteScheduler {

    private final RelatedSiteService service;

    @Scheduled(cron = "${relatedsite.refresh-cron}")
    public void refreshDaily() {
        try {
            RelatedSiteDto.RefreshResult result = service.refreshAll();
            log.info("[관련사이트] 자동 새로고침 — 대상 {}건, 성공 {}건, 실패 {}건, 게시물 {}건",
                    result.getTotal(), result.getSucceeded(), result.getFailed(), result.getItems());
        } catch (Exception e) {
            log.warn("[관련사이트] 자동 새로고침 실패", e);
        }
    }
}
