package com.monosun.secportal.isms.repository;

import com.monosun.secportal.isms.entity.IsmsItemNote;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface IsmsItemNoteRepository extends JpaRepository<IsmsItemNote, Long> {

    Optional<IsmsItemNote> findByItemIdAndYear(Long itemId, int year);
}
