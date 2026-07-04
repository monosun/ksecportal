package com.monosun.secportal.code.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "code_values")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CodeValue {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "group_code", length = 50, nullable = false)
    private String groupCode;

    @Column(name = "value", length = 100, nullable = false)
    private String value;

    @Column(name = "label", length = 100, nullable = false)
    private String label;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "sort_order", nullable = false)
    @Builder.Default
    private int sortOrder = 0;

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;
}
