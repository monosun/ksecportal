package com.monosun.secportal.rss.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * RSS 조회 결과 — 가져온 게시물과 함께 <b>실패한 피드의 사유</b>를 돌려준다.
 *
 * <p>피드 한두 개가 죽어도 나머지는 보여줘야 하므로 전체를 실패로 처리하지 않는다.
 * 대신 실패 사유를 화면에 그대로 노출해 "게시물이 없음"과 "접속 실패"를 구분할 수 있게 한다.
 */
@Getter
@Builder
public class RssResultDto {

    private List<RssItemDto> items;
    private List<FeedError> errors;

    @Getter
    @Builder
    public static class FeedError {
        /** 설정관리에 등록한 피드 이름 (없으면 호스트명) */
        private String label;
        private String category;
        private String url;
        /** 사용자에게 그대로 보여줄 사유 (예: "접속 시간이 초과되었습니다(15초)") */
        private String message;
    }
}
