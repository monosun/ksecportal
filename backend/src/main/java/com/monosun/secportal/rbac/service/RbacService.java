package com.monosun.secportal.rbac.service;

import com.monosun.secportal.auth.entity.User;
import com.monosun.secportal.auth.repository.UserRepository;
import com.monosun.secportal.common.exception.BusinessException;
import com.monosun.secportal.common.exception.ResourceNotFoundException;
import com.monosun.secportal.rbac.dto.RbacDto;
import com.monosun.secportal.rbac.entity.CustomRole;
import com.monosun.secportal.rbac.entity.RolePermission;
import com.monosun.secportal.rbac.repository.CustomRoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;
import java.util.LinkedHashMap;

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
        if (req.getPermissions() != null) {
            role.getPermissions().clear();
            applyPermissions(role, req.getPermissions());
        }
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

    // USER 역할 계정에 적용되는 기본 권한 (CustomRole 미설정 시)
    private static final Map<String, RbacDto.PermissionEntry> DEFAULT_USER_PERMISSIONS;
    static {
        DEFAULT_USER_PERMISSIONS = new LinkedHashMap<>();
        DEFAULT_USER_PERMISSIONS.put("training", new RbacDto.PermissionEntry("training", true, false, false));
        DEFAULT_USER_PERMISSIONS.put("sec_docs",  new RbacDto.PermissionEntry("sec_docs",  true, false, false));
    }

    @Transactional(readOnly = true)
    public RbacDto.MyPermissions getMyPermissions(User user) {
        boolean isAdmin = user.getRole() == User.Role.ADMIN;
        if (isAdmin) {
            return RbacDto.MyPermissions.builder()
                    .isAdmin(true)
                    .permissions(Collections.emptyMap())
                    .build();
        }

        List<CustomRole> roles = roleRepository.findByUserId(user.getId());
        Map<String, RbacDto.PermissionEntry> merged = new HashMap<>();

        for (CustomRole role : roles) {
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

        // USER 역할이고 CustomRole이 없으면 기본 권한(보안교육·보안문서 읽기) 적용
        if (user.getRole() == User.Role.USER && roles.isEmpty()) {
            DEFAULT_USER_PERMISSIONS.forEach(merged::putIfAbsent);
        }

        return RbacDto.MyPermissions.builder()
                .isAdmin(false)
                .permissions(merged)
                .build();
    }

    private CustomRole findRole(Long id) {
        return roleRepository.findByIdWithPermissions(id)
                .orElseThrow(() -> new ResourceNotFoundException("CustomRole", id));
    }

    private void applyPermissions(CustomRole role, List<RbacDto.PermissionEntry> entries) {
        if (entries == null) return;
        for (RbacDto.PermissionEntry entry : entries) {
            if (entry.getMenuKey() == null || entry.getMenuKey().isBlank()) continue;
            RolePermission perm = RolePermission.builder()
                    .role(role)
                    .menuKey(entry.getMenuKey())
                    .canRead(entry.isCanRead())
                    .canWrite(entry.isCanWrite())
                    .canDelete(entry.isCanDelete())
                    .build();
            role.getPermissions().add(perm);
        }
    }
}
