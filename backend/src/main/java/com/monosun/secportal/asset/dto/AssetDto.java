package com.monosun.secportal.asset.dto;

import com.monosun.secportal.asset.entity.Asset;
import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class AssetDto {

    @Getter
    public static class CreateRequest {
        @NotBlank private String name;
        private Asset.AssetCategory assetCategory;
        @NotBlank private String type;
        private String description;

        private String owner;
        private String department;
        private String operator;

        private Asset.Environment environment;
        private String location;
        private Asset.Status status;

        private Asset.CloudProvider cloudProvider;
        private String cloudResourceId;
        private String region;
        private String ipAddress;
        private String osType;
        private String spec;

        private Asset.Criticality criticality;
        private Asset.Criticality confidentiality;
        private Asset.Criticality integrity;
        private Asset.Criticality availability;

        private Boolean personalInfoIncluded;
        private String personalInfoType;
        private Boolean personalInfoProcessing;

        private String linkedSystems;

        private Boolean accessControlTarget;
        private Boolean backupTarget;
        private Boolean logManagementTarget;

        private BigDecimal monthlyCost;
        private LocalDate contractExpiry;
        private LocalDate lastInspectionDate;
        private LocalDate nextInspectionDate;
        private LocalDate lastReviewDate;
        private String remarks;
    }

    @Getter
    public static class UpdateRequest {
        private String name;
        private Asset.AssetCategory assetCategory;
        private String type;
        private String description;

        private String owner;
        private String department;
        private String operator;

        private Asset.Environment environment;
        private String location;
        private Asset.Status status;

        private Asset.CloudProvider cloudProvider;
        private String cloudResourceId;
        private String region;
        private String ipAddress;
        private String osType;
        private String spec;

        private Asset.Criticality criticality;
        private Asset.Criticality confidentiality;
        private Asset.Criticality integrity;
        private Asset.Criticality availability;

        private Boolean personalInfoIncluded;
        private String personalInfoType;
        private Boolean personalInfoProcessing;

        private String linkedSystems;

        private Boolean accessControlTarget;
        private Boolean backupTarget;
        private Boolean logManagementTarget;

        private BigDecimal monthlyCost;
        private LocalDate contractExpiry;
        private LocalDate lastInspectionDate;
        private LocalDate nextInspectionDate;
        private LocalDate lastReviewDate;
        private String remarks;

        private Boolean active;
    }

    @Getter
    @Builder
    public static class Response {
        private Long id;
        private String name;
        private String assetCategory;
        private String type;
        private String description;

        private String owner;
        private String department;
        private String operator;

        private String environment;
        private String location;
        private String status;

        private String cloudProvider;
        private String cloudResourceId;
        private String region;
        private String ipAddress;
        private String osType;
        private String spec;

        private String criticality;
        private String confidentiality;
        private String integrity;
        private String availability;

        private boolean personalInfoIncluded;
        private String personalInfoType;
        private boolean personalInfoProcessing;

        private String linkedSystems;

        private boolean accessControlTarget;
        private boolean backupTarget;
        private boolean logManagementTarget;

        private boolean active;
        private BigDecimal monthlyCost;
        private LocalDate contractExpiry;
        private LocalDate lastInspectionDate;
        private LocalDate nextInspectionDate;
        private LocalDate lastReviewDate;
        private String remarks;

        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        public static Response from(Asset a) {
            return Response.builder()
                    .id(a.getId())
                    .name(a.getName())
                    .assetCategory(a.getAssetCategory() != null ? a.getAssetCategory().name() : null)
                    .type(a.getType())
                    .description(a.getDescription())
                    .owner(a.getOwner())
                    .department(a.getDepartment())
                    .operator(a.getOperator())
                    .environment(a.getEnvironment() != null ? a.getEnvironment().name() : Asset.Environment.PRODUCTION.name())
                    .location(a.getLocation())
                    .status(a.getStatus() != null ? a.getStatus().name() : Asset.Status.OPERATIONAL.name())
                    .cloudProvider(a.getCloudProvider() != null ? a.getCloudProvider().name() : Asset.CloudProvider.ON_PREMISE.name())
                    .cloudResourceId(a.getCloudResourceId())
                    .region(a.getRegion())
                    .ipAddress(a.getIpAddress())
                    .osType(a.getOsType())
                    .spec(a.getSpec())
                    .criticality(a.getCriticality().name())
                    .confidentiality(a.getConfidentiality() != null ? a.getConfidentiality().name() : Asset.Criticality.MEDIUM.name())
                    .integrity(a.getIntegrity() != null ? a.getIntegrity().name() : Asset.Criticality.MEDIUM.name())
                    .availability(a.getAvailability() != null ? a.getAvailability().name() : Asset.Criticality.MEDIUM.name())
                    .personalInfoIncluded(a.isPersonalInfoIncluded())
                    .personalInfoType(a.getPersonalInfoType())
                    .personalInfoProcessing(a.isPersonalInfoProcessing())
                    .linkedSystems(a.getLinkedSystems())
                    .accessControlTarget(a.isAccessControlTarget())
                    .backupTarget(a.isBackupTarget())
                    .logManagementTarget(a.isLogManagementTarget())
                    .active(a.isActive())
                    .monthlyCost(a.getMonthlyCost())
                    .contractExpiry(a.getContractExpiry())
                    .lastInspectionDate(a.getLastInspectionDate())
                    .nextInspectionDate(a.getNextInspectionDate())
                    .lastReviewDate(a.getLastReviewDate())
                    .remarks(a.getRemarks())
                    .createdAt(a.getCreatedAt())
                    .updatedAt(a.getUpdatedAt())
                    .build();
        }
    }
}
