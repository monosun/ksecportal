package com.monosun.secportal.auth.repository;

import com.monosun.secportal.auth.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    List<User> findAllByActiveTrueOrderByNameAsc();
    List<User> findAllByRoleAndActiveTrue(User.Role role);
    long countByActiveTrue();
    Optional<User> findByOktaId(String oktaId);
}
