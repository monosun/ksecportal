package com.monosun.secportal.policydoc.dto;

import com.monosun.secportal.policydoc.entity.PolicyDoc;
import com.monosun.secportal.policydoc.entity.PolicyDocRevision;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public class PolicyDocDto {

    /** 문서 기본정보 */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DocRequest {
        private String docNo;
        private String title;
        private String category;
        private String department;
        private String owner;
        private String description;
    }

    /** 신규 문서 등록 — 기본정보 + 최초 버전(보통 제정) */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateRequest {
        private DocRequest doc;
        private RevisionRequest revision;
    }

    /** 버전(개정본) 등록·수정 */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RevisionRequest {
        private String version;
        private String revisionType;
        private LocalDate revisionDate;
        private LocalDate effectiveDate;
        private String approver;
        private String changeReason;
        private String changeDetails;
    }

    @Getter
    @Builder
    public static class RevisionResponse {
        private Long id;
        private Long docId;
        private String version;
        private String revisionType;
        private LocalDate revisionDate;
        private LocalDate effectiveDate;
        private String approver;
        private String changeReason;
        private String changeDetails;
        private String fileName;
        private Long fileSize;
        private String uploaderName;
        private boolean current;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        public static RevisionResponse from(PolicyDocRevision r, boolean current) {
            return RevisionResponse.builder()
                    .id(r.getId())
                    .docId(r.getDocument().getId())
                    .version(r.getVersion())
                    .revisionType(r.getRevisionType().name())
                    .revisionDate(r.getRevisionDate())
                    .effectiveDate(r.getEffectiveDate())
                    .approver(r.getApprover())
                    .changeReason(r.getChangeReason())
                    .changeDetails(r.getChangeDetails())
                    .fileName(r.getFileName())
                    .fileSize(r.getFileSize())
                    .uploaderName(r.getUploader() != null ? r.getUploader().getName() : null)
                    .current(current)
                    .createdAt(r.getCreatedAt())
                    .updatedAt(r.getUpdatedAt())
                    .build();
        }
    }

    @Getter
    @Builder
    public static class Response {
        private Long id;
        private String docNo;
        private String title;
        private String category;
        private String department;
        private String owner;
        private String description;
        private String status;
        private String registrantName;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
        private int revisionCount;
        /** 현행본 — 개정본이 없으면 null */
        private RevisionResponse current;
        /** 상세 조회에서만 채운다 (최신순) */
        private List<RevisionResponse> revisions;

        /** @param revisions 이 문서의 개정본 (최신순) */
        public static Response from(PolicyDoc d, List<PolicyDocRevision> revisions, boolean withHistory) {
            List<RevisionResponse> mapped = new java.util.ArrayList<>();
            for (int i = 0; i < revisions.size(); i++) {
                mapped.add(RevisionResponse.from(revisions.get(i), i == 0));
            }
            return Response.builder()
                    .id(d.getId())
                    .docNo(d.getDocNo())
                    .title(d.getTitle())
                    .category(d.getCategory().name())
                    .department(d.getDepartment())
                    .owner(d.getOwner())
                    .description(d.getDescription())
                    .status(d.getStatus().name())
                    .registrantName(d.getRegistrant() != null ? d.getRegistrant().getName() : null)
                    .createdAt(d.getCreatedAt())
                    .updatedAt(d.getUpdatedAt())
                    .revisionCount(revisions.size())
                    .current(mapped.isEmpty() ? null : mapped.get(0))
                    .revisions(withHistory ? mapped : null)
                    .build();
        }
    }
}
