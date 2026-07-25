package com.monosun.secportal.glossary.service;

import com.monosun.secportal.glossary.GlossaryDefaults;
import com.monosun.secportal.glossary.entity.GlossaryTerm;
import com.monosun.secportal.glossary.repository.GlossaryTermRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 보안 용어집 기본 데이터를 테이블이 비어 있을 때만 시드한다.
 *
 * 신규 설치·구 볼륨·DB 재생성 어디서든 자체 복구되도록 마이그레이션이 아닌 시더로 둔다.
 * 관리자가 용어를 모두 지우면 다음 기동 때 다시 채워지므로,
 * 쓰지 않는 용어는 삭제 대신 **미사용(active=false)** 으로 두는 것을 권장한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@Order(50)
public class GlossaryInitializer implements ApplicationRunner {

    private final GlossaryTermRepository repository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (repository.count() > 0) return;

        for (GlossaryDefaults.Row r : GlossaryDefaults.TERMS) {
            repository.save(GlossaryTerm.builder()
                    .name(r.name())
                    .nameEn(blankToNull(r.nameEn()))
                    .abbreviation(blankToNull(r.abbreviation()))
                    .category(blankToNull(r.category()))
                    .definition(blankToNull(r.definition()))
                    .keywords(blankToNull(r.keywords()))
                    .sortOrder(r.sortOrder())
                    .active(true)
                    .build());
        }
        log.info("[용어집] 보안 용어 {}건 초기화", GlossaryDefaults.TERMS.size());
    }

    private String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s;
    }
}
