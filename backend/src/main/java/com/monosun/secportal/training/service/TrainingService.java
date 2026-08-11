package com.monosun.secportal.training.service;

import com.monosun.secportal.auth.entity.User;
import com.monosun.secportal.auth.repository.UserRepository;
import com.monosun.secportal.common.exception.BusinessException;
import com.monosun.secportal.common.exception.ResourceNotFoundException;
import com.monosun.secportal.common.excel.ExcelWriter;
import com.monosun.secportal.common.excel.ExportSupport;
import com.monosun.secportal.training.dto.TrainingDto;
import com.monosun.secportal.training.entity.QuizQuestion;
import com.monosun.secportal.training.entity.TrainingCompletion;
import com.monosun.secportal.training.entity.TrainingCourse;
import com.monosun.secportal.training.repository.TrainingCompletionRepository;
import com.monosun.secportal.training.repository.TrainingCourseRepository;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Sheet;
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
    public Page<TrainingDto.CourseSummary> list(String keyword, Boolean mandatory,
                                                TrainingCourse.ContentType contentType,
                                                Long userId, Pageable pageable) {
        return courseRepository.search(keyword, mandatory, contentType, pageable)
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
                            .optionE(q.getOptionE())
                            .correctAnswer(QuizAnswers.normalize(q.getCorrectAnswer()))
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

        // 복수 정답 문항은 정답 보기를 모두 선택해야 정답으로 인정한다.
        int correct = (int) course.getQuestions().stream()
                .filter(q -> QuizAnswers.matches(q.getCorrectAnswer(), answers.getOrDefault(q.getId(), "")))
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
                            .optionE(q.getOptionE())
                            .correctAnswer(QuizAnswers.normalize(q.getCorrectAnswer()))
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

    /** 교육 코스 1건의 개요·이수 이력·퀴즈 문항을 엑셀로 만든다. */
    @Transactional(readOnly = true)
    public byte[] exportCourseExcel(Long courseId) {
        TrainingCourse course = findById(courseId);
        List<TrainingCompletion> comps = completionRepository.findByCourseIdOrderByCompletedAtDesc(courseId);
        long totalUsers = userRepository.countByActiveTrue();
        long passed = comps.stream().filter(c -> Boolean.TRUE.equals(c.getPassed())).count();
        Double avg = comps.stream()
                .map(TrainingCompletion::getScore)
                .filter(java.util.Objects::nonNull)
                .mapToInt(Integer::intValue)
                .average().stream().boxed().findFirst().orElse(null);

        try (ExcelWriter xw = new ExcelWriter()) {
            // ── 시트 1: 개요 + 이수 이력 ──
            Sheet sheet = xw.sheet("교육 결과");
            int r = xw.title(sheet, 0, "교육 결과 — " + course.getTitle(), 5);
            r++; // 빈 행
            r = xw.meta(sheet, r, new String[][]{
                    {"교육명", course.getTitle()},
                    {"필수 여부", course.isMandatory() ? "필수" : "선택"},
                    {"콘텐츠 유형", course.getContentType() != null ? course.getContentType().name() : ""},
                    {"합격 기준 점수", String.valueOf(course.getPassingScore())},
                    {"퀴즈 문항 수", String.valueOf(course.getQuestions().size())},
                    {"대상 인원", String.valueOf(totalUsers)},
                    {"이수 인원", String.valueOf(comps.size())},
                    {"이수율", pct(comps.size(), totalUsers) + "%"},
                    {"합격 인원", String.valueOf(passed)},
                    {"합격률", pct(passed, comps.size()) + "%"},
                    {"평균 점수", avg != null ? String.valueOf(Math.round(avg)) : "-"},
                    {"내려받은 시각", ExportSupport.now()},
            });
            r++; // 빈 행

            r = xw.header(sheet, r, new String[]{"No", "이수자", "부서", "점수", "결과", "이수 일시"});
            int seq = 1;
            for (TrainingCompletion c : comps) {
                r = xw.row(sheet, r, new Object[]{
                        seq++,
                        c.getUser() != null ? c.getUser().getName() : "-",
                        c.getUser() != null ? c.getUser().getDepartment() : "-",
                        c.getScore() != null ? c.getScore() : "-",
                        Boolean.TRUE.equals(c.getPassed()) ? "합격" : "불합격",
                        ExportSupport.dt(c.getCompletedAt()),
                }, 0, 3, 4, 5);
            }
            xw.widths(sheet, 6, 18, 20, 10, 10, 22);

            // ── 시트 2: 퀴즈 문항 ──
            if (!course.getQuestions().isEmpty()) {
                Sheet qs = xw.sheet("퀴즈 문항");
                int qr = xw.header(qs, 0, new String[]{
                        "No", "문항", "①", "②", "③", "④", "⑤", "정답", "난이도", "해설"});
                int qSeq = 1;
                List<QuizQuestion> questions = course.getQuestions().stream()
                        .sorted(java.util.Comparator.comparingInt(QuizQuestion::getSortOrder))
                        .toList();
                for (QuizQuestion q : questions) {
                    qr = xw.row(qs, qr, new Object[]{
                            qSeq++, q.getQuestion(),
                            q.getOptionA(), q.getOptionB(), q.getOptionC(), q.getOptionD(), q.getOptionE(),
                            q.getCorrectAnswer(), q.getDifficulty(), q.getExplanation(),
                    }, 0, 7, 8);
                }
                xw.widths(qs, 6, 44, 22, 22, 22, 22, 22, 8, 8, 40);
            }

            return xw.toBytes();
        }
    }

    /** 파일명에 쓸 코스 제목 */
    @Transactional(readOnly = true)
    public String courseTitle(Long courseId) {
        return findById(courseId).getTitle();
    }

    private static long pct(long n, long d) {
        return d > 0 ? Math.round(n * 100.0 / d) : 0;
    }

    private TrainingCourse findById(Long id) {
        return courseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("TrainingCourse", id));
    }
}
