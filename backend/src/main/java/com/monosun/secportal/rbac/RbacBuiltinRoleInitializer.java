package com.monosun.secportal.rbac;

import com.monosun.secportal.auth.entity.User;
import com.monosun.secportal.rbac.entity.CustomRole;
import com.monosun.secportal.rbac.entity.RolePermission;
import com.monosun.secportal.rbac.repository.CustomRoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 기본 역할(MANAGER·USER)의 권한 행을 없을 때만 생성한다.
 *
 * 초기값은 기존 동작을 그대로 재현한다 — MANAGER 는 전 메뉴 읽기·쓰기·삭제,
 * USER 는 보안교육·보안 가이드 읽기. 따라서 이 기능 도입만으로 기존 계정의 화면이 달라지지 않는다.
 * 신규 설치·구 볼륨·DB 재생성 어디서든 자체 복구되도록 마이그레이션이 아닌 시더로 둔다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@Order(30)
public class RbacBuiltinRoleInitializer implements ApplicationRunner {

    private final CustomRoleRepository roleRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        seedManager();
        seedUser();
    }

    private void seedManager() {
        if (roleRepository.existsByBuiltinRole(User.Role.MANAGER)) return;

        CustomRole role = roleRepository.save(CustomRole.builder()
                .name(User.Role.MANAGER.name())
                .description("보안·개인정보 담당자 기본 역할")
                .builtinRole(User.Role.MANAGER)
                .build());

        for (String key : MenuKeys.ALL) {
            role.getPermissions().add(RolePermission.builder()
                    .role(role).menuKey(key)
                    .canRead(true).canWrite(true).canDelete(true)
                    .build());
        }
        log.info("[RBAC] MANAGER 기본 역할 권한 {}개 메뉴로 초기화", MenuKeys.ALL.size());
    }

    private void seedUser() {
        if (roleRepository.existsByBuiltinRole(User.Role.USER)) return;

        CustomRole role = roleRepository.save(CustomRole.builder()
                .name(User.Role.USER.name())
                .description("일반 임직원 기본 역할")
                .builtinRole(User.Role.USER)
                .build());

        for (String key : MenuKeys.DEFAULT_USER_READ) {
            role.getPermissions().add(RolePermission.builder()
                    .role(role).menuKey(key)
                    .canRead(true).canWrite(false).canDelete(false)
                    .build());
        }
        log.info("[RBAC] USER 기본 역할 권한 {}개 메뉴로 초기화", MenuKeys.DEFAULT_USER_READ.size());
    }
}
