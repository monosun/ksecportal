package com.monosun.secportal.rbac.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "role_permissions",
        uniqueConstraints = @UniqueConstraint(columnNames = {"role_id", "menu_key"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RolePermission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "role_id", nullable = false)
    private CustomRole role;

    @Column(name = "menu_key", nullable = false, length = 100)
    private String menuKey;

    @Column(name = "can_read", nullable = false)
    @Builder.Default
    private boolean canRead = false;

    @Column(name = "can_write", nullable = false)
    @Builder.Default
    private boolean canWrite = false;

    @Column(name = "can_delete", nullable = false)
    @Builder.Default
    private boolean canDelete = false;
}
