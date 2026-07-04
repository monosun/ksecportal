package com.monosun.secportal.phishing.entity;

import com.monosun.secportal.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "phishing_targets")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class PhishingTarget extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false)
    private String email;

    @Column(length = 100)
    private String department;

    @Column(length = 100)
    private String position;

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;
}
