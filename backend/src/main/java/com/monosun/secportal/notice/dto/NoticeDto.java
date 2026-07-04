package com.monosun.secportal.notice.dto;

import com.monosun.secportal.notice.entity.Notice;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

public class NoticeDto {

    @Getter
    @Builder
    public static class Response {
        private Long id;
        private String title;
        private String content;
        private boolean active;
        private boolean pinned;
        private String createdByName;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        public static Response from(Notice n) {
            return Response.builder()
                    .id(n.getId())
                    .title(n.getTitle())
                    .content(n.getContent())
                    .active(n.isActive())
                    .pinned(n.isPinned())
                    .createdByName(n.getCreatedBy() != null ? n.getCreatedBy().getName() : null)
                    .createdAt(n.getCreatedAt())
                    .updatedAt(n.getUpdatedAt())
                    .build();
        }
    }

    @Getter
    public static class CreateRequest {
        private String title;
        private String content;
        private Boolean pinned;
    }

    @Getter
    public static class UpdateRequest {
        private String title;
        private String content;
        private Boolean active;
        private Boolean pinned;
    }
}
