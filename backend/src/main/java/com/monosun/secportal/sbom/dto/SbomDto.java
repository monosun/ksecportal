package com.monosun.secportal.sbom.dto;

import com.monosun.secportal.sbom.entity.SbomComponent;
import com.monosun.secportal.sbom.entity.SbomSoftware;
import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

public class SbomDto {

    @Getter
    public static class CreateRequest {
        @NotBlank private String name;
        @NotBlank private String version;
        private String vendor;
        private String description;
        private String remarks;
    }

    @Getter
    public static class UpdateRequest {
        private String name;
        private String version;
        private String vendor;
        private String description;
        private String remarks;
    }

    @Getter
    public static class ComponentRequest {
        @NotBlank private String libraryName;
        private String libraryVersion;
        private String componentType;
        private String groupName;
        private String purl;
        private String license;
        private String remarks;
    }

    @Getter
    @Builder
    public static class ComponentResponse {
        private Long id;
        private String componentType;
        private String groupName;
        private String libraryName;
        private String libraryVersion;
        private String purl;
        private String license;
        private String remarks;
        private LocalDateTime createdAt;

        public static ComponentResponse from(SbomComponent c) {
            return ComponentResponse.builder()
                    .id(c.getId())
                    .componentType(c.getComponentType())
                    .groupName(c.getGroupName())
                    .libraryName(c.getLibraryName())
                    .libraryVersion(c.getLibraryVersion())
                    .purl(c.getPurl())
                    .license(c.getLicense())
                    .remarks(c.getRemarks())
                    .createdAt(c.getCreatedAt())
                    .build();
        }
    }

    @Getter
    @Builder
    public static class Response {
        private Long id;
        private String name;
        private String version;
        private String vendor;
        private String description;
        private String remarks;
        private int componentCount;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        public static Response from(SbomSoftware s) {
            return Response.builder()
                    .id(s.getId())
                    .name(s.getName())
                    .version(s.getVersion())
                    .vendor(s.getVendor())
                    .description(s.getDescription())
                    .remarks(s.getRemarks())
                    .componentCount(s.getComponents().size())
                    .createdAt(s.getCreatedAt())
                    .updatedAt(s.getUpdatedAt())
                    .build();
        }
    }

    @Getter
    @Builder
    public static class DetailResponse {
        private Long id;
        private String name;
        private String version;
        private String vendor;
        private String description;
        private String remarks;
        private List<ComponentResponse> components;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        public static DetailResponse from(SbomSoftware s) {
            return DetailResponse.builder()
                    .id(s.getId())
                    .name(s.getName())
                    .version(s.getVersion())
                    .vendor(s.getVendor())
                    .description(s.getDescription())
                    .remarks(s.getRemarks())
                    .components(s.getComponents().stream().map(ComponentResponse::from).toList())
                    .createdAt(s.getCreatedAt())
                    .updatedAt(s.getUpdatedAt())
                    .build();
        }
    }

    @Getter
    @Builder
    public static class SimpleResponse {
        private Long id;
        private String name;
        private String version;
        private String vendor;
        private int componentCount;

        public static SimpleResponse from(SbomSoftware s) {
            return SimpleResponse.builder()
                    .id(s.getId())
                    .name(s.getName())
                    .version(s.getVersion())
                    .vendor(s.getVendor())
                    .componentCount(s.getComponents().size())
                    .build();
        }
    }
}
