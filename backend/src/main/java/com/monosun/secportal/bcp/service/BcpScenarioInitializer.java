package com.monosun.secportal.bcp.service;

import com.monosun.secportal.bcp.BcpScenarioDefaults;
import com.monosun.secportal.bcp.entity.BcpScenario;
import com.monosun.secportal.bcp.entity.BcpScenarioStep;
import com.monosun.secportal.bcp.repository.BcpScenarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 재해복구·BCP 훈련 시나리오 예제를 테이블이 비어 있을 때만 시드한다.
 *
 * 신규 설치·구 볼륨·DB 재생성 어디서든 자체 복구되도록 마이그레이션이 아닌 시더로 둔다.
 * 쓰지 않는 예제는 삭제 대신 교육 및 훈련 &gt; 재해복구·BCP 훈련 화면에서 비활성 처리한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@Order(61)
public class BcpScenarioInitializer implements ApplicationRunner {

    private final BcpScenarioRepository repository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (repository.count() > 0) return;

        for (BcpScenarioDefaults.Row r : BcpScenarioDefaults.SCENARIOS) {
            BcpScenario scenario = BcpScenario.builder()
                    .name(r.name())
                    .category(r.category())
                    .difficulty(parseDifficulty(r.difficulty()))
                    .targetSystem(r.targetSystem())
                    .rtoMinutes(r.rtoMinutes())
                    .rpoMinutes(r.rpoMinutes())
                    .situation(r.situation())
                    .objective(r.objective())
                    .description(r.description())
                    .build();

            for (BcpScenarioDefaults.Step s : r.steps()) {
                scenario.getSteps().add(BcpScenarioStep.builder()
                        .scenario(scenario)
                        .stepOrder(s.order())
                        .title(s.title())
                        .roleName(s.roleName())
                        .action(s.action())
                        .targetMinutes(s.targetMinutes())
                        .successCriteria(s.successCriteria())
                        .build());
            }
            repository.save(scenario);
        }
        log.info("[BCP] 재해복구·BCP 훈련 시나리오 예제 {}건 초기화", BcpScenarioDefaults.SCENARIOS.size());
    }

    private BcpScenario.Difficulty parseDifficulty(String d) {
        try { return BcpScenario.Difficulty.valueOf(d); }
        catch (Exception e) { return BcpScenario.Difficulty.MEDIUM; }
    }
}
