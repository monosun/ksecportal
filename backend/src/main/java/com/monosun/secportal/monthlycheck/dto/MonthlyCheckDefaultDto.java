package com.monosun.secportal.monthlycheck.dto;

import com.monosun.secportal.monthlycheck.entity.MonthlyCheckDefault;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

public class MonthlyCheckDefaultDto {

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Request {
        @NotBlank private String priority; // HIGH/MEDIUM/LOW
        @NotBlank private String category;
        @NotBlank private String itemName;
        private String checkMethod;
        private String checkExample;
        private int sortOrder;
        private boolean active = true;
    }

    @Getter
    @Builder
    public static class Response {
        private Long id;
        private String priority;
        private String category;
        private String itemName;
        private String checkMethod;
        private String checkExample;
        private int sortOrder;
        private boolean active;

        public static Response from(MonthlyCheckDefault d) {
            return Response.builder()
                    .id(d.getId())
                    .priority(d.getPriority().name())
                    .category(d.getCategory())
                    .itemName(d.getItemName())
                    .checkMethod(d.getCheckMethod())
                    .checkExample(d.getCheckExample())
                    .sortOrder(d.getSortOrder())
                    .active(d.isActive())
                    .build();
        }
    }
}
