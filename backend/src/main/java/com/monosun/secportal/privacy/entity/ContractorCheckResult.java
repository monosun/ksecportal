package com.monosun.secportal.privacy.entity;

import com.monosun.secportal.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "contractor_check_results",
        indexes = {
                @Index(name = "idx_result_check", columnList = "check_id")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_check_item", columnNames = {"check_id", "check_item_id"})
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContractorCheckResult extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "check_id", nullable = false)
    private ContractorCheck check;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "check_item_id", nullable = false)
    private ContractorCheckItem checkItem;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private Result result = Result.NOT_CHECKED;

    @Column(columnDefinition = "TEXT")
    private String notes;

    public enum Result { PASS, FAIL, NA, NOT_CHECKED }
}
