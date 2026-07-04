package com.monosun.secportal.privacy.entity;

import com.monosun.secportal.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "contractor_check_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContractorCheckItem extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String category;

    @Column(name = "item_name", nullable = false, length = 300)
    private String itemName;

    @Column(name = "check_method", length = 500)
    private String checkMethod;

    @Column(name = "check_standard", length = 500)
    private String checkStandard;

    @Column(name = "is_required", nullable = false)
    @Builder.Default
    private boolean isRequired = true;

    @Column(name = "sort_order")
    @Builder.Default
    private int sortOrder = 0;
}
