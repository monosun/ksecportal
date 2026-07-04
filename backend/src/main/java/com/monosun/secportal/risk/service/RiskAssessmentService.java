package com.monosun.secportal.risk.service;

import com.monosun.secportal.asset.entity.Asset;
import com.monosun.secportal.asset.repository.AssetRepository;
import com.monosun.secportal.common.exception.ResourceNotFoundException;
import com.monosun.secportal.risk.dto.RiskAssessmentDto;
import com.monosun.secportal.risk.entity.RiskAssessment;
import com.monosun.secportal.risk.entity.RiskAssessmentRound;
import com.monosun.secportal.risk.repository.RiskAssessmentRepository;
import com.monosun.secportal.risk.repository.RiskAssessmentRoundRepository;
import com.monosun.secportal.threat.entity.Threat;
import com.monosun.secportal.threat.repository.ThreatRepository;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RiskAssessmentService {

    private final RiskAssessmentRoundRepository roundRepository;
    private final RiskAssessmentRepository assessmentRepository;
    private final AssetRepository assetRepository;
    private final ThreatRepository threatRepository;

    // ── Round ──────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<Integer> listYears() {
        return roundRepository.findDistinctYears();
    }

    @Transactional(readOnly = true)
    public List<RiskAssessmentDto.RoundResponse> listRounds(int year) {
        return roundRepository.findByYearOrderByRoundNoAsc(year).stream()
                .map(r -> RiskAssessmentDto.RoundResponse.from(r, assessmentRepository.countByRoundId(r.getId())))
                .collect(Collectors.toList());
    }

    @Transactional
    public RiskAssessmentDto.RoundResponse createRound(RiskAssessmentDto.RoundRequest req) {
        RiskAssessmentRound round = RiskAssessmentRound.builder()
                .year(req.getYear())
                .roundNo(req.getRoundNo())
                .roundDate(req.getRoundDate())
                .title(req.getTitle())
                .status(req.getStatus() != null
                        ? RiskAssessmentRound.Status.valueOf(req.getStatus())
                        : RiskAssessmentRound.Status.IN_PROGRESS)
                .build();
        RiskAssessmentRound saved = roundRepository.save(round);
        return RiskAssessmentDto.RoundResponse.from(saved, 0L);
    }

    @Transactional
    public RiskAssessmentDto.RoundResponse updateRound(Long id, RiskAssessmentDto.RoundRequest req) {
        RiskAssessmentRound round = roundRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("RiskAssessmentRound", id));
        if (req.getYear() != null) round.setYear(req.getYear());
        if (req.getRoundNo() != null) round.setRoundNo(req.getRoundNo());
        if (req.getRoundDate() != null) round.setRoundDate(req.getRoundDate());
        if (req.getTitle() != null) round.setTitle(req.getTitle());
        if (req.getStatus() != null) round.setStatus(RiskAssessmentRound.Status.valueOf(req.getStatus()));
        RiskAssessmentRound saved = roundRepository.save(round);
        long count = assessmentRepository.countByRoundId(saved.getId());
        return RiskAssessmentDto.RoundResponse.from(saved, count);
    }

    @Transactional
    public void deleteRound(Long id) {
        if (!roundRepository.existsById(id)) {
            throw new ResourceNotFoundException("RiskAssessmentRound", id);
        }
        assessmentRepository.deleteByRoundId(id);
        roundRepository.deleteById(id);
    }

    // ── Assessment ─────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<RiskAssessmentDto.AssessmentResponse> listAssessments(
            Long roundId, String assetName, String assetType, String threatName, String threatType, String riskGrade) {
        RiskAssessment.Grade gradeEnum = null;
        if (riskGrade != null && !riskGrade.isBlank()) {
            try { gradeEnum = RiskAssessment.Grade.valueOf(riskGrade); } catch (IllegalArgumentException ignored) {}
        }
        String an = (assetName != null && assetName.isBlank()) ? null : assetName;
        String at = (assetType != null && assetType.isBlank()) ? null : assetType;
        String tn = (threatName != null && threatName.isBlank()) ? null : threatName;
        String tt = (threatType != null && threatType.isBlank()) ? null : threatType;
        return assessmentRepository.search(roundId, an, at, tn, tt, gradeEnum).stream()
                .map(RiskAssessmentDto.AssessmentResponse::from)
                .collect(Collectors.toList());
    }

    @Transactional
    public int autoPopulateFromAssets(Long roundId) {
        if (!roundRepository.existsById(roundId)) {
            throw new ResourceNotFoundException("RiskAssessmentRound", roundId);
        }
        // 스냅샷: 현재 활성 자산 & 위협 목록
        List<Asset> assets = assetRepository.findAll(PageRequest.of(0, 5000)).getContent()
                .stream().filter(Asset::isActive).collect(Collectors.toList());
        List<Threat> threats = threatRepository.findAllByOrderByCreatedAtDesc();

        // 이미 이 차수에 있는 (assetId, threatId) 쌍 (중복 방지)
        Set<String> existingPairs = assessmentRepository.findByRoundIdOrderByCreatedAtAsc(roundId)
                .stream()
                .filter(a -> a.getAssetId() != null && a.getThreatId() != null)
                .map(a -> a.getAssetId() + "-" + a.getThreatId())
                .collect(Collectors.toSet());

        List<RiskAssessment> toSave = new ArrayList<>();
        for (Asset asset : assets) {
            for (Threat threat : threats) {
                if (existingPairs.contains(asset.getId() + "-" + threat.getId())) continue;
                int score = threat.getLikelihood() * threat.getImpact();
                // assetName/assetType/threatName/threatType 스냅샷 저장 → 이후 변경에 영향 없음
                toSave.add(RiskAssessment.builder()
                        .roundId(roundId)
                        .assetId(asset.getId())
                        .assetName(asset.getName())
                        .assetType(asset.getType())
                        .assetEnvironment(asset.getEnvironment() != null ? asset.getEnvironment().name() : null)
                        .threatId(threat.getId())
                        .threatName(threat.getName())
                        .threatType(threat.getType())
                        .likelihood(threat.getLikelihood())
                        .impact(threat.getImpact())
                        .riskGrade(calcGrade(score))
                        .treatment(RiskAssessment.Treatment.경감)
                        .build());
            }
        }
        if (!toSave.isEmpty()) {
            assessmentRepository.saveAll(toSave);
        }
        return toSave.size();
    }

    @Transactional
    public RiskAssessmentDto.AssessmentResponse createAssessment(RiskAssessmentDto.AssessmentRequest req) {
        int score = req.getLikelihood() * req.getImpact();
        RiskAssessment assessment = RiskAssessment.builder()
                .roundId(req.getRoundId())
                .assetId(req.getAssetId())
                .assetName(req.getAssetName())
                .assetType(req.getAssetType())
                .assetEnvironment(req.getAssetEnvironment())
                .threatId(req.getThreatId())
                .threatName(req.getThreatName())
                .threatType(req.getThreatType())
                .vulnerability(req.getVulnerability())
                .likelihood(req.getLikelihood())
                .impact(req.getImpact())
                .riskGrade(calcGrade(score))
                .treatment(req.getTreatment() != null
                        ? RiskAssessment.Treatment.valueOf(req.getTreatment())
                        : RiskAssessment.Treatment.경감)
                .notes(req.getNotes())
                .build();
        return RiskAssessmentDto.AssessmentResponse.from(assessmentRepository.save(assessment));
    }

    @Transactional
    public RiskAssessmentDto.AssessmentResponse updateAssessment(Long id, RiskAssessmentDto.AssessmentRequest req) {
        RiskAssessment assessment = assessmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("RiskAssessment", id));
        if (req.getAssetId() != null) assessment.setAssetId(req.getAssetId());
        if (req.getAssetName() != null) assessment.setAssetName(req.getAssetName());
        if (req.getAssetType() != null) assessment.setAssetType(req.getAssetType());
        if (req.getAssetEnvironment() != null) assessment.setAssetEnvironment(req.getAssetEnvironment());
        if (req.getThreatId() != null) assessment.setThreatId(req.getThreatId());
        if (req.getThreatName() != null) assessment.setThreatName(req.getThreatName());
        if (req.getThreatType() != null) assessment.setThreatType(req.getThreatType());
        if (req.getVulnerability() != null) assessment.setVulnerability(req.getVulnerability());
        assessment.setLikelihood(req.getLikelihood());
        assessment.setImpact(req.getImpact());
        int score = req.getLikelihood() * req.getImpact();
        assessment.setRiskGrade(calcGrade(score));
        if (req.getTreatment() != null) assessment.setTreatment(RiskAssessment.Treatment.valueOf(req.getTreatment()));
        if (req.getNotes() != null) assessment.setNotes(req.getNotes());
        return RiskAssessmentDto.AssessmentResponse.from(assessmentRepository.save(assessment));
    }

    @Transactional
    public void deleteAssessment(Long id) {
        if (!assessmentRepository.existsById(id)) {
            throw new ResourceNotFoundException("RiskAssessment", id);
        }
        assessmentRepository.deleteById(id);
    }

    @Transactional
    public int bulkUpdateTreatment(List<Long> ids, String treatment) {
        if (ids == null || ids.isEmpty()) return 0;
        RiskAssessment.Treatment t = RiskAssessment.Treatment.valueOf(treatment);
        List<RiskAssessment> items = assessmentRepository.findAllById(ids);
        items.forEach(item -> item.setTreatment(t));
        assessmentRepository.saveAll(items);
        return items.size();
    }

    @Transactional(readOnly = true)
    public byte[] exportExcel(Long roundId) {
        RiskAssessmentRound round = roundRepository.findById(roundId)
                .orElseThrow(() -> new ResourceNotFoundException("RiskAssessmentRound", roundId));
        List<RiskAssessment> items = assessmentRepository.findByRoundIdOrderByCreatedAtAsc(roundId);

        // 자산명 기준으로 그룹핑 (등록 순서 유지)
        java.util.LinkedHashMap<String, List<RiskAssessment>> byAsset = new java.util.LinkedHashMap<>();
        for (RiskAssessment item : items) {
            String key = nvl(item.getAssetName());
            byAsset.computeIfAbsent(key, k -> new ArrayList<>()).add(item);
        }

        try (XSSFWorkbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            // ── 워크북 공통 스타일 ──────────────────────────────────────────
            Font boldFont = wb.createFont();
            boldFont.setBold(true);

            Font titleFont = wb.createFont();
            titleFont.setBold(true);
            titleFont.setFontHeightInPoints((short) 14);

            Font whiteBoldFont = wb.createFont();
            whiteBoldFont.setBold(true);
            whiteBoldFont.setColor(IndexedColors.WHITE.getIndex());

            CellStyle titleStyle = wb.createCellStyle();
            titleStyle.setFont(titleFont);
            titleStyle.setAlignment(HorizontalAlignment.CENTER);
            titleStyle.setVerticalAlignment(VerticalAlignment.CENTER);

            CellStyle metaLabelStyle = wb.createCellStyle();
            metaLabelStyle.setFont(boldFont);
            metaLabelStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            metaLabelStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            setBorder(metaLabelStyle);

            CellStyle metaValueStyle = wb.createCellStyle();
            setBorder(metaValueStyle);

            CellStyle headerStyle = wb.createCellStyle();
            headerStyle.setFont(whiteBoldFont);
            headerStyle.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);
            headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            setBorder(headerStyle);

            CellStyle assetHeaderStyle = wb.createCellStyle();
            assetHeaderStyle.setFont(whiteBoldFont);
            assetHeaderStyle.setFillForegroundColor(IndexedColors.DARK_TEAL.getIndex());
            assetHeaderStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            assetHeaderStyle.setAlignment(HorizontalAlignment.LEFT);
            assetHeaderStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            setBorder(assetHeaderStyle);

            CellStyle summaryHeaderStyle = wb.createCellStyle();
            summaryHeaderStyle.setFont(whiteBoldFont);
            summaryHeaderStyle.setFillForegroundColor(IndexedColors.DARK_GREEN.getIndex());
            summaryHeaderStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            summaryHeaderStyle.setAlignment(HorizontalAlignment.CENTER);
            summaryHeaderStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            setBorder(summaryHeaderStyle);

            CellStyle centerStyle = wb.createCellStyle();
            centerStyle.setAlignment(HorizontalAlignment.CENTER);
            centerStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            setBorder(centerStyle);

            CellStyle dataStyle = wb.createCellStyle();
            dataStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            setBorder(dataStyle);

            CellStyle highStyle = wb.createCellStyle();
            highStyle.setAlignment(HorizontalAlignment.CENTER);
            highStyle.setFillForegroundColor(IndexedColors.ROSE.getIndex());
            highStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            setBorder(highStyle);

            CellStyle medStyle = wb.createCellStyle();
            medStyle.setAlignment(HorizontalAlignment.CENTER);
            medStyle.setFillForegroundColor(IndexedColors.LIGHT_YELLOW.getIndex());
            medStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            setBorder(medStyle);

            CellStyle lowStyle = wb.createCellStyle();
            lowStyle.setAlignment(HorizontalAlignment.CENTER);
            lowStyle.setFillForegroundColor(IndexedColors.LIGHT_GREEN.getIndex());
            lowStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            setBorder(lowStyle);

            String roundTitle = round.getYear() + "년 " + round.getRoundNo() + "차 위험평가 결과"
                    + (round.getTitle() != null && !round.getTitle().isBlank() ? " — " + round.getTitle() : "");

            // ══════════════════════════════════════════════════════════════
            // 시트 1: 전체 요약
            // ══════════════════════════════════════════════════════════════
            Sheet summarySheet = wb.createSheet("전체 요약");
            int rowNum = 0;

            // 제목
            Row titleRow = summarySheet.createRow(rowNum++);
            titleRow.setHeightInPoints(30);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue(roundTitle);
            titleCell.setCellStyle(titleStyle);
            summarySheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 6));

            rowNum++; // 빈 행

            // 차수 메타 정보
            String[][] meta = {
                {"연도", String.valueOf(round.getYear())},
                {"차수", round.getRoundNo() + "차"},
                {"평가일", round.getRoundDate().toString()},
                {"상태", "COMPLETED".equals(round.getStatus().name()) ? "완료" : "진행중"},
                {"총 자산수", String.valueOf(byAsset.size())},
                {"총 평가건수", String.valueOf(items.size())},
                {"고위험", String.valueOf(items.stream().filter(i -> i.getRiskGrade() == RiskAssessment.Grade.HIGH).count())},
                {"중위험", String.valueOf(items.stream().filter(i -> i.getRiskGrade() == RiskAssessment.Grade.MEDIUM).count())},
                {"저위험", String.valueOf(items.stream().filter(i -> i.getRiskGrade() == RiskAssessment.Grade.LOW).count())},
            };
            for (String[] pair : meta) {
                Row r = summarySheet.createRow(rowNum++);
                r.setHeightInPoints(18);
                Cell lbl = r.createCell(0); lbl.setCellValue(pair[0]); lbl.setCellStyle(metaLabelStyle);
                Cell val = r.createCell(1); val.setCellValue(pair[1]); val.setCellStyle(metaValueStyle);
            }

            rowNum++; // 빈 행

            // 자산별 요약 테이블 헤더
            String[] sumHeaders = {"No", "자산명", "자산유형", "위협수", "고위험", "중위험", "저위험"};
            Row sumHeaderRow = summarySheet.createRow(rowNum++);
            sumHeaderRow.setHeightInPoints(22);
            for (int i = 0; i < sumHeaders.length; i++) {
                Cell c = sumHeaderRow.createCell(i);
                c.setCellValue(sumHeaders[i]);
                c.setCellStyle(summaryHeaderStyle);
            }

            // 자산별 요약 데이터
            int seq = 1;
            for (Map.Entry<String, List<RiskAssessment>> entry : byAsset.entrySet()) {
                List<RiskAssessment> assetItems = entry.getValue();
                String assetType = assetItems.stream()
                        .map(i -> nvl(i.getAssetType())).filter(s -> !s.isEmpty()).findFirst().orElse("");
                long high = assetItems.stream().filter(i -> i.getRiskGrade() == RiskAssessment.Grade.HIGH).count();
                long med  = assetItems.stream().filter(i -> i.getRiskGrade() == RiskAssessment.Grade.MEDIUM).count();
                long low  = assetItems.stream().filter(i -> i.getRiskGrade() == RiskAssessment.Grade.LOW).count();

                Row r = summarySheet.createRow(rowNum++);
                r.setHeightInPoints(18);
                Cell c0 = r.createCell(0); c0.setCellValue(seq++); c0.setCellStyle(centerStyle);
                Cell c1 = r.createCell(1); c1.setCellValue(entry.getKey()); c1.setCellStyle(dataStyle);
                Cell c2 = r.createCell(2); c2.setCellValue(assetType); c2.setCellStyle(centerStyle);
                Cell c3 = r.createCell(3); c3.setCellValue(assetItems.size()); c3.setCellStyle(centerStyle);
                Cell c4 = r.createCell(4); c4.setCellValue(high); c4.setCellStyle(high > 0 ? highStyle : centerStyle);
                Cell c5 = r.createCell(5); c5.setCellValue(med);  c5.setCellStyle(med  > 0 ? medStyle  : centerStyle);
                Cell c6 = r.createCell(6); c6.setCellValue(low);  c6.setCellStyle(centerStyle);
            }

            int[] sumColWidths = {6, 28, 14, 8, 8, 8, 8};
            for (int i = 0; i < sumColWidths.length; i++) {
                summarySheet.setColumnWidth(i, sumColWidths[i] * 256);
            }

            // ══════════════════════════════════════════════════════════════
            // 시트 2~N: 자산별 개별 시트
            // ══════════════════════════════════════════════════════════════
            for (Map.Entry<String, List<RiskAssessment>> entry : byAsset.entrySet()) {
                String assetName = entry.getKey();
                List<RiskAssessment> assetItems = entry.getValue();
                String assetType = assetItems.stream()
                        .map(i -> nvl(i.getAssetType())).filter(s -> !s.isEmpty()).findFirst().orElse("");

                String sheetName = sanitizeSheetName(assetName);
                Sheet aSheet = wb.createSheet(sheetName);
                int r = 0;

                // 자산 정보 헤더 행
                Row assetTitleRow = aSheet.createRow(r++);
                assetTitleRow.setHeightInPoints(24);
                Cell atCell = assetTitleRow.createCell(0);
                atCell.setCellValue("자산명: " + assetName + (assetType.isEmpty() ? "" : "  [" + assetType + "]"));
                atCell.setCellStyle(assetHeaderStyle);
                aSheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 9));

                r++; // 빈 행

                // 위협 목록 헤더
                String[] threatHeaders = {"No", "위협명", "위협유형", "취약점", "발생가능성", "영향도", "위험점수", "위험등급", "처리방법", "비고"};
                Row hRow = aSheet.createRow(r++);
                hRow.setHeightInPoints(22);
                for (int i = 0; i < threatHeaders.length; i++) {
                    Cell c = hRow.createCell(i);
                    c.setCellValue(threatHeaders[i]);
                    c.setCellStyle(headerStyle);
                }

                // 위협 데이터 행
                int tSeq = 1;
                for (RiskAssessment item : assetItems) {
                    Row dRow = aSheet.createRow(r++);
                    dRow.setHeightInPoints(18);
                    int col = 0;

                    Cell noCell = dRow.createCell(col++); noCell.setCellValue(tSeq++); noCell.setCellStyle(centerStyle);
                    Cell tnCell = dRow.createCell(col++); tnCell.setCellValue(nvl(item.getThreatName())); tnCell.setCellStyle(dataStyle);
                    Cell ttCell = dRow.createCell(col++); ttCell.setCellValue(nvl(item.getThreatType())); ttCell.setCellStyle(centerStyle);
                    Cell vuCell = dRow.createCell(col++); vuCell.setCellValue(nvl(item.getVulnerability())); vuCell.setCellStyle(dataStyle);
                    Cell liCell = dRow.createCell(col++); liCell.setCellValue(item.getLikelihood()); liCell.setCellStyle(centerStyle);
                    Cell imCell = dRow.createCell(col++); imCell.setCellValue(item.getImpact()); imCell.setCellStyle(centerStyle);
                    int score = item.getLikelihood() * item.getImpact();
                    Cell scCell = dRow.createCell(col++); scCell.setCellValue(score); scCell.setCellStyle(centerStyle);

                    String grade = item.getRiskGrade().name();
                    String gradeLabel = "HIGH".equals(grade) ? "고위험" : "MEDIUM".equals(grade) ? "중위험" : "저위험";
                    CellStyle gradeStyle = "HIGH".equals(grade) ? highStyle : "MEDIUM".equals(grade) ? medStyle : lowStyle;
                    Cell grCell = dRow.createCell(col++); grCell.setCellValue(gradeLabel); grCell.setCellStyle(gradeStyle);

                    Cell trCell = dRow.createCell(col++);
                    trCell.setCellValue(item.getTreatment() != null ? item.getTreatment().name() : "");
                    trCell.setCellStyle(centerStyle);

                    Cell ntCell = dRow.createCell(col); ntCell.setCellValue(nvl(item.getNotes())); ntCell.setCellStyle(dataStyle);
                }

                int[] assetColWidths = {6, 26, 14, 28, 10, 8, 9, 9, 9, 28};
                for (int i = 0; i < assetColWidths.length; i++) {
                    aSheet.setColumnWidth(i, assetColWidths[i] * 256);
                }
            }

            wb.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Excel 생성 실패", e);
        }
    }

    private String sanitizeSheetName(String name) {
        if (name == null || name.isBlank()) name = "미분류";
        // Excel 시트명 금지 문자 제거, 최대 31자
        String sanitized = name.replaceAll("[\\\\/*?:\\[\\]]", "_");
        return sanitized.length() > 31 ? sanitized.substring(0, 31) : sanitized;
    }

    private void setBorder(CellStyle style) {
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
    }

    private String nvl(String s) {
        return s != null ? s : "";
    }

    private RiskAssessment.Grade calcGrade(int score) {
        if (score >= 15) return RiskAssessment.Grade.HIGH;
        if (score >= 8) return RiskAssessment.Grade.MEDIUM;
        return RiskAssessment.Grade.LOW;
    }
}
