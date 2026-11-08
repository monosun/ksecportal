package com.monosun.secportal.detection.service;

import com.monosun.secportal.detection.entity.DetectionEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 탐지 신호 발생 창구. 필터·로그인 처리 등 요청 경로에서 호출되므로, 이벤트 저장이 실패해도
 * (트랜잭션 롤백 예외 포함) 원래 요청 처리에는 절대 영향을 주지 않도록 모든 예외를 삼킨다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DetectionSignals {

    private final DetectionEventService eventService;

    public DetectionEvent fire(DetectionEventService.Signal signal) {
        try {
            return eventService.raise(signal);
        } catch (Exception e) {
            log.warn("[탐지] 신호 처리 실패({}): {}", signal.category(), e.getMessage());
            return null;
        }
    }
}
