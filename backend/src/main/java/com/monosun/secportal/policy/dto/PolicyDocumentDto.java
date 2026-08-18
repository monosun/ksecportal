package com.monosun.secportal.policy.dto;

import com.monosun.secportal.policy.entity.Policy;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

/** 지침 문서(PDF/DOCX/TXT/MD) 등록 관련 DTO */
public class PolicyDocumentDto {

    /** 개별 정책 — 문서에서 뽑은 제목·본문 초안(저장 전) */
    @Getter
    @Builder
    public static class ExtractResult {
        private String title;
        private String content;
        /** 본문에서 찾은 조(條) 개수 — 0 이면 전문 한 건으로 등록된다 */
        private int articleCount;
        /** 본문에서 찾은 장(章) 개수 — 2 이상이면 '지침 문서 등록' 을 권한다 */
        private int chapterCount;
        private List<String> warnings;
    }

    /** 지침 문서 등록 옵션 — 장별로 만들어질 정책에 공통 적용된다. */
    @Getter
    @Setter
    public static class ImportOptions {
        /** 비워두면 문서 제목 줄 또는 파일명에서 추측한다. */
        private String guidelineName;
        private Policy.Category category;
        private Policy.Status status;
        private String version;
        private LocalDate effectiveDate;
        /** true 면 저장하지 않고 결과만 미리 보여준다. */
        private boolean dryRun;
    }

    /** 장 하나에 대응하는 등록 결과(또는 미리보기) */
    @Getter
    @Builder
    public static class ChapterDraft {
        private String title;
        private String chapterLabel;
        private String chapterTitle;
        private int articleCount;
        private int contentLength;
        /** 같은 제목의 정책이 이미 있어 본문을 갱신하는 경우 true */
        private boolean existing;
        private Long existingPolicyId;
    }

    @Getter
    @Builder
    public static class ImportResult {
        private String guidelineName;
        private String fileName;
        private boolean dryRun;
        private int created;
        private int updated;
        /** 장 전체에서 찾은 조 개수 합계 */
        private int articleCount;
        private List<ChapterDraft> chapters;
        private List<String> warnings;
    }
}
