package com.monosun.secportal.privacy.dto;

import com.monosun.secportal.privacy.entity.Contractor;
import com.monosun.secportal.privacy.entity.ContractorInspection;
import com.monosun.secportal.privacy.entity.ContractorInspectionFile;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public class ContractorDto {

    @Getter
    public static class ContractorRequest {
        @NotBlank @Size(max = 200)
        private String name;
        @Size(max = 20)
        private String businessNumber;
        @Size(max = 100)
        private String representative;
        @Size(max = 200)
        private String serviceType;
        private LocalDate contractStart;
        private LocalDate contractEnd;
        @Size(max = 100)
        private String contactPerson;
        @Size(max = 200)
        private String contactEmail;
        @Size(max = 50)
        private String contactPhone;
        private String status;
        private String notes;
    }

    @Getter
    @Builder
    public static class ContractorResponse {
        private Long id;
        private String name;
        private String businessNumber;
        private String representative;
        private String serviceType;
        private LocalDate contractStart;
        private LocalDate contractEnd;
        private String contactPerson;
        private String contactEmail;
        private String contactPhone;
        private String status;
        private String notes;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
        private List<InspectionResponse> inspections;
        private Long checkCount;   // 연도별 점검(ContractorCheck) 건수 — 목록 카드 표시용

        public static ContractorResponse from(Contractor c, long checkCount) {
            ContractorResponse r = from(c);
            r.checkCount = checkCount;
            return r;
        }

        public static ContractorResponse from(Contractor c) {
            return ContractorResponse.builder()
                    .id(c.getId())
                    .name(c.getName())
                    .businessNumber(c.getBusinessNumber())
                    .representative(c.getRepresentative())
                    .serviceType(c.getServiceType())
                    .contractStart(c.getContractStart())
                    .contractEnd(c.getContractEnd())
                    .contactPerson(c.getContactPerson())
                    .contactEmail(c.getContactEmail())
                    .contactPhone(c.getContactPhone())
                    .status(c.getStatus().name())
                    .notes(c.getNotes())
                    .createdAt(c.getCreatedAt())
                    .updatedAt(c.getUpdatedAt())
                    .inspections(c.getInspections().stream()
                            .map(InspectionResponse::from).toList())
                    .build();
        }

        public static ContractorResponse fromWithoutInspections(Contractor c) {
            return ContractorResponse.builder()
                    .id(c.getId())
                    .name(c.getName())
                    .businessNumber(c.getBusinessNumber())
                    .representative(c.getRepresentative())
                    .serviceType(c.getServiceType())
                    .contractStart(c.getContractStart())
                    .contractEnd(c.getContractEnd())
                    .contactPerson(c.getContactPerson())
                    .contactEmail(c.getContactEmail())
                    .contactPhone(c.getContactPhone())
                    .status(c.getStatus().name())
                    .notes(c.getNotes())
                    .createdAt(c.getCreatedAt())
                    .updatedAt(c.getUpdatedAt())
                    .build();
        }
    }

    @Getter
    public static class InspectionRequest {
        private LocalDate inspectionDate;
        @Size(max = 100)
        private String inspector;
        private String status;
        private String result;
        private String notes;
    }

    @Getter
    @Builder
    public static class InspectionResponse {
        private Long id;
        private Long contractorId;
        private LocalDate inspectionDate;
        private String inspector;
        private String status;
        private String result;
        private String notes;
        private LocalDateTime createdAt;
        private List<FileResponse> files;

        public static InspectionResponse from(ContractorInspection i) {
            return InspectionResponse.builder()
                    .id(i.getId())
                    .contractorId(i.getContractor().getId())
                    .inspectionDate(i.getInspectionDate())
                    .inspector(i.getInspector())
                    .status(i.getStatus().name())
                    .result(i.getResult())
                    .notes(i.getNotes())
                    .createdAt(i.getCreatedAt())
                    .files(i.getFiles().stream().map(FileResponse::from).toList())
                    .build();
        }
    }

    @Getter
    @Builder
    public static class FileResponse {
        private Long id;
        private String title;
        private String fileName;
        private Long fileSize;
        private LocalDateTime createdAt;

        public static FileResponse from(ContractorInspectionFile f) {
            return FileResponse.builder()
                    .id(f.getId())
                    .title(f.getTitle())
                    .fileName(f.getFileName())
                    .fileSize(f.getFileSize())
                    .createdAt(f.getCreatedAt())
                    .build();
        }
    }
}
