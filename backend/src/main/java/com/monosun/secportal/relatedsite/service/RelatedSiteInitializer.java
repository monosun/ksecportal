package com.monosun.secportal.relatedsite.service;

import com.monosun.secportal.relatedsite.RelatedSiteDefaults;
import com.monosun.secportal.relatedsite.entity.RelatedSite;
import com.monosun.secportal.relatedsite.repository.RelatedSiteRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 관련 사이트 기본 목록을 테이블이 비어 있을 때만 시드한다.
 *
 * 신규 설치·구 볼륨·DB 재생성 어디서든 자체 복구되도록 마이그레이션이 아닌 시더로 둔다.
 * 쓰지 않는 사이트는 삭제 대신 **미사용(active=false)** 으로 두면 다시 살아나지 않는다.
 * 실제 사이트 내용은 기동 시 가져오지 않고, 화면의 새로고침 또는 스케줄러가 채운다
 * (외부 접속이 막힌 환경에서 기동이 느려지지 않도록).
 */
@Slf4j
@Component
@RequiredArgsConstructor
@Order(51)
public class RelatedSiteInitializer implements ApplicationRunner {

    private final RelatedSiteRepository repository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (repository.count() > 0) return;

        for (RelatedSiteDefaults.Row r : RelatedSiteDefaults.SITES) {
            repository.save(RelatedSite.builder()
                    .name(r.name())
                    .url(r.url())
                    .feedUrl(blankToNull(r.feedUrl()))
                    .category(blankToNull(r.category()))
                    .description(blankToNull(r.description()))
                    .sortOrder(r.sortOrder())
                    .active(true)
                    .fetchStatus(RelatedSite.FetchStatus.NONE)
                    .build());
        }
        log.info("[관련사이트] 기본 사이트 {}건 초기화", RelatedSiteDefaults.SITES.size());
    }

    private String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s;
    }
}
