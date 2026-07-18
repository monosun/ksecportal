package com.monosun.secportal.privacy.dto;

import com.monosun.secportal.privacy.entity.PrivacyDpiaFile;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

public class PrivacyDpiaFileDto {

    @Getter
    @Setter
    public static class CreateRequest {
        /** REPORT / IMPROVEMENT / CHECKLIST / OTHER */
        private String category;
        private String title;
    }

    @Getter
    @Builder
    public static class Response {
        private Long id;
        private String category;
        private String title;
        private String fileName;
        private Long fileSize;
        private String uploader;
        private LocalDateTime createdAt;

        public static Response from(PrivacyDpiaFile f) {
            return Response.builder()
                    .id(f.getId())
                    .category(f.getCategory())
                    .title(f.getTitle())
                    .fileName(f.getFileName())
                    .fileSize(f.getFileSize())
                    .uploader(f.getUploader() != null ? f.getUploader().getName() : null)
                    .createdAt(f.getCreatedAt())
                    .build();
        }
    }
}
