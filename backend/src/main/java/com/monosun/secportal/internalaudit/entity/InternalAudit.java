package com.monosun.secportal.internalaudit.entity;

import com.monosun.secportal.auth.entity.User;
import com.monosun.secportal.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "internal_audits",
        indexes = @Index(name = "idx_internal_audits_year", columnList = "year"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InternalAudit extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private int year;

    @Column(nullable = false, length = 500)
    private String title;

    @Column(name = "audit_start")
    private LocalDate auditStart;

    @Column(name = "audit_end")
    private LocalDate auditEnd;

    @Column(length = 300)
    private String auditor;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private Status status = Status.PLANNED;

    @Column(columnDefinition = "TEXT")
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @OneToMany(mappedBy = "audit", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC, createdAt ASC")
    @Builder.Default
    private List<AuditTarget> targets = new ArrayList<>();

    @OneToMany(mappedBy = "audit", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC, createdAt ASC")
    @Builder.Default
    private List<AuditItem> items = new ArrayList<>();

    @OneToMany(mappedBy = "audit", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("createdAt ASC")
    @Builder.Default
    private List<AuditFile> files = new ArrayList<>();

    public enum Status { PLANNED, IN_PROGRESS, COMPLETED }
}
