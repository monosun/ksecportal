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

    /** 분류별 문제 수 (미분류는 category=null 버킷으로 반환) */
    @Query("SELECT q.category AS category, COUNT(q) AS total FROM QuizBankQuestion q GROUP BY q.category ORDER BY q.category")
    List<CategoryCount> countByCategory();

    /** 일괄등록 중복 제외용 — 등록된 모든 문제 텍스트 */
    @Query("SELECT q.question FROM QuizBankQuestion q")
    List<String> findAllQuestionTexts();

    @Modifying
    @Query("DELETE FROM QuizBankQuestion q WHERE q.category = :category")
    int deleteByCategory(@Param("category") String category);

    @Modifying
    @Query("DELETE FROM QuizBankQuestion q WHERE q.category IS NULL OR q.category = ''")
    int deleteUncategorized();

    /** 난이도가 비어 있는 기존 문항을 기본값 '중'으로 채운다 (시작 시 1회 백필) */
    @Modifying
    @Query("UPDATE QuizBankQuestion q SET q.difficulty = '중' WHERE q.difficulty IS NULL OR q.difficulty = ''")
    int fillMissingDifficulty();

    /** 분류별 문제 수 프로젝션 */
    interface CategoryCount {
        String getCategory();
        long getTotal();
    }
}
