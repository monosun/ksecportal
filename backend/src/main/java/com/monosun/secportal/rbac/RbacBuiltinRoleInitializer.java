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

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

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
        backfillManagerMenus();
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

    /**
     * 새로 추가된 메뉴를 MANAGER 기본 역할에 보충한다.
     *
     * MANAGER 는 "기본적으로 전 메뉴 사용"이 전제이므로, 업그레이드로 메뉴가 늘어났을 때
     * 이미 만들어진 권한 행에 자동으로 반영되지 않으면 신규 메뉴가 MANAGER 에게 보이지 않는다.
     * (시더는 비어 있을 때만 돌기 때문) 그래서 <b>없는 menu_key 만</b> 읽기·쓰기·삭제로 추가한다.
     * 관리자가 의도적으로 끈 메뉴는 행이 남아 있으므로 다시 켜지지 않는다 — 새 메뉴만 대상이다.
     * USER 는 기본값이 최소 권한이라 보충하지 않는다.
     */
    private void backfillManagerMenus() {
        CustomRole role = roleRepository.findBuiltinWithPermissions(User.Role.MANAGER).orElse(null);
        if (role == null) return;

        Set<String> existing = role.getPermissions().stream()
                .map(RolePermission::getMenuKey)
                .collect(Collectors.toSet());

        List<String> added = MenuKeys.ALL.stream().filter(k -> !existing.contains(k)).toList();
        if (added.isEmpty()) return;

        for (String key : added) {
            role.getPermissions().add(RolePermission.builder()
                    .role(role).menuKey(key)
                    .canRead(true).canWrite(true).canDelete(true)
                    .build());
        }
        log.info("[RBAC] MANAGER 기본 역할에 신규 메뉴 {}개 보충: {}", added.size(), added);
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
