package com.monosun.secportal.privacy.dto;

import com.monosun.secportal.privacy.entity.PrivacySafeguard;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class PrivacySafeguardDto {

    @Getter
    @Setter
    public static class Request {
        private String safeguardType;
        @NotBlank @Size(max = 200)
        private String title;
        @Size(max = 200)
        private String targetSystem;
        @Size(max = 100)
        private String department;
        private LocalDate checkDate;
        @Size(max = 100)
        private String performer;
        private Integer targetCount;
        private Integer actionCount;
        private String result;
        private String findings;
        private String improvement;
        private LocalDate nextCheckDate;
        private String status;
        private String notes;
    }

    @Getter
    @Builder
    public static class Response {
        private Long id;
        private String safeguardType;
        private String title;
        private String targetSystem;
        private String department;
        private LocalDate checkDate;
        private String performer;
        private Integer targetCount;
        private Integer actionCount;
        private String result;
        private String findings;
        private String improvement;
        private LocalDate nextCheckDate;
        private String status;
        private String notes;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        public static Response from(PrivacySafeguard s) {
            return Response.builder()
                    .id(s.getId())
                    .safeguardType(s.getSafeguardType().name())
                    .title(s.getTitle())
                    .targetSystem(s.getTargetSystem())
                    .department(s.getDepartment())
                    .checkDate(s.getCheckDate())
                    .performer(s.getPerformer())
                    .targetCount(s.getTargetCount())
                    .actionCount(s.getActionCount())
                    .result(s.getResult())
                    .findings(s.getFindings())
                    .improvement(s.getImprovement())
                    .nextCheckDate(s.getNextCheckDate())
                    .status(s.getStatus().name())
                    .notes(s.getNotes())
                    .createdAt(s.getCreatedAt())
                    .updatedAt(s.getUpdatedAt())
                    .build();
        }
    }
}
