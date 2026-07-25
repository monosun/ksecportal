package com.monosun.secportal.rbac.entity;

import com.monosun.secportal.auth.entity.User;
import com.monosun.secportal.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "custom_roles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomRole extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    /**
     * 기본 역할(MANAGER·USER)의 권한을 담는 시스템 행이면 해당 역할이 들어간다.
     * null 이면 관리자가 만든 일반 커스텀 Role. 시스템 행은 이름 변경·삭제·사용자 배정을 하지 않고
     * 권한만 수정하며, 소속은 users.role 로 결정된다. (ADMIN 은 항상 전체 권한이라 행을 두지 않는다)
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "builtin_role", length = 20)
    private User.Role builtinRole;

    @OneToMany(mappedBy = "role", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<RolePermission> permissions = new ArrayList<>();

    @ManyToMany
    @JoinTable(
            name = "user_custom_roles",
            joinColumns = @JoinColumn(name = "role_id"),
            inverseJoinColumns = @JoinColumn(name = "user_id")
    )
    @Builder.Default
    private Set<User> users = new HashSet<>();
}
