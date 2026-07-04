package com.monosun.secportal.secdoc.dto;

import com.monosun.secportal.secdoc.entity.SecDoc;
import lombok.*;

import java.time.LocalDateTime;

public class SecDocDto {

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateRequest {
        private String title;
        private String description;
        private String category;
        private String version;
        private String producingOrg;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class NewVersionRequest {
        private String version;
        private String description;
    }

    @Getter
    @Builder
    public static class Response {
        private Long id;
        private String documentKey;
        private String title;
        private String description;
        private String category;
        private String version;
        private boolean latest;
        private String fileName;
        private Long fileSize;
        private String producingOrg;
        private String uploaderName;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
        private int versionCount;

        public static Response from(SecDoc doc) {
            return from(doc, 0);
        }

        public static Response from(SecDoc doc, int versionCount) {
            return Response.builder()
                    .id(doc.getId())
                    .documentKey(doc.getDocumentKey())
                    .title(doc.getTitle())
                    .description(doc.getDescription())
                    .category(doc.getCategory().name())
                    .version(doc.getVersion())
                    .latest(doc.isLatest())
                    .fileName(doc.getFileName())
                    .fileSize(doc.getFileSize())
                    .producingOrg(doc.getProducingOrg())
                    .uploaderName(doc.getUploader() != null ? doc.getUploader().getName() : null)
                    .createdAt(doc.getCreatedAt())
                    .updatedAt(doc.getUpdatedAt())
                    .versionCount(versionCount)
                    .build();
        }
    }
}
