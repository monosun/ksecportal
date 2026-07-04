package com.monosun.secportal.isms.entity;

import com.monosun.secportal.auth.entity.User;
import com.monosun.secportal.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "isms_evidences")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IsmsEvidence extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_id", nullable = false)
    private IsmsItem item;

    @Column(nullable = false)
    private int year;

    @Column(nullable = false, length = 500)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(length = 500)
    private String fileName;

    @Column(length = 1000)
    private String filePath;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private Status status = Status.COMPLIANT;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "source_evidence_id")
    private IsmsEvidence sourceEvidence;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "registrant_id")
    private User registrant;

    public enum Status {
        COMPLIANT, PARTIAL, NON_COMPLIANT, NA
    }
}
