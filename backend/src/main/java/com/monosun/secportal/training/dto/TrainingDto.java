package com.monosun.secportal.training.dto;

import com.monosun.secportal.training.entity.QuizQuestion;
import com.monosun.secportal.training.entity.TrainingCompletion;
import com.monosun.secportal.training.entity.TrainingCourse;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class TrainingDto {

    @Getter
    @Setter
    public static class CreateRequest {
        @NotBlank private String title;
        private String description;
        @NotNull private TrainingCourse.ContentType contentType;
        private String contentUrl;
        private Integer passingScore;
        private Boolean mandatory;
        private List<QuestionRequest> questions;
    }

    @Getter
    @Setter
    public static class UpdateRequest {
        private String title;
        private String description;
        private TrainingCourse.ContentType contentType;
        private String contentUrl;
        private Integer passingScore;
        private Boolean mandatory;
        private List<QuestionRequest> questions;
    }

    @Getter
    @Setter
    public static class QuestionRequest {
        @NotBlank private String question;
        @NotBlank private String optionA;
        @NotBlank private String optionB;
        private String optionC;
        private String optionD;
        @NotBlank private String correctAnswer;
        private String difficulty;   // 상 / 중 / 하
        private String explanation;
        private int sortOrder;
    }

    @Getter
    @Setter
    public static class SubmitQuizRequest {
        @NotNull private Map<Long, String> answers;
    }

    @Getter
    @Builder
    public static class CourseResponse {
        private Long id;
        private String title;
        private String description;
        private String contentType;
        private String contentUrl;
        private int passingScore;
        private boolean mandatory;
        private List<QuestionResponse> questions;
        private long completionCount;
        private boolean completedByMe;
        private Integer myScore;
        private LocalDateTime createdAt;

        public static CourseResponse from(TrainingCourse course, long completionCount, boolean completed, Integer myScore) {
            return CourseResponse.builder()
                    .id(course.getId())
                    .title(course.getTitle())
                    .description(course.getDescription())
                    .contentType(course.getContentType().name())
                    .contentUrl(course.getContentUrl())
                    .passingScore(course.getPassingScore())
                    .mandatory(course.isMandatory())
                    .questions(course.getQuestions().stream()
                            .map(QuestionResponse::from)
                            .collect(Collectors.toList()))
                    .completionCount(completionCount)
                    .completedByMe(completed)
                    .myScore(myScore)
                    .createdAt(course.getCreatedAt())
                    .build();
        }
    }

    @Getter
    @Builder
    public static class CourseSummary {
        private Long id;
        private String title;
        private String contentType;
        private boolean mandatory;
        private int passingScore;
        private boolean completedByMe;
        private Integer myScore;
        private LocalDateTime createdAt;

        public static CourseSummary from(TrainingCourse course, boolean completed, Integer myScore) {
            return CourseSummary.builder()
                    .id(course.getId())
                    .title(course.getTitle())
                    .contentType(course.getContentType().name())
                    .mandatory(course.isMandatory())
                    .passingScore(course.getPassingScore())
                    .completedByMe(completed)
                    .myScore(myScore)
                    .createdAt(course.getCreatedAt())
                    .build();
        }
    }

    @Getter
    @Builder
    public static class QuestionResponse {
        private Long id;
        private String question;
        private String optionA;
        private String optionB;
        private String optionC;
        private String optionD;
        private String correctAnswer;
        private String difficulty;
        private String explanation;
        private int sortOrder;

        public static QuestionResponse from(QuizQuestion q) {
            return QuestionResponse.builder()
                    .id(q.getId())
                    .question(q.getQuestion())
                    .optionA(q.getOptionA())
                    .optionB(q.getOptionB())
                    .optionC(q.getOptionC())
                    .optionD(q.getOptionD())
                    .correctAnswer(q.getCorrectAnswer())
                    .difficulty(q.getDifficulty())
                    .explanation(q.getExplanation())
                    .sortOrder(q.getSortOrder())
                    .build();
        }
    }

    @Getter
    @Builder
    public static class QuizResult {
        private int score;
        private boolean passed;
        private int correctCount;
        private int totalCount;
    }

    @Getter
    @Builder
    public static class CourseResultSummary {
        private Long courseId;
        private String title;
        private boolean mandatory;
        private int passingScore;
        private long completedCount;
        private long passedCount;
        private Double avgScore;
        private long totalUsers;    // 활성 사용자 수 (이수율 분모)
    }

    @Getter
    @Builder
    public static class CompletionRow {
        private Long id;
        private Long courseId;
        private String courseTitle;
        private String userName;
        private String userDepartment;
        private Integer score;
        private Boolean passed;
        private java.time.LocalDateTime completedAt;

        public static CompletionRow from(TrainingCompletion c) {
            return CompletionRow.builder()
                    .id(c.getId())
                    .courseId(c.getCourse().getId())
                    .courseTitle(c.getCourse().getTitle())
                    .userName(c.getUser() != null ? c.getUser().getName() : null)
                    .userDepartment(c.getUser() != null ? c.getUser().getDepartment() : null)
                    .score(c.getScore())
                    .passed(c.getPassed())
                    .completedAt(c.getCompletedAt())
                    .build();
        }
    }
}
