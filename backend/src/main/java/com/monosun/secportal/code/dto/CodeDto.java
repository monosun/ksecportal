package com.monosun.secportal.code.dto;

import com.monosun.secportal.code.entity.CodeGroup;
import com.monosun.secportal.code.entity.CodeValue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

public class CodeDto {

    @Getter
    public static class GroupRequest {
        @NotBlank @Size(max = 50)
        private String groupCode;
        @NotBlank @Size(max = 100)
        private String groupName;
        @Size(max = 255)
        private String description;
        private int sortOrder;
    }

    @Getter
    @Builder
    public static class GroupResponse {
        private String groupCode;
        private String groupName;
        private String description;
        private int sortOrder;
        private int valueCount;

        public static GroupResponse from(CodeGroup g, int valueCount) {
            return GroupResponse.builder()
                    .groupCode(g.getGroupCode())
                    .groupName(g.getGroupName())
                    .description(g.getDescription())
                    .sortOrder(g.getSortOrder())
                    .valueCount(valueCount)
                    .build();
        }
    }

    @Getter
    public static class ValueRequest {
        @NotBlank @Size(max = 100)
        private String value;
        @NotBlank @Size(max = 100)
        private String label;
        @Size(max = 500)
        private String description;
        private int sortOrder;
        private Boolean active;
    }

    @Getter
    @Builder
    public static class ValueResponse {
        private Long id;
        private String groupCode;
        private String value;
        private String label;
        private String description;
        private int sortOrder;
        private boolean active;

        public static ValueResponse from(CodeValue v) {
            return ValueResponse.builder()
                    .id(v.getId())
                    .groupCode(v.getGroupCode())
                    .value(v.getValue())
                    .label(v.getLabel())
                    .description(v.getDescription())
                    .sortOrder(v.getSortOrder())
                    .active(v.isActive())
                    .build();
        }
    }

    @Getter
    @Builder
    public static class SimpleValue {
        private String value;
        private String label;

        public static SimpleValue from(CodeValue v) {
            return SimpleValue.builder()
                    .value(v.getValue())
                    .label(v.getLabel())
                    .build();
        }
    }
}
