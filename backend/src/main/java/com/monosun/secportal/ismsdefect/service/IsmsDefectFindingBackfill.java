package com.monosun.secportal.ismsdefect.service;

import com.monosun.secportal.ismsdefect.repository.IsmsDefectRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 기동 시 <b>아직 보안 결함사항으로 가져오지 않은 ISMS 결함</b>을 보충한다.
 *
 * <p>연동 기능이 생기기 전에 등록된 결함, 동기화가 어긋난 경우를 스스로 복구하는 용도이며,
 * 이미 가져온 건은 건드리지 않으므로 매 기동마다 안전하게 반복 실행된다
 * (프로젝트의 seed-when-empty 초기화와 같은 성격).
 */
@Slf4j
@Component
@Order(63)
@RequiredArgsConstructor
public class IsmsDefectFindingBackfill implements ApplicationRunner {

    private final IsmsDefectRepository defectRepository;
    private final IsmsDefectFindingSync findingSync;

    @Override
    public void run(ApplicationArguments args) {
        try {
            int created = findingSync.syncMissing(defectRepository.findAll());
            if (created > 0) {
                log.info("[ISMS 결함] 보안 결함사항으로 신규 반영 {}건", created);
            }
        } catch (Exception e) {
            // 보충 실패가 기동을 막지 않도록 한다 — 다음 기동이나 결함 수정 시 다시 시도된다
            log.warn("[ISMS 결함] 보안 결함사항 보충 중 오류: {}", e.getMessage());
        }
    }
}
