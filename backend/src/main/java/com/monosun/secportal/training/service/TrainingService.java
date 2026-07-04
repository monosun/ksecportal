package com.monosun.secportal.training.service;

import com.monosun.secportal.auth.entity.User;
import com.monosun.secportal.auth.repository.UserRepository;
import com.monosun.secportal.common.exception.BusinessException;
import com.monosun.secportal.common.exception.ResourceNotFoundException;
import com.monosun.secportal.training.dto.TrainingDto;
import com.monosun.secportal.training.entity.QuizQuestion;
import com.monosun.secportal.training.entity.TrainingCompletion;
import com.monosun.secportal.training.entity.TrainingCourse;
import com.monosun.secportal.training.repository.TrainingCompletionRepository;
import com.monosun.secportal.training.repository.TrainingCourseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TrainingService {

    private final TrainingCourseRepository courseRepository;
    private final TrainingCompletionRepository completionRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public Page<TrainingDto.CourseSummary> list(String keyword, Long userId, Pageable pageable) {
        return courseRepository.search(keyword, pageable)
                .map(course -> {
                    boolean completed = completionRepository.existsByCourseIdAndUserId(course.getId(), userId);
                    Integer score = completionRepository.findByCourseIdAndUserId(course.getId(), userId)
                            .map(TrainingCompletion::getScore).orElse(null);
                    return TrainingDto.CourseSummary.from(course, completed, score);
                });
    }

    @Transactional(readOnly = true)
    public TrainingDto.CourseResponse get(Long id, Long userId) {
        TrainingCourse course = findById(id);
        long count = completionRepository.countByCourseId(id);
        boolean completed = completionRepository.existsByCourseIdAndUserId(id, userId);
        Integer score = completionRepository.findByCourseIdAndUserId(id, userId)
                .map(TrainingCompletion::getScore).orElse(null);
        return TrainingDto.CourseResponse.from(course, count, completed, score);
    }

    @Transactional
    public TrainingDto.CourseResponse create(TrainingDto.CreateRequest request, Long creatorId) {
        User creator = userRepository.findById(creatorId)
                .orElseThrow(() -> new BusinessException("User not found"));
        TrainingCourse course = TrainingCourse.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .contentType(request.getContentType())
                .contentUrl(request.getContentUrl())
                .passingScore(request.getPassingScore() != null ? request.getPassingScore() : 70)
                .mandatory(request.getMandatory() != null && request.getMandatory())
                .createdBy(creator)
                .build();
        if (request.getQuestions() != null) {
            List<QuizQuestion> questions = request.getQuestions().stream()
                    .map(q -> QuizQuestion.builder()
                            .course(course)
                            .question(q.getQuestion())
                            .optionA(q.getOptionA())
                            .optionB(q.getOptionB())
                            .optionC(q.getOptionC())
                            .optionD(q.getOptionD())
                            .correctAnswer(q.getCorrectAnswer().toUpperCase())
                            .difficulty(defaultDifficulty(q.getDifficulty()))
                            .explanation(q.getExplanation())
                            .sortOrder(q.getSortOrder())
                            .build())
                    .collect(Collectors.toList());
            course.getQuestions().addAll(questions);
        }
        return TrainingDto.CourseResponse.from(courseRepository.save(course), 0, false, null);
    }

    @Transactional
    public TrainingDto.QuizResult submitQuiz(Long courseId, Long userId, Map<Long, String> answers) {
        if (completionRepository.existsByCourseIdAndUserId(courseId, userId)) {
            throw new BusinessException("Already completed this course");
        }
        TrainingCourse course = findById(courseId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("User not found"));

        int correct = (int) course.getQuestions().stream()
                .filter(q -> q.getCorrectAnswer().equalsIgnoreCase(answers.getOrDefault(q.getId(), "")))
                .count();
        int total = course.getQuestions().size();
        int score = total > 0 ? (correct * 100 / total) : 0;
        boolean passed = score >= course.getPassingScore();

        if (passed) {
            completionRepository.save(TrainingCompletion.builder()
                    .course(course)
                    .user(user)
                    .score(score)
                    .passed(true)
                    .build());
        }

        return TrainingDto.QuizResult.builder()
                .score(score)
                .passed(passed)
                .correctCount(correct)
                .totalCount(total)
                .build();
    }

    @Transactional
    public TrainingDto.CourseResponse update(Long id, TrainingDto.UpdateRequest request) {
        TrainingCourse course = findById(id);
        if (request.getTitle() != null) course.setTitle(request.getTitle());
        if (request.getDescription() != null) course.setDescription(request.getDescription());
        if (request.getContentType() != null) course.setContentType(request.getContentType());
        course.setContentUrl(request.getContentUrl());
        if (request.getPassingScore() != null) course.setPassingScore(request.getPassingScore());
        if (request.getMandatory() != null) course.setMandatory(request.getMandatory());
        if (request.getQuestions() != null) {
            course.getQuestions().clear();
            List<QuizQuestion> newQuestions = request.getQuestions().stream()
                    .map(q -> QuizQuestion.builder()
                            .course(course)
                            .question(q.getQuestion())
                            .optionA(q.getOptionA())
                            .optionB(q.getOptionB())
                            .optionC(q.getOptionC())
                            .optionD(q.getOptionD())
                            .correctAnswer(q.getCorrectAnswer().toUpperCase())
                            .difficulty(defaultDifficulty(q.getDifficulty()))
                            .explanation(q.getExplanation())
                            .sortOrder(q.getSortOrder())
                            .build())
                    .collect(Collectors.toList());
            course.getQuestions().addAll(newQuestions);
        }
        long count = completionRepository.countByCourseId(id);
        return TrainingDto.CourseResponse.from(courseRepository.save(course), count, false, null);
    }

    @Transactional
    public void delete(Long id) {
        findById(id);
        completionRepository.deleteByCourseId(id);
        courseRepository.deleteById(id);
    }

    /** 난이도 기본값 보정 — 비어 있으면 '중' */
    private String defaultDifficulty(String d) {
        return (d == null || d.isBlank()) ? "중" : d.trim();
    }

    // ── 교육·훈련 결과 (MANAGER+) ─────────────────────────────────────

    @Transactional(readOnly = true)
    public List<TrainingDto.CourseResultSummary> resultSummary() {
        long totalUsers = userRepository.countByActiveTrue();
        return courseRepository.findAll().stream().map(c -> {
            List<TrainingCompletion> comps = completionRepository.findByCourseId(c.getId());
            long passed = comps.stream().filter(x -> Boolean.TRUE.equals(x.getPassed())).count();
            Double avg = comps.stream()
                    .map(TrainingCompletion::getScore)
                    .filter(java.util.Objects::nonNull)
                    .mapToInt(Integer::intValue)
                    .average().stream().boxed().findFirst().orElse(null);
            return TrainingDto.CourseResultSummary.builder()
                    .courseId(c.getId())
                    .title(c.getTitle())
                    .mandatory(c.isMandatory())
                    .passingScore(c.getPassingScore())
                    .completedCount(comps.size())
                    .passedCount(passed)
                    .avgScore(avg)
                    .totalUsers(totalUsers)
                    .build();
        }).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<TrainingDto.CompletionRow> resultCompletions(Long courseId) {
        List<TrainingCompletion> comps = (courseId != null)
                ? completionRepository.findByCourseIdOrderByCompletedAtDesc(courseId)
                : completionRepository.findAllByOrderByCompletedAtDesc();
        return comps.stream().map(TrainingDto.CompletionRow::from).collect(Collectors.toList());
    }

    private TrainingCourse findById(Long id) {
        return courseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("TrainingCourse", id));
    }
}
