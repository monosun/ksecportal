package com.monosun.secportal.emergency.service;

import com.monosun.secportal.audit.service.AuditLogService;
import com.monosun.secportal.common.exception.BusinessException;
import com.monosun.secportal.common.exception.ResourceNotFoundException;
import com.monosun.secportal.emergency.dto.EmergencyDto;
import com.monosun.secportal.emergency.entity.EmergencyContact;
import com.monosun.secportal.emergency.entity.EmergencyContactGroup;
import com.monosun.secportal.emergency.repository.EmergencyContactGroupRepository;
import com.monosun.secportal.emergency.repository.EmergencyContactRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EmergencyContactService {

    private final EmergencyContactGroupRepository groupRepo;
    private final EmergencyContactRepository contactRepo;
    private final AuditLogService auditLogService;

    // ── Groups ────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<EmergencyDto.GroupResponse> listGroups() {
        return groupRepo.findAllWithContacts()
                .stream().map(EmergencyDto.GroupResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public EmergencyDto.GroupResponse getGroup(Long id) {
        return EmergencyDto.GroupResponse.from(findGroup(id));
    }

    @Transactional
    public EmergencyDto.GroupResponse createGroup(EmergencyDto.GroupRequest req) {
        EmergencyContactGroup g = EmergencyContactGroup.builder()
                .name(req.getName())
                .contactType(parseType(req.getContactType()))
                .description(req.getDescription())
                .sortOrder(req.getSortOrder() != null ? req.getSortOrder() : nextSortOrder())
                .build();
        EmergencyContactGroup saved = groupRepo.save(g);
        auditLogService.log("EMERGENCY_GROUP_CREATED", "EMERGENCY_GROUP", saved.getId(), "name=" + saved.getName());
        return EmergencyDto.GroupResponse.from(saved);
    }

    @Transactional
    public EmergencyDto.GroupResponse updateGroup(Long id, EmergencyDto.GroupRequest req) {
        EmergencyContactGroup g = findGroup(id);
        g.setName(req.getName());
        g.setContactType(parseType(req.getContactType()));
        g.setDescription(req.getDescription());
        if (req.getSortOrder() != null) g.setSortOrder(req.getSortOrder());
        auditLogService.log("EMERGENCY_GROUP_UPDATED", "EMERGENCY_GROUP", id, "name=" + g.getName());
        return EmergencyDto.GroupResponse.from(groupRepo.save(g));
    }

    @Transactional
    public void toggleGroupActive(Long id) {
        EmergencyContactGroup g = findGroup(id);
        g.setActive(!g.isActive());
        groupRepo.save(g);
    }

    @Transactional
    public void deleteGroup(Long id) {
        findGroup(id);
        if (contactRepo.countByGroupId(id) > 0) {
            throw new BusinessException("연락처가 등록된 그룹은 삭제할 수 없습니다. 연락처를 먼저 정리하거나 비활성 처리하세요.");
        }
        groupRepo.deleteById(id);
        auditLogService.log("EMERGENCY_GROUP_DELETED", "EMERGENCY_GROUP", id, "");
    }

    // ── Contacts ──────────────────────────────────────────────────────────

    @Transactional
    public EmergencyDto.ContactResponse createContact(EmergencyDto.ContactRequest req) {
        EmergencyContactGroup g = findGroup(req.getGroupId());
        EmergencyContact c = EmergencyContact.builder()
                .group(g)
                .name(req.getName())
                .organization(req.getOrganization())
                .department(req.getDepartment())
                .position(req.getPosition())
                .roleName(req.getRoleName())
                .contactOrder(req.getContactOrder() != null ? req.getContactOrder() : nextContactOrder(g))
                .mobile(req.getMobile())
                .officePhone(req.getOfficePhone())
                .email(req.getEmail())
                .available24h(Boolean.TRUE.equals(req.getAvailable24h()))
                .note(req.getNote())
                .build();
        EmergencyContact saved = contactRepo.save(c);
        // 연락처 본문(휴대전화·이메일)은 개인정보이므로 감사 로그에 남기지 않는다.
        auditLogService.log("EMERGENCY_CONTACT_CREATED", "EMERGENCY_CONTACT", saved.getId(),
                "group=" + g.getName() + ", name=" + saved.getName());
        return EmergencyDto.ContactResponse.from(saved);
    }

    @Transactional
    public EmergencyDto.ContactResponse updateContact(Long id, EmergencyDto.ContactRequest req) {
        EmergencyContact c = findContact(id);
        if (req.getGroupId() != null && !req.getGroupId().equals(c.getGroup().getId())) {
            c.setGroup(findGroup(req.getGroupId()));
        }
        c.setName(req.getName());
        c.setOrganization(req.getOrganization());
        c.setDepartment(req.getDepartment());
        c.setPosition(req.getPosition());
        c.setRoleName(req.getRoleName());
        if (req.getContactOrder() != null) c.setContactOrder(req.getContactOrder());
        c.setMobile(req.getMobile());
        c.setOfficePhone(req.getOfficePhone());
        c.setEmail(req.getEmail());
        c.setAvailable24h(Boolean.TRUE.equals(req.getAvailable24h()));
        c.setNote(req.getNote());
        auditLogService.log("EMERGENCY_CONTACT_UPDATED", "EMERGENCY_CONTACT", id, "name=" + c.getName());
        return EmergencyDto.ContactResponse.from(contactRepo.save(c));
    }

    @Transactional
    public void toggleContactActive(Long id) {
        EmergencyContact c = findContact(id);
        c.setActive(!c.isActive());
        contactRepo.save(c);
    }

    @Transactional
    public void deleteContact(Long id) {
        EmergencyContact c = findContact(id);
        String name = c.getName();
        contactRepo.deleteById(id);
        auditLogService.log("EMERGENCY_CONTACT_DELETED", "EMERGENCY_CONTACT", id, "name=" + name);
    }

    // ── Private helpers ───────────────────────────────────────────────────

    private int nextSortOrder() {
        return groupRepo.findAll().stream()
                .mapToInt(g -> g.getSortOrder() != null ? g.getSortOrder() : 0)
                .max().orElse(0) + 1;
    }

    private int nextContactOrder(EmergencyContactGroup g) {
        return g.getContacts().stream()
                .mapToInt(c -> c.getContactOrder() != null ? c.getContactOrder() : 0)
                .max().orElse(0) + 1;
    }

    private EmergencyContactGroup findGroup(Long id) {
        return groupRepo.findByIdWithContacts(id)
                .orElseThrow(() -> new ResourceNotFoundException("EmergencyContactGroup", id));
    }

    private EmergencyContact findContact(Long id) {
        return contactRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("EmergencyContact", id));
    }

    private EmergencyContactGroup.ContactType parseType(String t) {
        try { return EmergencyContactGroup.ContactType.valueOf(t); }
        catch (Exception e) { return EmergencyContactGroup.ContactType.INTERNAL; }
    }
}
