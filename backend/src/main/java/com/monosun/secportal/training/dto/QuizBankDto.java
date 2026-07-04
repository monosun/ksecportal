package com.monosun.secportal.training.dto;

import com.monosun.secportal.training.entity.QuizBankQuestion;
import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

public class QuizBankDto {

    @Getter
    @Setter
    public static class Request {
        private String category;
        private String difficulty;   // 상 / 중 / 하
        @NotBlank private String question;
        @NotBlank private String optionA;
        @NotBlank private String optionB;
        private String optionC;
        private String optionD;
        @NotBlank private String correctAnswer;   // A/B/C/D
        private String explanation;
    }

    @Getter
    @Builder
    public static class Response {
        private Long id;
        private String category;
        private String difficulty;
        private String question;
        private String optionA;
        private String optionB;
        private String optionC;
        private String optionD;
        private String correctAnswer;
        private String explanation;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        public static Response from(QuizBankQuestion q) {
            return Response.builder()
                    .id(q.getId())
                    .category(q.getCategory())
                    .difficulty(q.getDifficulty())
                    .question(q.getQuestion())
                    .optionA(q.getOptionA())
                    .optionB(q.getOptionB())
                    .optionC(q.getOptionC())
                    .optionD(q.getOptionD())
                    .correctAnswer(q.getCorrectAnswer())
                    .explanation(q.getExplanation())
                    .createdAt(q.getCreatedAt())
                    .updatedAt(q.getUpdatedAt())
                    .build();
        }
    }

    @Getter
    @Builder
    public static class BulkResult {
        private int successCount;
        private int failCount;
        private List<String> errors;
    }
}
