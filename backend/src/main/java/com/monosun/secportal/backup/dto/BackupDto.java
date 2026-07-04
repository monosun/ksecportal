package com.monosun.secportal.backup.dto;

import com.monosun.secportal.backup.entity.BackupHistory;
import lombok.*;

import java.time.LocalDateTime;

public class BackupDto {

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DownloadRequest {
        private String password;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Config {
        private boolean enabled;
        private String cron;
        private int keepCount;
        private boolean defaultPasswordSet;
        private String defaultPassword;
    }

    @Getter
    @Builder
    public static class HistoryResponse {
        private Long id;
        private String filename;
        private Long fileSize;
        private String backupType;
        private String status;
        private String message;
        private LocalDateTime createdAt;

        public static HistoryResponse from(BackupHistory h) {
            return HistoryResponse.builder()
                    .id(h.getId())
                    .filename(h.getFilename())
                    .fileSize(h.getFileSize())
                    .backupType(h.getBackupType())
                    .status(h.getStatus())
                    .message(h.getMessage())
                    .createdAt(h.getCreatedAt())
                    .build();
        }
    }

    @Getter
    @Builder
    public static class FileInfo {
        private String filename;
        private long fileSize;
        private LocalDateTime lastModified;
    }
}
