package com.monosun.secportal.phishing.service;

import com.monosun.secportal.phishing.PhishingTemplateDefaults;
import com.monosun.secportal.phishing.entity.PhishingTemplate;
import com.monosun.secportal.phishing.repository.PhishingTemplateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 모의 악성메일 훈련 기본 템플릿 예제를 테이블이 비어 있을 때만 시드한다.
 *
 * 신규 설치·구 볼륨·DB 재생성 어디서든 자체 복구되도록 마이그레이션이 아닌 시더로 둔다.
 * 관리자가 템플릿을 모두 지우면 다음 기동 때 다시 채워지므로,
 * 쓰지 않는 예제는 삭제 대신 교육 및 훈련 &gt; 모의 훈련 화면에서 관리한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@Order(60)
public class PhishingTemplateInitializer implements ApplicationRunner {

    private final PhishingTemplateRepository repository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (repository.count() > 0) return;

        for (PhishingTemplateDefaults.Row r : PhishingTemplateDefaults.TEMPLATES) {
            repository.save(PhishingTemplate.builder()
                    .name(r.name())
                    .category(r.category())
                    .difficulty(parseDifficulty(r.difficulty()))
                    .subject(r.subject())
                    .senderName(r.senderName())
                    .senderEmail(r.senderEmail())
                    .bodyHtml(r.bodyHtml())
                    .description(r.description())
                    .build());
        }
        log.info("[모의훈련] 악성메일 훈련 템플릿 예제 {}건 초기화", PhishingTemplateDefaults.TEMPLATES.size());
    }

    private PhishingTemplate.Difficulty parseDifficulty(String d) {
        try { return PhishingTemplate.Difficulty.valueOf(d); }
        catch (Exception e) { return PhishingTemplate.Difficulty.MEDIUM; }
    }
}
