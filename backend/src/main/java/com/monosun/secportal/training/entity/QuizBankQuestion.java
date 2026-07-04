package com.monosun.secportal.training.entity;

import com.monosun.secportal.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

/** 문제은행 — 교육 코스와 독립적으로 관리되는 퀴즈 문항 저장소 */
@Entity
@Table(name = "quiz_bank_questions",
        indexes = @Index(name = "idx_quiz_bank_category", columnList = "category"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuizBankQuestion extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 분류 (예: 개인정보보호, 정보보안 일반, 피싱 대응) */
    @Column(length = 100)
    private String category;

    /** 난이도: 상 / 중 / 하 */
    @Column(length = 10)
    private String difficulty;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String question;

    @Column(name = "option_a", nullable = false, length = 500)
    private String optionA;

    @Column(name = "option_b", nullable = false, length = 500)
    private String optionB;

    @Column(name = "option_c", length = 500)
    private String optionC;

    @Column(name = "option_d", length = 500)
    private String optionD;

    @Column(name = "correct_answer", nullable = false, length = 1)
    private String correctAnswer;

    /** 해설 (선택) */
    @Column(columnDefinition = "TEXT")
    private String explanation;
}
