package com.monosun.secportal.isms.dto;

import com.monosun.secportal.isms.entity.IsmsEvidence;
import com.monosun.secportal.isms.entity.IsmsItem;
import com.monosun.secportal.isms.entity.IsmsItemNote;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

public class IsmsDto {

    /**
     * 통제항목에 매핑된 정책 참조.
     *
     * <p>{@code articleId} 가 null 이면 <b>장(章) 전체</b> 매핑, 값이 있으면 그 장의
     * <b>조(條) 단위</b> 매핑이다. 화면에서 장 기준으로 묶어 보여줄 수 있도록
     * 조 매핑도 소속 장 정보를 함께 싣는다.
     */
    @Getter
    @Builder
    public static class PolicyRef {
        /** 소속 장(정책) id */
        private Long id;
        private String title;
        private String status;
        private String category;
        private String guidelineName;
        private String chapterLabel;
        private String chapterTitle;
        /** 조 단위 매핑일 때만 채워진다 */
        private Long articleId;
        private String articleLabel;
        private String articleTitle;
        /** "제31조(접속기록의 보관 및 점검)" 형태의 표시용 이름 */
        private String articleDisplayName;
    }

    @Getter
    @Builder
    public static class ItemResponse {
        private Long id;
        private String itemCode;
        private String itemName;
        private String domainCode;
        private String domainName;
        private int sectionNum;
        private String sectionName;
        private String description;
        private String guide;
        private String defaultEvidenceTitle;
        private String defaultEvidenceContent;
        private String evidenceExamples;
        private int sortOrder;
        private long evidenceCount;
        private String latestStatus;
        private List<PolicyRef> mappedPolicies;

        public static ItemResponse from(IsmsItem item) {
            return ItemResponse.builder()
                    .id(item.getId())
                    .itemCode(item.getItemCode())
                    .itemName(item.getItemName())
                    .domainCode(item.getDomainCode())
                    .domainName(item.getDomainName())
                    .sectionNum(item.getSectionNum())
                    .sectionName(item.getSectionName())
                    .description(item.getDescription())
                    .guide(item.getGuide())
                    .defaultEvidenceTitle(item.getDefaultEvidenceTitle())
                    .defaultEvidenceContent(item.getDefaultEvidenceContent())
                    .evidenceExamples(item.getEvidenceExamples())
                    .sortOrder(item.getSortOrder())
                    .mappedPolicies(List.of())
                    .build();
        }

        public static ItemResponse from(IsmsItem item, long evidenceCount, String latestStatus,
                                        List<PolicyRef> mappedPolicies) {
            return ItemResponse.builder()
                    .id(item.getId())
                    .itemCode(item.getItemCode())
                    .itemName(item.getItemName())
                    .domainCode(item.getDomainCode())
                    .domainName(item.getDomainName())
                    .sectionNum(item.getSectionNum())
                    .sectionName(item.getSectionName())
                    .description(item.getDescription())
                    .guide(item.getGuide())
                    .defaultEvidenceTitle(item.getDefaultEvidenceTitle())
                    .defaultEvidenceContent(item.getDefaultEvidenceContent())
                    .evidenceExamples(item.getEvidenceExamples())
                    .sortOrder(item.getSortOrder())
                    .evidenceCount(evidenceCount)
                    .latestStatus(latestStatus)
                    .mappedPolicies(mappedPolicies != null ? mappedPolicies : List.of())
                    .build();
        }
    }

    @Getter
    @Builder
    public static class EvidenceResponse {
        private Long id;
        private Long itemId;
        private String itemCode;
        private String itemName;
        private int year;
        private String title;
        private String content;
        private String fileName;
        private String filePath;
        private String status;
        private Long registrantId;
        private String registrantName;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
        private Long sourceEvidenceId;
        private String sourceItemCode;
        private String sourceItemName;
        private String sourceEvidenceTitle;
        private String sourceFileName;

        public static EvidenceResponse from(IsmsEvidence e) {
            IsmsEvidence src = e.getSourceEvidence();
            return EvidenceResponse.builder()
                    .id(e.getId())
                    .itemId(e.getItem().getId())
                    .itemCode(e.getItem().getItemCode())
                    .itemName(e.getItem().getItemName())
                    .year(e.getYear())
                    .title(e.getTitle())
                    .content(e.getContent())
                    .fileName(src != null ? src.getFileName() : e.getFileName())
                    .filePath(src != null ? src.getFilePath() : e.getFilePath())
                    .status(e.getStatus().name())
                    .registrantId(e.getRegistrant() != null ? e.getRegistrant().getId() : null)
                    .registrantName(e.getRegistrant() != null ? e.getRegistrant().getName() : null)
                    .createdAt(e.getCreatedAt())
                    .updatedAt(e.getUpdatedAt())
                    .sourceEvidenceId(src != null ? src.getId() : null)
                    .sourceItemCode(src != null ? src.getItem().getItemCode() : null)
                    .sourceItemName(src != null ? src.getItem().getItemName() : null)
                    .sourceEvidenceTitle(src != null ? src.getTitle() : null)
                    .sourceFileName(src != null ? src.getFileName() : null)
                    .build();
        }
    }

