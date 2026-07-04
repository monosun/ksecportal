package com.monosun.secportal.training.service;

import com.monosun.secportal.training.repository.QuizBankQuestionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** 문제은행 데이터 보정 — 난이도 컬럼 도입 이전에 등록된 문항의 난이도를 기본값 '중'으로 채운다 */
@Component
@RequiredArgsConstructor
@Slf4j
public class QuizBankDataInitializer implements ApplicationRunner {

    private final QuizBankQuestionRepository repository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        int updated = repository.fillMissingDifficulty();
        if (updated > 0) {
            log.info("문제은행 난이도 백필 완료: 기존 문항 {}건의 난이도를 '중'으로 설정", updated);
        }
    }
}
