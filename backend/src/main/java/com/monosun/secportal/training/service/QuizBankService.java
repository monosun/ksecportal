package com.monosun.secportal.training.service;

import com.monosun.secportal.audit.service.AuditLogService;
import com.monosun.secportal.common.exception.BusinessException;
import com.monosun.secportal.common.exception.ResourceNotFoundException;
import com.monosun.secportal.training.dto.QuizBankDto;
import com.monosun.secportal.training.entity.QuizBankQuestion;
import com.monosun.secportal.training.repository.QuizBankQuestionRepository;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class QuizBankService {

    private final QuizBankQuestionRepository repository;
    private final AuditLogService auditLogService;

    private static final Set<String> ANSWERS = Set.of("A", "B", "C", "D");
    private static final Set<String> DIFFICULTIES = Set.of("상", "중", "하");

    private static final String[] HEADERS = {
            "분류", "난이도(상/중/하)*", "문제*", "보기A*", "보기B*", "보기C", "보기D", "정답(A~D)*", "해설"
    };

    // ── 조회 ─────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Page<QuizBankDto.Response> list(String category, String difficulty, String keyword, int page, int size) {
        String cat = (category == null || category.isBlank()) ? null : category.trim();
        String diff = (difficulty == null || difficulty.isBlank()) ? null : difficulty.trim();
        String kw = (keyword == null || keyword.isBlank()) ? null : keyword.trim();
        return repository.search(cat, diff, kw, PageRequest.of(page, size)).map(QuizBankDto.Response::from);
    }

    @Transactional(readOnly = true)
    public List<String> categories() {
        return repository.findDistinctCategories();
    }

    @Transactional(readOnly = true)
    public List<QuizBankDto.CategoryStat> categoryStats() {
        return repository.countByCategory().stream()
                .map(c -> QuizBankDto.CategoryStat.builder()
                        .category(c.getCategory())
                        .count(c.getTotal())
                        .build())
                .collect(Collectors.toList());
    }

    /** 분류 단위 일괄 삭제. category가 비어 있으면 미분류 문항을 삭제한다. */
    @Transactional
    public int deleteByCategory(String category) {
        String cat = (category == null) ? null : category.trim();
        boolean uncategorized = (cat == null || cat.isEmpty());
        int deleted = uncategorized ? repository.deleteUncategorized() : repository.deleteByCategory(cat);
        auditLogService.log("QUIZ_BANK_CATEGORY_DELETED", "QUIZ_BANK", null,
                "category=" + (uncategorized ? "(미분류)" : cat) + ", count=" + deleted);
        return deleted;
    }

    // ── CRUD ─────────────────────────────────────────────────────────

    @Transactional
    public QuizBankDto.Response create(QuizBankDto.Request req) {
        validate(req.getCorrectAnswer(), req.getOptionC(), req.getOptionD());
        QuizBankQuestion q = QuizBankQuestion.builder()
                .category(trim(req.getCategory()))
                .difficulty(normalizeDifficulty(req.getDifficulty()))
                .question(req.getQuestion().trim())
                .optionA(req.getOptionA().trim())
                .optionB(req.getOptionB().trim())
                .optionC(trim(req.getOptionC()))
                .optionD(trim(req.getOptionD()))
                .correctAnswer(req.getCorrectAnswer().trim().toUpperCase())
                .explanation(trim(req.getExplanation()))
                .build();
        q = repository.save(q);
        auditLogService.log("QUIZ_BANK_CREATED", "QUIZ_BANK", q.getId(), "category=" + q.getCategory());
        return QuizBankDto.Response.from(q);
    }

    @Transactional
    public QuizBankDto.Response update(Long id, QuizBankDto.Request req) {
        validate(req.getCorrectAnswer(), req.getOptionC(), req.getOptionD());
        QuizBankQuestion q = find(id);
        q.setCategory(trim(req.getCategory()));
        q.setDifficulty(normalizeDifficulty(req.getDifficulty()));
        q.setQuestion(req.getQuestion().trim());
        q.setOptionA(req.getOptionA().trim());
        q.setOptionB(req.getOptionB().trim());
        q.setOptionC(trim(req.getOptionC()));
        q.setOptionD(trim(req.getOptionD()));
        q.setCorrectAnswer(req.getCorrectAnswer().trim().toUpperCase());
        q.setExplanation(trim(req.getExplanation()));
        auditLogService.log("QUIZ_BANK_UPDATED", "QUIZ_BANK", id, "");
        return QuizBankDto.Response.from(q);
    }

    @Transactional
    public void delete(Long id) {
        find(id);
        repository.deleteById(id);
        auditLogService.log("QUIZ_BANK_DELETED", "QUIZ_BANK", id, "");
    }

    // ── Excel 템플릿 / 일괄 등록 ──────────────────────────────────────

    public byte[] generateTemplate() throws IOException {
        try (XSSFWorkbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = wb.createSheet("문제은행");

            CellStyle headerStyle = wb.createCellStyle();
            Font headerFont = wb.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            Row header = sheet.createRow(0);
            for (int i = 0; i < HEADERS.length; i++) {
                Cell cell = header.createCell(i);
                cell.setCellValue(HEADERS[i]);
                cell.setCellStyle(headerStyle);
            }

            // 예시 행
            Row ex = sheet.createRow(1);
            ex.createCell(0).setCellValue("개인정보보호");
            ex.createCell(1).setCellValue("하");
            ex.createCell(2).setCellValue("개인정보 보호법상 '개인정보'는 누구에 관한 정보인가?");
            ex.createCell(3).setCellValue("살아 있는 개인에 관한 정보");
            ex.createCell(4).setCellValue("법인에 관한 정보");
            ex.createCell(5).setCellValue("사망한 사람에 관한 정보");
            ex.createCell(6).setCellValue("국가기관에 관한 정보");
            ex.createCell(7).setCellValue("A");
            ex.createCell(8).setCellValue("개인정보는 살아 있는 개인에 관한 정보만 해당합니다.");

            int[] widths = {14, 14, 60, 30, 30, 30, 30, 12, 40};
            for (int i = 0; i < widths.length; i++) sheet.setColumnWidth(i, widths[i] * 256);

            wb.write(out);
            return out.toByteArray();
        }
    }

    @Transactional
    public QuizBankDto.BulkResult upload(MultipartFile file) throws IOException {
        List<String> errors = new ArrayList<>();
        List<QuizBankQuestion> toSave = new ArrayList<>();
        int skipped = 0;

        // 이미 등록된 문제 텍스트 + 이번 파일에서 이미 본 문제 텍스트 — 동일 문제는 등록하지 않는다.
        Set<String> known = repository.findAllQuestionTexts().stream()
                .map(s -> s == null ? "" : s.trim())
                .collect(Collectors.toCollection(HashSet::new));

        try (Workbook wb = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = wb.getSheetAt(0);
            for (int r = 1; r <= sheet.getLastRowNum(); r++) {
                Row row = sheet.getRow(r);
                if (row == null || isEmptyRow(row)) continue;
                int rowNo = r + 1;
                try {
                    String difficulty = str(row, 1);
                    String question = str(row, 2);
                    String optA = str(row, 3);
                    String optB = str(row, 4);
                    String answer = str(row, 7).toUpperCase();
                    if (question.isBlank()) { errors.add(rowNo + "행: 문제가 비어 있습니다."); continue; }
                    if (optA.isBlank() || optB.isBlank()) { errors.add(rowNo + "행: 보기A·보기B는 필수입니다."); continue; }
                    if (!difficulty.isBlank() && !DIFFICULTIES.contains(difficulty)) {
                        errors.add(rowNo + "행: 난이도는 상/중/하 중 하나여야 합니다.");
                        continue;
                    }
                    String optC = str(row, 5);
                    String optD = str(row, 6);
                    if (!ANSWERS.contains(answer)) { errors.add(rowNo + "행: 정답은 A~D 중 하나여야 합니다."); continue; }
                    if (("C".equals(answer) && optC.isBlank()) || ("D".equals(answer) && optD.isBlank())) {
                        errors.add(rowNo + "행: 정답 보기가 비어 있습니다.");
                        continue;
                    }
                    // 동일 문제(기존 등록분 또는 파일 내 중복)는 등록하지 않고 건너뛴다.
                    if (!known.add(question.trim())) { skipped++; continue; }
                    toSave.add(QuizBankQuestion.builder()
                            .category(blankToNull(str(row, 0)))
                            .difficulty(difficulty.isBlank() ? "중" : difficulty)
                            .question(question)
                            .optionA(optA)
                            .optionB(optB)
                            .optionC(blankToNull(optC))
                            .optionD(blankToNull(optD))
                            .correctAnswer(answer)
                            .explanation(blankToNull(str(row, 8)))
                            .build());
                } catch (Exception e) {
                    errors.add(rowNo + "행: " + e.getMessage());
                }
            }
        }

        repository.saveAll(toSave);
        auditLogService.log("QUIZ_BANK_BULK_UPLOADED", "QUIZ_BANK", null,
                "success=" + toSave.size() + ", fail=" + errors.size() + ", skipped=" + skipped);
        return QuizBankDto.BulkResult.builder()
                .successCount(toSave.size())
                .failCount(errors.size())
                .skippedCount(skipped)
                .errors(errors)
                .build();
    }

    // ── 헬퍼 ─────────────────────────────────────────────────────────

    private QuizBankQuestion find(Long id) {
        return repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("QuizBankQuestion", id));
    }

    private void validate(String answer, String optC, String optD) {
        String a = answer == null ? "" : answer.trim().toUpperCase();
        if (!ANSWERS.contains(a)) throw new BusinessException("정답은 A~D 중 하나여야 합니다.");
        if ("C".equals(a) && (optC == null || optC.isBlank())) throw new BusinessException("정답으로 지정한 보기C가 비어 있습니다.");
        if ("D".equals(a) && (optD == null || optD.isBlank())) throw new BusinessException("정답으로 지정한 보기D가 비어 있습니다.");
    }

    /** 난이도 정규화 — 비어 있으면 '중', 그 외에는 상/중/하만 허용 */
    private String normalizeDifficulty(String d) {
        if (d == null || d.isBlank()) return "중";
        String v = d.trim();
        if (!DIFFICULTIES.contains(v)) throw new BusinessException("난이도는 상/중/하 중 하나여야 합니다.");
        return v;
    }

    private String trim(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }

    private String blankToNull(String s) {
        return s.isBlank() ? null : s;
    }

    private boolean isEmptyRow(Row row) {
        for (int c = 0; c <= 8; c++) {
            if (!str(row, c).isBlank()) return false;
        }
        return true;
    }

    private String str(Row row, int col) {
        Cell cell = row.getCell(col);
        if (cell == null) return "";
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue().trim();
            case NUMERIC -> {
                double d = cell.getNumericCellValue();
                yield d == Math.floor(d) ? String.valueOf((long) d) : String.valueOf(d);
            }
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            case FORMULA -> cell.getRichStringCellValue().getString().trim();
            default -> "";
        };
    }
}
