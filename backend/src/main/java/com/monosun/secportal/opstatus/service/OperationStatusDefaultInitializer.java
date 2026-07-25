package com.monosun.secportal.opstatus.service;

import com.monosun.secportal.opstatus.OperationStatusDefaults;
import com.monosun.secportal.opstatus.entity.OperationStatusDefault;
import com.monosun.secportal.opstatus.entity.OperationStatusItem;
import com.monosun.secportal.opstatus.repository.OperationStatusDefaultRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 운영현황관리 기본 항목 마스터를 구분별로 비어 있을 때만 시드한다.
 *
 * 신규 설치·구 볼륨·DB 재생성 어디서든 자체 복구되도록 마이그레이션이 아닌 시더로 둔다.
 * 관리자가 코드 관리에서 항목을 모두 지운 경우에는 다음 기동 때 다시 채워지므로,
 * "쓰지 않는 항목"은 삭제 대신 **미사용(active=false)** 으로 두는 것을 권장한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@Order(40)
public class OperationStatusDefaultInitializer implements ApplicationRunner {

    private final OperationStatusDefaultRepository repository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        for (OperationStatusItem.Type type : OperationStatusItem.Type.values()) {
            seed(type);
        }
    }

    private void seed(OperationStatusItem.Type type) {
        if (repository.countByType(type) > 0) return;

        int order = 1;
        for (OperationStatusDefaults.Row r : OperationStatusDefaults.of(type)) {
            repository.save(OperationStatusDefault.builder()
                    .type(type)
                    .category(blankToNull(r.category()))
                    .name(r.name())
                    .cycle(blankToNull(r.cycle()))
                    .deliverable(blankToNull(r.deliverable()))
                    .owner(blankToNull(r.owner()))
                    .manager(blankToNull(r.manager()))
                    .note(blankToNull(r.note()))
                    .planMonths(r.planMonths())
                    .sortOrder(order++)
                    .active(true)
                    .build());
        }
        log.info("[운영현황] {} 기본 항목 {}건 초기화", type, order - 1);
    }

    private String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s;
    }
}
