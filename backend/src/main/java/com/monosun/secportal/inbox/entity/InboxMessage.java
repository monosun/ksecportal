package com.monosun.secportal.inbox.entity;

import com.monosun.secportal.auth.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "inbox_messages")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InboxMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recipient_id", nullable = false)
    private User recipient;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MessageType type;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String content;

    /** PendingAdminAction 토큰 — APPROVAL_REQUEST 타입일 때만 사용 */
    @Column(name = "action_token", length = 100)
    private String actionToken;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(name = "action_status", columnDefinition = "VARCHAR(20) NOT NULL DEFAULT 'PENDING'")
    private ActionStatus actionStatus = ActionStatus.PENDING;

    @Builder.Default
    @Column(name = "is_read", nullable = false)
    private boolean read = false;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }

    public enum MessageType {
        APPROVAL_REQUEST, INFO, SYSTEM
    }

    public enum ActionStatus {
        PENDING, APPROVED, REJECTED
    }
}
