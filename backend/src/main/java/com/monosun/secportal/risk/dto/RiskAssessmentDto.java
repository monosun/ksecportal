package com.monosun.secportal.risk.dto;

import com.monosun.secportal.risk.entity.RiskAssessment;
import com.monosun.secportal.risk.entity.RiskAssessmentRound;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class RiskAssessmentDto {

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RoundRequest {
        @NotNull private Integer year;
        @NotNull private Integer roundNo;
        @NotNull private LocalDate roundDate;
        private String title;
        private String status; // IN_PROGRESS or COMPLETED
    }

    @Getter
    @Builder
    public static class RoundResponse {
        private Long id;
        private int year;
        private int roundNo;
        private LocalDate roundDate;
        private String title;
        private String status;
        private long itemCount;
        private LocalDateTime createdAt;

        public static RoundResponse from(RiskAssessmentRound r, long itemCount) {
            return RoundResponse.builder()
                    .id(r.getId())
                    .year(r.getYear())
                    .roundNo(r.getRoundNo())
                    .roundDate(r.getRoundDate())
                    .title(r.getTitle())
                    .status(r.getStatus().name())
                    .itemCount(itemCount)
                    .createdAt(r.getCreatedAt())
                    .build();
        }
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AssessmentRequest {
        @NotNull private Long roundId;
        private Long assetId;
        private String assetName;
        private String assetType;
        private String assetEnvironment;
        private Long threatId;
        private String threatName;
        private String threatType;
        private String vulnerability;
        private int likelihood = 3;
        private int impact = 3;
        private String treatment; // 수용/감소/회피/이전
        private String notes;
    }

    @Getter
    @Builder
    public static class AssessmentResponse {
        private Long id;
        private Long roundId;
        private Long assetId;
        private String assetName;
        private String assetType;
        private String assetEnvironment;
        private Long threatId;
        private String threatName;
        private String threatType;
        private String vulnerability;
        private int likelihood;
        private int impact;
        private int riskScore;
        private String riskGrade;
        private String treatment;
        private String notes;
        // 위험 처리 계획
        private String plan;
        private String planAssignee;
        private LocalDate planDueDate;
        private int planProgress;
        private String planStatus;
        private LocalDateTime createdAt;

        public static AssessmentResponse from(RiskAssessment a) {
            int score = a.getLikelihood() * a.getImpact();
            return AssessmentResponse.builder()
                    .id(a.getId())
                    .roundId(a.getRoundId())
                    .assetId(a.getAssetId())
                    .assetName(a.getAssetName())
                    .assetType(a.getAssetType())
                    .assetEnvironment(a.getAssetEnvironment())
                    .threatId(a.getThreatId())
                    .threatName(a.getThreatName())
                    .threatType(a.getThreatType())
                    .vulnerability(a.getVulnerability())
                    .likelihood(a.getLikelihood())
                    .impact(a.getImpact())
                    .riskScore(score)
                    .riskGrade(a.getRiskGrade().name())
                    .treatment(a.getTreatment() != null ? a.getTreatment().name() : null)
                    .notes(a.getNotes())
                    .plan(a.getPlan())
                    .planAssignee(a.getPlanAssignee())
                    .planDueDate(a.getPlanDueDate())
                    .planProgress(a.getPlanProgress() != null ? a.getPlanProgress() : 0)
                    .planStatus(a.getPlanStatus() != null ? a.getPlanStatus() : "계획중")
                    .createdAt(a.getCreatedAt())
                    .build();
        }
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TreatmentPlanRequest {
        private String plan;
        private String planAssignee;
        private LocalDate planDueDate;
        private Integer planProgress;
        private String planStatus; // 계획중/진행중/완료/기한초과
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BulkTreatmentRequest {
        @NotNull private List<Long> ids;
        @NotNull private String treatment;
    }

    @Getter
    @Builder
    public static class YearsResponse {
        private List<Integer> years;
    }
}
