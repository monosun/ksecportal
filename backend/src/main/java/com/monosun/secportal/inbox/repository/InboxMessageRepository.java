package com.monosun.secportal.inbox.repository;

import com.monosun.secportal.inbox.entity.InboxMessage;
import com.monosun.secportal.auth.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InboxMessageRepository extends JpaRepository<InboxMessage, Long> {
    List<InboxMessage> findAllByRecipientOrderByCreatedAtDesc(User recipient);
    long countByRecipientAndReadFalse(User recipient);
    void deleteAllByRecipient(User recipient);
}