    @Getter
    @Builder
    public static class EvidenceSearchResult {
        private Long id;
        private String itemCode;
        private String itemName;
        private String title;
        private String fileName;
        private String status;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class EvidenceCreateRequest {
        @NotNull
        private Integer year;
        @NotBlank
        private String title;
        private String content;
        private String status;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class EvidenceRefRequest {
        @NotNull
        private Integer year;
        @NotBlank
        private String title;
        private String status;
        @NotNull
        private Long sourceEvidenceId;
    }

    /** 항목별 의견·현재 상태 (연도별) */
    @Getter
    @Builder
    public static class ItemNoteResponse {
        private Long itemId;
        private int year;
        private String statusNote;
        private String opinion;
        private String updaterName;
        private LocalDateTime updatedAt;

        public static ItemNoteResponse empty(Long itemId, int year) {
            return ItemNoteResponse.builder().itemId(itemId).year(year).build();
        }

        public static ItemNoteResponse from(IsmsItemNote n) {
            return ItemNoteResponse.builder()
                    .itemId(n.getItem().getId())
                    .year(n.getYear())
                    .statusNote(n.getStatusNote())
                    .opinion(n.getOpinion())
                    .updaterName(n.getUpdater() != null ? n.getUpdater().getName() : null)
                    .updatedAt(n.getUpdatedAt())
                    .build();
        }
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class ItemNoteRequest {
        private String statusNote;
        private String opinion;
    }

    /** 항목 이행 가이드 (연도 무관) */
    @Getter
    @Setter
    @NoArgsConstructor
    public static class ItemGuideRequest {
        private String guide;
    }

    /** 코드관리 'ISMS-P 101항목' 탭 — 항목별 기본 증적제목·증적내용·이행가이드 편집 */
    @Getter
    @Setter
    @NoArgsConstructor
    public static class ItemDefaultsRequest {
        private String defaultEvidenceTitle;
        private String defaultEvidenceContent;
        private String evidenceExamples;
        private String guide;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class EvidenceUpdateRequest {
        private String title;
        private String content;
        private String status;
    }

    @Getter
    @Builder
    public static class DomainSummary {
        private String domainCode;
        private String domainName;
        private int sectionNum;
        private int total;
        private int compliant;
        private int partial;
        private int nonCompliant;
        private int na;
        private int noEvidence;
    }

    @Getter
    @Builder
    public static class BulkImportResult {
        private int total;
        private int success;
        private int failed;
        private List<RowError> errors;

        @Getter
        @Builder
        public static class RowError {
            private int row;
            private String itemCode;
            private String message;
        }
    }

    /** 전년도 증적 가져오기 버튼 상태 — 가져올 수 있는 연도와 되돌릴 수 있는 건수 */
    @Getter
    @Builder
    public static class CopyPreviousStatus {
        /** 대상 연도 이전에 증적이 있는 가장 최근 연도 (없으면 null → 가져오기 불가) */
        private Integer previousYear;
        /** 대상 연도에 남아 있는 가져오기 증적 건수 (0 이면 초기화 불가) */
        private long copiedEvidences;
        private long copiedNotes;
        /** 대상 연도의 가져오기 증적이 어느 연도에서 왔는지 */
        private Integer copiedFromYear;
    }

    /** 전년도 증적 가져오기 초기화(되돌리기) 결과 */
    @Getter
    @Builder
    public static class RevertCopyResult {
        private int targetYear;
        private Integer copiedFromYear;
        /** 삭제된 가져오기 증적 건수 */
        private int removedEvidences;
        /** 삭제된 가져오기 현재상태·의견 건수 */
        private int removedNotes;
        /** 가져온 증적을 참조하고 있어 함께 삭제된 증적 건수 */
        private int removedReferences;
    }

    /** 전년도 증적 가져오기 결과 */
    @Getter
    @Builder
    public static class CopyPreviousResult {
        /** 실제로 가져온 원본 연도 */
        private int sourceYear;
        private int targetYear;
        /** 복사된 증적 건수 */
        private int copiedEvidences;
        /** 복사된 현재상태·의견 건수 */
        private int copiedNotes;
        /** 대상 연도에 이미 증적이 있어 건너뛴 항목 수 */
        private int skippedItems;
    }

    @Getter
    @Builder
    public static class SummaryResponse {
        private int year;
        private int totalItems;
        private int compliant;
        private int partial;
        private int nonCompliant;
        private int na;
        private int noEvidence;
        private List<DomainSummary> byDomain;
    }
}
