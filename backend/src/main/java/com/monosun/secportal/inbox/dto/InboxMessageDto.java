package com.monosun.secportal.inbox.dto;

import com.monosun.secportal.inbox.entity.InboxMessage;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

public class InboxMessageDto {

    @Getter
    @Builder
    public static class Response {
        private Long id;
        private String type;
        private String title;
        private String content;
        private boolean read;
        private boolean hasAction;
        private String actionStatus;
        private LocalDateTime createdAt;

        public static Response from(InboxMessage m) {
            return Response.builder()
                    .id(m.getId())
                    .type(m.getType().name())
                    .title(m.getTitle())
                    .content(m.getContent())
                    .read(m.isRead())
                    .hasAction(m.getActionToken() != null)
                    .actionStatus(m.getActionToken() != null ? m.getActionStatus().name() : null)
                    .createdAt(m.getCreatedAt())
                    .build();
        }
    }

    @Getter
    @Builder
    public static class UnreadCount {
        private long count;
    }
}
