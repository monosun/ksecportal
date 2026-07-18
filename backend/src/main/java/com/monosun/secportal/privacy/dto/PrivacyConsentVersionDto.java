package com.monosun.secportal.privacy.dto;

import com.monosun.secportal.privacy.entity.PrivacyConsentVersion;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class PrivacyConsentVersionDto {

    @Getter
    @Setter
    public static class CreateRequest {
        // 버전은 서버에서 자동 증가하므로 클라이언트가 지정하지 않는다.
        private LocalDate effectiveDate;
        private String changeNote;
    }

    @Getter
    @Builder
    public static class Response {
        private Long id;
        private String version;
        private LocalDate effectiveDate;
        private String changeNote;
        private String fileName;
        private Long fileSize;
        private boolean hasFile;
        private String author;
        private LocalDateTime createdAt;

        public static Response from(PrivacyConsentVersion v) {
            return Response.builder()
                    .id(v.getId())
                    .version(v.getVersion())
                    .effectiveDate(v.getEffectiveDate())
                    .changeNote(v.getChangeNote())
                    .fileName(v.getFileName())
                    .fileSize(v.getFileSize())
                    .hasFile(v.getFilePath() != null)
                    .author(v.getAuthor() != null ? v.getAuthor().getName() : null)
                    .createdAt(v.getCreatedAt())
                    .build();
        }
    }
}
