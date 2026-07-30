package com.monosun.secportal.training.service;

import com.monosun.secportal.common.exception.BusinessException;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.TreeSet;

/**
 * 퀴즈 정답 문자열 유틸.
 * 정답은 단일 "A" 또는 복수 "A,C" 형태(콤마 구분·오름차순)로 저장한다.
 * 입력은 "a c", "AC", "A/C" 처럼 느슨하게 들어와도 같은 정규형으로 받아들인다.
 */
public final class QuizAnswers {

    private static final Set<String> VALID = Set.of("A", "B", "C", "D");

    private QuizAnswers() {
    }

    /** 관리자 입력(문제 등록/수정·엑셀 업로드)용 — 정규형으로 변환하고 잘못된 값이면 예외를 던진다. */
    public static String normalize(String raw) {
        Set<String> letters = new TreeSet<>();
        String src = raw == null ? "" : raw.trim().toUpperCase();
        for (char ch : src.toCharArray()) {
            if (isSeparator(ch)) continue;
            String letter = String.valueOf(ch);
            if (!VALID.contains(letter)) {
                throw new BusinessException("정답은 A~D 중 하나 이상이어야 합니다. (입력값: " + raw + ")");
            }
            letters.add(letter);
        }
        if (letters.isEmpty()) throw new BusinessException("정답을 하나 이상 선택해야 합니다.");
        return String.join(",", letters);
    }

    /** 채점·비교용 — 유효한 A~D만 추려낸 집합. 응시자 입력이 비정상이어도 예외를 던지지 않는다. */
    public static Set<String> toSet(String raw) {
        Set<String> letters = new TreeSet<>();
        if (raw == null) return letters;
        for (char ch : raw.trim().toUpperCase().toCharArray()) {
            String letter = String.valueOf(ch);
            if (VALID.contains(letter)) letters.add(letter);
        }
        return letters;
    }

    /** 응시자 답안이 정답과 정확히 일치하는지 — 복수 정답은 모두 골라야 정답으로 인정한다. */
    public static boolean matches(String correctAnswer, String submitted) {
        Set<String> correct = toSet(correctAnswer);
        return !correct.isEmpty() && correct.equals(toSet(submitted));
    }

    /** 정답으로 지정한 보기에 내용이 있는지 검증한다. options는 A,B,C,D 순서. */
    public static void validateOptionsPresent(String correctAnswer, String... options) {
        Set<String> missing = new LinkedHashSet<>();
        for (String letter : toSet(correctAnswer)) {
            int idx = letter.charAt(0) - 'A';
            String option = idx < options.length ? options[idx] : null;
            if (option == null || option.isBlank()) missing.add(letter);
        }
        if (!missing.isEmpty()) {
            throw new BusinessException("정답으로 지정한 보기(" + String.join(", ", missing) + ")가 비어 있습니다.");
        }
    }

    private static boolean isSeparator(char ch) {
        return ch == ',' || ch == ' ' || ch == '/' || ch == '|' || ch == '+' || ch == '·' || ch == '.';
    }
}
