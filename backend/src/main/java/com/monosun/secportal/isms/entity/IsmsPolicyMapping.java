package com.monosun.secportal.isms.entity;

import com.monosun.secportal.policy.entity.Policy;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "isms_policy_mappings",
       uniqueConstraints = @UniqueConstraint(columnNames = {"isms_item_id", "policy_id"}))
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IsmsPolicyMapping {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "isms_item_id", nullable = false)
    private IsmsItem ismsItem;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "policy_id", nullable = false)
    private Policy policy;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void prePersist() {
        this.createdAt = LocalDateTime.now();
    }
}
