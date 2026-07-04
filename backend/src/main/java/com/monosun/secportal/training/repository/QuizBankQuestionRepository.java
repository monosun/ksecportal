package com.monosun.secportal.training.repository;

import com.monosun.secportal.training.entity.QuizBankQuestion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface QuizBankQuestionRepository extends JpaRepository<QuizBankQuestion, Long> {

    @Query("SELECT q FROM QuizBankQuestion q WHERE " +
           "(:category IS NULL OR q.category = :category) AND " +
           "(:difficulty IS NULL OR q.difficulty = :difficulty) AND " +
           "(:keyword IS NULL OR q.question LIKE %:keyword%) " +
           "ORDER BY q.id DESC")
    Page<QuizBankQuestion> search(@Param("category") String category,
                                  @Param("difficulty") String difficulty,
                                  @Param("keyword") String keyword,
                                  Pageable pageable);

    @Query("SELECT DISTINCT q.category FROM QuizBankQuestion q WHERE q.category IS NOT NULL ORDER BY q.category")
    List<String> findDistinctCategories();

    /** 난이도가 비어 있는 기존 문항을 기본값 '중'으로 채운다 (시작 시 1회 백필) */
    @Modifying
    @Query("UPDATE QuizBankQuestion q SET q.difficulty = '중' WHERE q.difficulty IS NULL OR q.difficulty = ''")
    int fillMissingDifficulty();
}
