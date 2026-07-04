package com.monosun.secportal.isms.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "isms_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IsmsItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 20)
    private String itemCode;

    @Column(nullable = false, length = 200)
    private String itemName;

    @Column(nullable = false, length = 10)
    private String domainCode;

    @Column(nullable = false, length = 100)
    private String domainName;

    @Column(nullable = false)
    private int sectionNum;

    @Column(nullable = false, length = 100)
    private String sectionName;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private int sortOrder;

    @OneToMany(mappedBy = "item", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<IsmsEvidence> evidences = new ArrayList<>();
}
