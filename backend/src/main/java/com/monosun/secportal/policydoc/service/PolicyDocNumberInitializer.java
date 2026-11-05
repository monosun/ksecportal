package com.monosun.secportal.policydoc.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * 문서번호 자동 부여 도입 전에 번호 없이 등록된 정책 문서에 분류 키워드 번호(REG-001 등)를 채운다.
 * 대상이 없으면 아무것도 하지 않으므로 매 기동마다 실행해도 안전하다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PolicyDocNumberInitializer implements ApplicationRunner {

    private final PolicyDocService policyDocService;

    @Override
    public void run(ApplicationArguments args) {
        try {
            int filled = policyDocService.backfillDocNos();
            if (filled > 0) log.info("[PolicyDoc] 문서번호 없는 정책 문서 {}건에 번호 자동 부여", filled);
        } catch (Exception e) {
            // 번호 보충 실패가 기동을 막지 않도록 한다 — 다음 기동이나 문서 수정 시 다시 채워진다
            log.warn("[PolicyDoc] 문서번호 자동 부여 실패: {}", e.getMessage());
        }
    }
}
