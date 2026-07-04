package com.monosun.secportal.internalaudit.entity;

import com.monosun.secportal.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "audit_targets",
        indexes = @Index(name = "idx_audit_targets_audit", columnList = "audit_id"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditTarget extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "audit_id", nullable = false)
    private InternalAudit audit;

    @Column(name = "target_name", nullable = false, length = 300)
    private String targetName;

    @Column(name = "target_type", length = 100)
    private String targetType;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "sort_order")
    @Builder.Default
    private int sortOrder = 0;
}
