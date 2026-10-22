package com.monosun.secportal.rbac;

import com.monosun.secportal.auth.entity.User;
import com.monosun.secportal.rbac.dto.RbacDto;
import com.monosun.secportal.rbac.service.RbacService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.function.Predicate;

/**
 * 메뉴 단위 권한(RBAC)을 서버에서 강제한다 — {@code @PreAuthorize("@menuAuth.canWrite('isms')")}.
 *
 * <p>화면(auth store)의 canWrite/canDelete 와 같은 유효 권한({@link RbacService#getMyPermissions})을 쓰므로
 * 관리자가 커스텀 Role 로 USER 에게 준 쓰기 권한은 그대로 동작하고, 기본 USER 는 막힌다.</p>
 *
 * <p>기본 역할 권한 행이 없어 fullAccess 로 폴백되는 경우, 화면은 메뉴가 사라지지 않도록 전체 허용하지만
 * 서버는 판단 근거가 없으므로 기존 역할 기준(ADMIN·MANAGER)으로만 허용한다.</p>
 */
@Component("menuAuth")
@RequiredArgsConstructor
public class MenuAuthorization {

    private final RbacService rbacService;

    public boolean canWrite(String menuKey) {
        return check(menuKey, RbacDto.PermissionEntry::isCanWrite);
    }

    public boolean canDelete(String menuKey) {
        return check(menuKey, RbacDto.PermissionEntry::isCanDelete);
    }

    private boolean check(String menuKey, Predicate<RbacDto.PermissionEntry> allowed) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof User user)) return false;
        if (user.getRole() == User.Role.ADMIN) return true;

        RbacDto.MyPermissions perms = rbacService.getMyPermissions(user);
        if (perms.isFullAccess()) return user.getRole() == User.Role.MANAGER;

        RbacDto.PermissionEntry entry = perms.getPermissions().get(menuKey);
        return entry != null && allowed.test(entry);
    }
}
