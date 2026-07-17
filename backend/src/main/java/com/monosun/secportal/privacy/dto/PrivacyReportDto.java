package com.monosun.secportal.privacy.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/** 개인정보 현황보고서 — 경영진 보고 및 ISMS-P 심사 대응용 집계 */
public class PrivacyReportDto {

    @Getter
    @Builder
    public static class Summary {
        private LocalDate generatedAt;

        /** 개인정보 처리현황 */
        private Section processing;
        /** 개인정보파일 현황 */
        private FileSection files;
        /** 수탁사 현황 */
        private ContractorSection contractors;
        /** 제3자 제공 현황 */
        private ProvisionSection provisions;
        /** 보유기간 현황 */
        private RetentionSection retentions;
        /** 파기 현황 */
        private DisposalSection disposals;
        /** 권리행사 현황 */
        private RightsSection rights;
        /** 유출사고 현황 */
        private BreachSection breaches;
        /** 법령 준수현황 — DPIA·보호조치 이행 */
        private ComplianceSection compliance;
    }

    @Getter
    @Builder
    public static class Section {
        private long total;
        private long active;
        private long inactive;
    }

    @Getter
    @Builder
    public static class FileSection {
        private long total;
        private long active;
        private long sensitive;          // 민감정보 포함 파일 수
        private long uniqueIdentifier;   // 고유식별정보 포함 파일 수
    }

    @Getter
    @Builder
    public static class ContractorSection {
        private long total;
        private long checked;            // 점검 이력이 있는 수탁사 수
        private long unchecked;          // 점검 이력이 없는 수탁사 수
    }

    @Getter
    @Builder
    public static class ProvisionSection {
        private long total;
        private long thirdParty;
        private long jointUse;
        private long overseas;
    }

    @Getter
    @Builder
    public static class RetentionSection {
        private long total;
        private long expiringIn30Days;   // 30일 내 만료예정
        private long overdue;            // 만료됐으나 미파기
        private long disposed;
    }

    @Getter
    @Builder
    public static class DisposalSection {
        private long total;
        private long planned;
        private long completed;
        private long pendingApproval;
    }

    @Getter
    @Builder
    public static class RightsSection {
        private long total;
        private long inProgress;
        private long completed;
        private long slaBreached;        // 처리기한 초과
        private Map<String, Long> byType;
    }

    @Getter
    @Builder
    public static class BreachSection {
        private long total;
        private long open;               // 종결되지 않은 사고
        private long reportOverdue;      // 신고기한 경과·미신고
        private long affectedSubjects;   // 유출 정보주체 총계
    }

    @Getter
    @Builder
    public static class ComplianceSection {
        private long dpiaTotal;
        private long dpiaCompleted;
        private long dpiaHighRisk;
        private long safeguardTotal;
        private long safeguardCompleted;
        private List<TypeCount> safeguardByType;
    }

    @Getter
    @Builder
    public static class TypeCount {
        private String type;
        private long count;
    }
}
