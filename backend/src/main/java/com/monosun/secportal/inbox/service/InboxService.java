package com.monosun.secportal.inbox.service;

import com.monosun.secportal.admin.service.UserAdminService;
import com.monosun.secportal.auth.entity.User;
import com.monosun.secportal.common.exception.ResourceNotFoundException;
import com.monosun.secportal.inbox.dto.InboxMessageDto;
import com.monosun.secportal.inbox.entity.InboxMessage;
import com.monosun.secportal.inbox.repository.InboxMessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InboxService {

    private final InboxMessageRepository inboxMessageRepository;
    private final UserAdminService userAdminService;

    @Transactional(readOnly = true)
    public List<InboxMessageDto.Response> list(User recipient) {
        return inboxMessageRepository.findAllByRecipientOrderByCreatedAtDesc(recipient)
                .stream().map(InboxMessageDto.Response::from).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public long unreadCount(User recipient) {
        return inboxMessageRepository.countByRecipientAndReadFalse(recipient);
    }

    @Transactional
    public void markRead(Long id, User recipient) {
        InboxMessage msg = findOwned(id, recipient);
        msg.setRead(true);
    }

    @Transactional
    public void markAllRead(User recipient) {
        inboxMessageRepository.findAllByRecipientOrderByCreatedAtDesc(recipient)
                .forEach(m -> m.setRead(true));
    }

    @Transactional
    public String approve(Long id, User recipient) {
        InboxMessage msg = findOwned(id, recipient);
        if (msg.getActionToken() == null) {
            throw new IllegalStateException("승인 가능한 메시지가 아닙니다.");
        }
        msg.setRead(true);
        msg.setActionStatus(InboxMessage.ActionStatus.APPROVED);
        return userAdminService.approveAction(msg.getActionToken());
    }

    @Transactional
    public String reject(Long id, User recipient) {
        InboxMessage msg = findOwned(id, recipient);
        if (msg.getActionToken() == null) {
            throw new IllegalStateException("거부 가능한 메시지가 아닙니다.");
        }
        msg.setRead(true);
        msg.setActionStatus(InboxMessage.ActionStatus.REJECTED);
        return userAdminService.rejectAction(msg.getActionToken());
    }

    @Transactional
    public void deleteAll(User recipient) {
        inboxMessageRepository.deleteAllByRecipient(recipient);
    }

    /** 메시지 생성 (UserAdminService에서 호출) */
    @Transactional
    public void send(User recipient, InboxMessage.MessageType type, String title, String content, String actionToken) {
        InboxMessage msg = InboxMessage.builder()
                .recipient(recipient)
                .type(type)
                .title(title)
                .content(content)
                .actionToken(actionToken)
                .build();
        inboxMessageRepository.save(msg);
    }

    private InboxMessage findOwned(Long id, User recipient) {
        InboxMessage msg = inboxMessageRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("InboxMessage", id));
        if (!msg.getRecipient().getId().equals(recipient.getId())) {
            throw new IllegalStateException("접근 권한이 없습니다.");
        }
        return msg;
    }
}
