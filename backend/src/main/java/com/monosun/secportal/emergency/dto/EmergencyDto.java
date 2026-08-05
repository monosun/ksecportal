package com.monosun.secportal.emergency.dto;

import com.monosun.secportal.emergency.entity.EmergencyContact;
import com.monosun.secportal.emergency.entity.EmergencyContactGroup;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

public class EmergencyDto {

    // ── Group ──────────────────────────────────────────────────────────────

    @Getter
    public static class GroupRequest {
        @NotBlank private String name;
        private String contactType;
        private String description;
        private Integer sortOrder;
    }

    @Getter @Builder
    public static class GroupResponse {
        private Long id;
        private String name;
        private String contactType;
        private String description;
        private Integer sortOrder;
        private boolean active;
        private int contactCount;
        private List<ContactResponse> contacts;
        private LocalDateTime createdAt;

        public static GroupResponse from(EmergencyContactGroup g) {
            List<ContactResponse> contacts = g.getContacts().stream()
                    .map(ContactResponse::from).toList();
            return GroupResponse.builder()
                    .id(g.getId())
                    .name(g.getName())
                    .contactType(g.getContactType().name())
                    .description(g.getDescription())
                    .sortOrder(g.getSortOrder())
                    .active(g.isActive())
                    .contactCount(contacts.size())
                    .contacts(contacts)
                    .createdAt(g.getCreatedAt())
                    .build();
        }
    }

    // ── Contact ────────────────────────────────────────────────────────────

    @Getter
    public static class ContactRequest {
        @NotNull private Long groupId;
        @NotBlank private String name;
        private String organization;
        private String department;
        private String position;
        private String roleName;
        private Integer contactOrder;
        private String mobile;
        private String officePhone;
        private String email;
        private Boolean available24h;
        private String note;
    }

    @Getter @Builder
    public static class ContactResponse {
        private Long id;
        private Long groupId;
        private String groupName;
        private String name;
        private String organization;
        private String department;
        private String position;
        private String roleName;
        private Integer contactOrder;
        private String mobile;
        private String officePhone;
        private String email;
        private boolean available24h;
        private String note;
        private boolean active;

        public static ContactResponse from(EmergencyContact c) {
            EmergencyContactGroup g = c.getGroup();
            return ContactResponse.builder()
                    .id(c.getId())
                    .groupId(g != null ? g.getId() : null)
                    .groupName(g != null ? g.getName() : null)
                    .name(c.getName())
                    .organization(c.getOrganization())
                    .department(c.getDepartment())
                    .position(c.getPosition())
                    .roleName(c.getRoleName())
                    .contactOrder(c.getContactOrder())
                    .mobile(c.getMobile())
                    .officePhone(c.getOfficePhone())
                    .email(c.getEmail())
                    .available24h(c.isAvailable24h())
                    .note(c.getNote())
                    .active(c.isActive())
                    .build();
        }
    }
}
