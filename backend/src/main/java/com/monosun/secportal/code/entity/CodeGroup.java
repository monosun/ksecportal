package com.monosun.secportal.code.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "code_groups")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CodeGroup {

    @Id
    @Column(name = "group_code", length = 50, nullable = false)
    private String groupCode;

    @Column(name = "group_name", length = 100, nullable = false)
    private String groupName;

    @Column(length = 255)
    private String description;

    @Column(name = "sort_order", nullable = false)
    @Builder.Default
    private int sortOrder = 0;
}
