package com.monosun.secportal.isms.repository;

import com.monosun.secportal.isms.entity.IsmsItemNote;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface IsmsItemNoteRepository extends JpaRepository<IsmsItemNote, Long> {

    Optional<IsmsItemNote> findByItemIdAndYear(Long itemId, int year);

    List<IsmsItemNote> findByYear(int year);

    /** 전년도 가져오기로 생성된 현재상태·의견 (가져오기 초기화 대상) */
    List<IsmsItemNote> findByYearAndCopiedFromYearIsNotNull(int year);

    long countByYearAndCopiedFromYearIsNotNull(int year);
}
