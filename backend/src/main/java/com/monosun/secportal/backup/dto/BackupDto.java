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

    /** 서버 저장 백업 파일 다운로드 요청 */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FileDownloadRequest {
        /** 백업 파일 생성 시 사용한 비밀번호 (항상 검증) */
        private String password;
        /** true 면 복호화된 JSON 으로, false 면 암호화된 원본(.bak)으로 내려받는다 */
        private boolean decrypt;
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
