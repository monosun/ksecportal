package com.monosun.secportal.internalaudit.entity;

import com.monosun.secportal.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "audit_items",
        indexes = @Index(name = "idx_audit_items_audit", columnList = "audit_id"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditItem extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "audit_id", nullable = false)
    private InternalAudit audit;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_id")
    private AuditTarget target;

    @Column(name = "item_name", nullable = false, length = 500)
    private String itemName;

    @Column(name = "check_method", columnDefinition = "TEXT")
    private String checkMethod;

    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private Result result;

    @Column(columnDefinition = "TEXT")
    private String finding;

    @Column(name = "action_required", columnDefinition = "TEXT")
    private String actionRequired;

    @Column(name = "sort_order")
    @Builder.Default
    private int sortOrder = 0;

    public enum Result { PASS, FAIL, NA }
}
