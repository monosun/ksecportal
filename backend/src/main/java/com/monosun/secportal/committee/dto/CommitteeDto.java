package com.monosun.secportal.committee.dto;

import com.monosun.secportal.committee.entity.CommitteeFile;
import com.monosun.secportal.committee.entity.CommitteeMeeting;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

public class CommitteeDto {

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MeetingRequest {
        private int year;
        private int sessionNo;
        private String title;
        private LocalDate meetingDate;
        private String location;
        private String attendees;
        private String status;
    }

    @Getter
    @Builder
    public static class MeetingResponse {
        private Long id;
        private int year;
        private int sessionNo;
        private String title;
        private LocalDate meetingDate;
        private String location;
        private String attendees;
        private String status;
        private String createdByName;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
        private List<FileResponse> files;

        public static MeetingResponse from(CommitteeMeeting m) {
            return MeetingResponse.builder()
                    .id(m.getId())
                    .year(m.getYear())
                    .sessionNo(m.getSessionNo())
                    .title(m.getTitle())
                    .meetingDate(m.getMeetingDate())
                    .location(m.getLocation())
                    .attendees(m.getAttendees())
                    .status(m.getStatus().name())
                    .createdByName(m.getCreatedBy() != null ? m.getCreatedBy().getName() : null)
                    .createdAt(m.getCreatedAt())
                    .updatedAt(m.getUpdatedAt())
                    .files(m.getFiles().stream().map(FileResponse::from).collect(Collectors.toList()))
                    .build();
        }
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FileRequest {
        private String fileType;
        private String title;
    }

    @Getter
    @Builder
    public static class FileResponse {
        private Long id;
        private String fileType;
        private String title;
        private String fileName;
        private Long fileSize;
        private String uploaderName;
        private LocalDateTime createdAt;

        public static FileResponse from(CommitteeFile f) {
            return FileResponse.builder()
                    .id(f.getId())
                    .fileType(f.getFileType().name())
                    .title(f.getTitle())
                    .fileName(f.getFileName())
                    .fileSize(f.getFileSize())
                    .uploaderName(f.getUploader() != null ? f.getUploader().getName() : null)
                    .createdAt(f.getCreatedAt())
                    .build();
        }
    }
}
