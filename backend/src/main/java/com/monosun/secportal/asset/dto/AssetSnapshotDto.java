package com.monosun.secportal.asset.dto;

import com.monosun.secportal.asset.entity.AssetSnapshot;
import com.monosun.secportal.asset.entity.AssetSnapshotItem;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class AssetSnapshotDto {

    @Getter
    public static class CreateRequest {
        private String title;
        private String memo;
    }

    @Getter
    @Builder
    public static class SnapshotResponse {
        private Long id;
        private String title;
        private String memo;
        private int assetCount;
        private String createdBy;
        private LocalDateTime createdAt;

        public static SnapshotResponse from(AssetSnapshot s) {
            return SnapshotResponse.builder()
                    .id(s.getId())
                    .title(s.getTitle())
                    .memo(s.getMemo())
                    .assetCount(s.getAssetCount())
                    .createdBy(s.getCreatedBy())
                    .createdAt(s.getCreatedAt())
                    .build();
        }
    }

    @Getter
    @Builder
    public static class ItemResponse {
        private Long assetId;
        private String name;
        private String assetCategory;
        private String type;
        private String owner;
        private String department;
        private String operator;
        private String environment;
        private String location;
        private String status;
        private String cloudProvider;
        private String ipAddress;
        private String region;
        private String cloudResourceId;
        private String osType;
        private String criticality;
        private boolean personalInfoIncluded;
        private BigDecimal monthlyCost;

        public static ItemResponse from(AssetSnapshotItem i) {
            return ItemResponse.builder()
                    .assetId(i.getAssetId())
                    .name(i.getName())
                    .assetCategory(i.getAssetCategory())
                    .type(i.getType())
                    .owner(i.getOwner())
                    .department(i.getDepartment())
                    .operator(i.getOperator())
                    .environment(i.getEnvironment())
                    .location(i.getLocation())
                    .status(i.getStatus())
                    .cloudProvider(i.getCloudProvider())
                    .ipAddress(i.getIpAddress())
                    .region(i.getRegion())
                    .cloudResourceId(i.getCloudResourceId())
                    .osType(i.getOsType())
                    .criticality(i.getCriticality())
                    .personalInfoIncluded(i.isPersonalInfoIncluded())
                    .monthlyCost(i.getMonthlyCost())
                    .build();
        }
    }
}
