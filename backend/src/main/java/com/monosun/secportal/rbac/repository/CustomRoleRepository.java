package com.monosun.secportal.rbac.repository;

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

    @Query("SELECT DISTINCT r FROM CustomRole r LEFT JOIN FETCH r.permissions")
    List<CustomRole> findAllWithPermissions();

    @Query("SELECT r FROM CustomRole r JOIN r.users u WHERE u.id = :userId")
    List<CustomRole> findByUserId(@Param("userId") Long userId);
}
