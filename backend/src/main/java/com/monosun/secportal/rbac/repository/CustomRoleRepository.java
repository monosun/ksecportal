package com.monosun.secportal.rbac.repository;

import com.monosun.secportal.auth.entity.User;
import com.monosun.secportal.rbac.entity.CustomRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CustomRoleRepository extends JpaRepository<CustomRole, Long> {

    boolean existsByName(String name);

    @Query("SELECT r FROM CustomRole r LEFT JOIN FETCH r.permissions WHERE r.id = :id")
    Optional<CustomRole> findByIdWithPermissions(@Param("id") Long id);

    /** 일반(커스텀) Role 목록 — 기본 역할 시스템 행은 제외한다. */
    @Query("SELECT DISTINCT r FROM CustomRole r LEFT JOIN FETCH r.permissions WHERE r.builtinRole IS NULL")
    List<CustomRole> findAllWithPermissions();

    /** 사용자에게 배정된 커스텀 Role — 기본 역할 행은 사용자 배정 대상이 아니라 자연히 제외된다. */
    @Query("SELECT r FROM CustomRole r JOIN r.users u WHERE u.id = :userId")
    List<CustomRole> findByUserId(@Param("userId") Long userId);

    /** 기본 역할(MANAGER·USER)의 권한 행 */
    @Query("SELECT r FROM CustomRole r LEFT JOIN FETCH r.permissions WHERE r.builtinRole = :builtinRole")
    Optional<CustomRole> findBuiltinWithPermissions(@Param("builtinRole") User.Role builtinRole);

    boolean existsByBuiltinRole(User.Role builtinRole);
}
