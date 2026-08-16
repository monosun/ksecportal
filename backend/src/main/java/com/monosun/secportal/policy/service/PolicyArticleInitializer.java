package com.monosun.secportal.policy.service;

import com.monosun.secportal.policy.entity.Policy;
import com.monosun.secportal.policy.repository.PolicyArticleRepository;
import com.monosun.secportal.policy.repository.PolicyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 이미 등록된 보안정책(장)을 조(條) 단위로 세분화해 둔다.
 *
 * <p>조 테이블이 비어 있을 때만 전체 파싱하는 seed-when-empty 시더 —
 * 신규 설치·구 볼륨·DB 재생성 어디서든 자체 복구된다.
 * 조가 이미 있으면 지침/장 정규화 컬럼이 비어 있는 정책만 보정한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@Order(60)
public class PolicyArticleInitializer implements ApplicationRunner {

    private final PolicyRepository policyRepository;
    private final PolicyArticleRepository articleRepository;
    private final PolicyStructureParser parser;
    private final PolicyArticleService articleService;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        List<Policy> policies = policyRepository.findAll();
        if (policies.isEmpty()) return;

        if (articleRepository.count() == 0) {
            int total = 0;
            for (Policy policy : policies) {
                total += articleService.sync(policy);
            }
            log.info("보안정책 조문 세분화 완료 — 정책(장) {}건 → 조 {}건", policies.size(), total);
            return;
        }

        // 조는 이미 있는데 지침/장 컬럼만 비어 있는 경우(구 버전에서 올라온 데이터) 보정
        int patched = 0;
        for (Policy policy : policies) {
            if (policy.getGuidelineName() == null) {
                parser.applyTitleStructure(policy);
                patched++;
            }
        }
        if (patched > 0) {
            log.info("보안정책 지침/장 정보 보정 — {}건", patched);
        }
    }
}
