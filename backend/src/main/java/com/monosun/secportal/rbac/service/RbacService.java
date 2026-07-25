package com.monosun.secportal.rbac.service;

import com.monosun.secportal.auth.entity.User;
import com.monosun.secportal.auth.repository.UserRepository;
import com.monosun.secportal.common.exception.BusinessException;
import com.monosun.secportal.common.exception.ResourceNotFoundException;
import com.monosun.secportal.rbac.MenuKeys;
import com.monosun.secportal.rbac.dto.RbacDto;
import com.monosun.secportal.rbac.entity.CustomRole;
import com.monosun.secportal.rbac.entity.RolePermission;
import com.monosun.secportal.rbac.repository.CustomRoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RbacService {

    private final CustomRoleRepository roleRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<RbacDto.RoleResponse> listRoles() {
        return roleRepository.findAllWithPermissions().stream()
                .map(RbacDto.RoleResponse::from)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public RbacDto.RoleResponse getRole(Long id) {
        return RbacDto.RoleResponse.from(findRole(id));
    }

    @Transactional
    public RbacDto.RoleResponse createRole(RbacDto.RoleCreateRequest req) {
        if (req.getName() == null || req.getName().isBlank())
            throw new BusinessException("Role 이름을 입력해주세요.");
        if (roleRepository.existsByName(req.getName().trim()))
            throw new BusinessException("이미 존재하는 Role 이름입니다: " + req.getName());

        CustomRole role = CustomRole.builder()
                .name(req.getName().trim())
                .description(req.getDescription())
                .build();
        role = roleRepository.save(role);
        applyPermissions(role, req.getPermissions());
        return RbacDto.RoleResponse.from(role);
    }

    @Transactional
    public RbacDto.RoleResponse updateRole(Long id, RbacDto.RoleUpdateRequest req) {
        CustomRole role = findRole(id);
        if (req.getName() != null && !req.getName().isBlank()) {
            String newName = req.getName().trim();
            if (!newName.equals(role.getName()) && roleRepository.existsByName(newName))
                throw new BusinessException("이미 존재하는 Role 이름입니다: " + newName);
            role.setName(newName);
        }
        if (req.getDescription() != null) role.setDescription(req.getDescription());
        // applyPermissions 가 제자리 갱신(추가·수정·삭제)을 처리한다 — clear() 하면 유니크 제약 충돌
        if (req.getPermissions() != null) applyPermissions(role, req.getPermissions());
        return RbacDto.RoleResponse.from(role);
    }

    @Transactional
    public void deleteRole(Long id) {
        CustomRole role = findRole(id);
        roleRepository.delete(role);
    }

    @Transactional(readOnly = true)
    public List<RbacDto.UserSummary> getRoleUsers(Long id) {
        CustomRole role = findRole(id);
        return role.getUsers().stream()
                .map(RbacDto.UserSummary::from)
                .collect(Collectors.toList());
    }

    @Transactional
    public void assignUser(Long roleId, Long userId) {
        CustomRole role = findRole(roleId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
        role.getUsers().add(user);
    }

    @Transactional
    public void removeUser(Long roleId, Long userId) {
        CustomRole role = findRole(roleId);
        role.getUsers().removeIf(u -> u.getId().equals(userId));
    }

    // ── 기본 역할(MANAGER·USER) 권한 ──────────────────────────────────────────

    private static final Map<User.Role, String> BUILTIN_LABEL = Map.of(
            User.Role.ADMIN,   "ADMIN (관리자)",
            User.Role.MANAGER, "MANAGER (보안담당자)",
            User.Role.USER,    "USER (일반 사용자)"
    );

    private static final Map<User.Role, String> BUILTIN_DESC = Map.of(
            User.Role.ADMIN,   "시스템 관리자. 항상 전체 메뉴에 대한 읽기·쓰기·삭제 권한을 가지며 변경할 수 없습니다.",
            User.Role.MANAGER, "보안·개인정보 담당자에게 부여하는 기본 역할입니다. 여기서 끈 메뉴는 화면에서 숨겨집니다.",
            User.Role.USER,    "일반 임직원 계정의 기본 역할입니다. 별도 Role을 배정하면 그 권한이 합산(OR)됩니다."
    );

    /** 기본 역할 3종의 현재 권한. ADMIN 은 저장하지 않고 '전체 권한'으로 만들어 보여준다. */
    @Transactional(readOnly = true)
    public List<RbacDto.BuiltinRoleResponse> listBuiltinRoles() {
        List<RbacDto.BuiltinRoleResponse> result = new ArrayList<>();

        result.add(RbacDto.BuiltinRoleResponse.builder()
                .role(User.Role.ADMIN.name())
                .label(BUILTIN_LABEL.get(User.Role.ADMIN))
                .description(BUILTIN_DESC.get(User.Role.ADMIN))
                .editable(false)
                .userCount((int) userRepository.countByRoleAndActiveTrue(User.Role.ADMIN))
                .permissions(MenuKeys.ALL.stream()
                        .map(k -> new RbacDto.PermissionEntry(k, true, true, true))
                        .collect(Collectors.toList()))
                .build());

        for (User.Role role : List.of(User.Role.MANAGER, User.Role.USER)) {
            CustomRole row = roleRepository.findBuiltinWithPermissions(role).orElse(null);
            result.add(RbacDto.BuiltinRoleResponse.builder()
                    .role(role.name())
                    .label(BUILTIN_LABEL.get(role))
                    .description(BUILTIN_DESC.get(role))
                    .editable(true)
                    .userCount((int) userRepository.countByRoleAndActiveTrue(role))
                    .permissions(row == null ? List.of() : row.getPermissions().stream()
                            .map(p -> new RbacDto.PermissionEntry(p.getMenuKey(), p.isCanRead(), p.isCanWrite(), p.isCanDelete()))
                            .collect(Collectors.toList()))
                    .updatedAt(row == null ? null : row.getUpdatedAt())
                    .build());
        }
        return result;
    }

    @Transactional
    public RbacDto.BuiltinRoleResponse updateBuiltinRole(String roleName, RbacDto.BuiltinRoleUpdateRequest req) {
        User.Role role = parseBuiltinRole(roleName);
        if (role == User.Role.ADMIN)
            throw new BusinessException("ADMIN 은 항상 전체 권한을 가지므로 변경할 수 없습니다.");

        CustomRole row = roleRepository.findBuiltinWithPermissions(role)
                .orElseGet(() -> roleRepository.save(CustomRole.builder()
                        .name(role.name())
                        .description(BUILTIN_DESC.get(role))
                        .builtinRole(role)
                        .build()));

        applyPermissions(row, req.getPermissions());

        return RbacDto.BuiltinRoleResponse.builder()
                .role(role.name())
                .label(BUILTIN_LABEL.get(role))
                .description(BUILTIN_DESC.get(role))
                .editable(true)
                .userCount((int) userRepository.countByRoleAndActiveTrue(role))
                .permissions(req.getPermissions() == null ? List.of() : req.getPermissions())
                .build();
    }

    private User.Role parseBuiltinRole(String name) {
        try {
            return User.Role.valueOf(String.valueOf(name).toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessException("알 수 없는 기본 역할입니다: " + name);
        }
    }

    // ── 내 유효 권한 ─────────────────────────────────────────────────────────

    /**
     * 유효 권한 = 기본 역할(MANAGER·USER) 권한 ∪ 배정된 커스텀 Role 권한 (OR 합산).
     * ADMIN 은 항상 전체 허용이며, 기본 역할 권한 행이 아직 없으면 판단 근거가 없으므로
     * 메뉴가 통째로 사라지지 않도록 fullAccess 로 폴백한다(초기화 실패·구 볼륨 대비).
     */
    @Transactional(readOnly = true)
    public RbacDto.MyPermissions getMyPermissions(User user) {
        if (user.getRole() == User.Role.ADMIN) {
            return RbacDto.MyPermissions.builder()
                    .isAdmin(true)
                    .fullAccess(true)
                    .permissions(Collections.emptyMap())
                    .build();
        }

        Map<String, RbacDto.PermissionEntry> merged = new HashMap<>();

        CustomRole builtin = roleRepository.findBuiltinWithPermissions(user.getRole()).orElse(null);
        if (builtin == null) {
            return RbacDto.MyPermissions.builder()
                    .isAdmin(false)
                    .fullAccess(true)
                    .permissions(Collections.emptyMap())
                    .build();
        }
        mergePermissions(merged, builtin);

        for (CustomRole role : roleRepository.findByUserId(user.getId())) {
            mergePermissions(merged, role);
        }

        return RbacDto.MyPermissions.builder()
                .isAdmin(false)
                .fullAccess(false)
                .permissions(merged)
                .build();
    }

    private void mergePermissions(Map<String, RbacDto.PermissionEntry> merged, CustomRole role) {
        for (RolePermission perm : role.getPermissions()) {
            merged.merge(perm.getMenuKey(),
                    new RbacDto.PermissionEntry(perm.getMenuKey(), perm.isCanRead(), perm.isCanWrite(), perm.isCanDelete()),
                    (a, b) -> new RbacDto.PermissionEntry(
                            a.getMenuKey(),
                            a.isCanRead() || b.isCanRead(),
                            a.isCanWrite() || b.isCanWrite(),
                            a.isCanDelete() || b.isCanDelete()
                    ));
        }
    }

    /** 커스텀 Role 조회. 기본 역할 시스템 행은 이 경로로 수정·삭제·사용자 배정을 할 수 없다. */
    private CustomRole findRole(Long id) {
        CustomRole role = roleRepository.findByIdWithPermissions(id)
                .orElseThrow(() -> new ResourceNotFoundException("CustomRole", id));
        if (role.getBuiltinRole() != null)
            throw new BusinessException("기본 역할(" + role.getBuiltinRole().name() + ")은 기본 역할 권한 화면에서만 수정할 수 있습니다.");
        return role;
    }

    /**
     * 권한 목록을 요청 내용과 일치시킨다 — <b>기존 행을 지우고 다시 넣지 않고 제자리에서 갱신</b>한다.
     *
     * role_permissions 에는 (role_id, menu_key) 유니크 제약(uk_role_menu)이 있고,
     * clear() 후 같은 menu_key 를 다시 add 하면 Hibernate 가 flush 시 DELETE 보다 INSERT 를 먼저 실행해
     * "Duplicate entry '<roleId>-<menuKey>'" 로 저장이 실패한다. 그래서 다음 순서로 처리한다.
     *   1) 요청에 없는 menu_key 는 제거 (orphanRemoval 로 삭제)
     *   2) 남아 있는 menu_key 는 읽기·쓰기·삭제 값만 갱신
     *   3) 요청에만 있는 menu_key 는 새로 추가
     * 같은 menu_key 가 요청에 여러 번 오면 마지막 값을 쓴다.
     */
    private void applyPermissions(CustomRole role, List<RbacDto.PermissionEntry> entries) {
        if (entries == null) return;

        Map<String, RbacDto.PermissionEntry> wanted = new LinkedHashMap<>();
        for (RbacDto.PermissionEntry entry : entries) {
            if (entry.getMenuKey() == null || entry.getMenuKey().isBlank()) continue;
            wanted.put(entry.getMenuKey(), entry);
        }

        // 1) 요청에서 빠진 권한 제거
        role.getPermissions().removeIf(p -> !wanted.containsKey(p.getMenuKey()));

        // 2) 유지되는 권한은 값만 갱신하고 처리 목록에서 뺀다
        for (RolePermission perm : role.getPermissions()) {
            RbacDto.PermissionEntry entry = wanted.remove(perm.getMenuKey());
            if (entry == null) continue;
            perm.setCanRead(entry.isCanRead());
            perm.setCanWrite(entry.isCanWrite());
            perm.setCanDelete(entry.isCanDelete());
        }

        // 3) 새로 생긴 권한만 추가
        for (RbacDto.PermissionEntry entry : wanted.values()) {
            role.getPermissions().add(RolePermission.builder()
                    .role(role)
                    .menuKey(entry.getMenuKey())
                    .canRead(entry.isCanRead())
                    .canWrite(entry.isCanWrite())
                    .canDelete(entry.isCanDelete())
                    .build());
        }
    }
}
