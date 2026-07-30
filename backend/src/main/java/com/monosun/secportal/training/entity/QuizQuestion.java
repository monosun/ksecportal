package com.monosun.secportal.training.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "quiz_questions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuizQuestion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    private TrainingCourse course;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String question;

    @Column(name = "option_a", nullable = false)
    private String optionA;

    @Column(name = "option_b", nullable = false)
    private String optionB;

    @Column(name = "option_c")
    private String optionC;

    @Column(name = "option_d")
    private String optionD;

    /** 정답 — 단일은 "A", 복수 정답은 콤마로 구분해 오름차순 저장한다 (예: "A,C") */
    @Column(name = "correct_answer", nullable = false, length = 7)
    private String correctAnswer;

    /** 난이도: 상 / 중 / 하 */
    @Column(length = 10)
    private String difficulty;

    /** 해설 (선택) */
    @Column(columnDefinition = "TEXT")
    private String explanation;

    @Column(nullable = false)
    @Builder.Default
    private int sortOrder = 0;
}
