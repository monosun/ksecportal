package com.monosun.secportal.opstatus.service;

import com.monosun.secportal.audit.service.AuditLogService;
import com.monosun.secportal.common.exception.BusinessException;
import com.monosun.secportal.common.exception.ResourceNotFoundException;
import com.monosun.secportal.opstatus.dto.OperationStatusDto;
import com.monosun.secportal.opstatus.entity.OperationStatusDefault;
import com.monosun.secportal.opstatus.entity.OperationStatusItem;
import com.monosun.secportal.opstatus.repository.OperationStatusDefaultRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** 운영현황관리 기본 항목 마스터 (코드 관리에서 관리) */
@Service
@RequiredArgsConstructor
public class OperationStatusDefaultService {

    private final OperationStatusDefaultRepository repository;
    private final AuditLogService auditLogService;

    @Transactional(readOnly = true)
    public List<OperationStatusDto.DefaultResponse> list(String type, Boolean activeOnly) {
        List<OperationStatusDefault> rows;
        if (type == null || type.isBlank()) {
            rows = repository.findAllByOrderByTypeAscSortOrderAscIdAsc();
        } else {
            OperationStatusItem.Type t = parseType(type);
            rows = Boolean.TRUE.equals(activeOnly)
                    ? repository.findByTypeAndActiveTrueOrderBySortOrderAscIdAsc(t)
                    : repository.findByTypeOrderBySortOrderAscIdAsc(t);
        }
        return rows.stream().map(OperationStatusDto.DefaultResponse::from).toList();
    }

    @Transactional
    public OperationStatusDto.DefaultResponse create(OperationStatusDto.DefaultRequest req) {
        if (req.getName() == null || req.getName().isBlank())
            throw new BusinessException("점검 항목명을 입력해주세요.");

        OperationStatusItem.Type type = parseType(req.getType());
        int nextOrder = repository.findByTypeOrderBySortOrderAscIdAsc(type).stream()
                .mapToInt(d -> d.getSortOrder() == null ? 0 : d.getSortOrder())
                .max().orElse(0) + 1;

        OperationStatusDefault row = repository.save(OperationStatusDefault.builder()
                .type(type)
                .category(req.getCategory())
                .name(req.getName().trim())
                .cycle(req.getCycle())
                .deliverable(req.getDeliverable())
                .owner(req.getOwner())
                .manager(req.getManager())
                .note(req.getNote())
                .planMonths(toMask(req.getPlan()))
                .sortOrder(req.getSortOrder() != null ? req.getSortOrder() : nextOrder)
                .active(req.getActive() == null || req.getActive())
                .build());

        auditLogService.log("CREATE", "OPERATION_STATUS_DEFAULT", row.getId(),
                "운영현황 기본 항목 등록: [" + type + "] " + row.getName());
        return OperationStatusDto.DefaultResponse.from(row);
    }

    @Transactional
    public OperationStatusDto.DefaultResponse update(Long id, OperationStatusDto.DefaultRequest req) {
        OperationStatusDefault row = find(id);

        if (req.getType() != null && !req.getType().isBlank()) row.setType(parseType(req.getType()));
        if (req.getCategory() != null) row.setCategory(req.getCategory());
        if (req.getName() != null && !req.getName().isBlank()) row.setName(req.getName().trim());
        if (req.getCycle() != null) row.setCycle(req.getCycle());
        if (req.getDeliverable() != null) row.setDeliverable(req.getDeliverable());
        if (req.getOwner() != null) row.setOwner(req.getOwner());
        if (req.getManager() != null) row.setManager(req.getManager());
        if (req.getNote() != null) row.setNote(req.getNote());
        if (req.getSortOrder() != null) row.setSortOrder(req.getSortOrder());
        if (req.getActive() != null) row.setActive(req.getActive());
        if (req.getPlan() != null) row.setPlanMonths(toMask(req.getPlan()));

        auditLogService.log("UPDATE", "OPERATION_STATUS_DEFAULT", id,
                "운영현황 기본 항목 수정: " + row.getName());
        return OperationStatusDto.DefaultResponse.from(row);
    }

    @Transactional
    public void delete(Long id) {
        OperationStatusDefault row = find(id);
        auditLogService.log("DELETE", "OPERATION_STATUS_DEFAULT", id,
                "운영현황 기본 항목 삭제: " + row.getName());
        repository.delete(row);
    }

    private OperationStatusDefault find(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("OperationStatusDefault", id));
    }

    private OperationStatusItem.Type parseType(String type) {
        if (type == null || type.isBlank()) return OperationStatusItem.Type.ISMS;
        try {
            return OperationStatusItem.Type.valueOf(type.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessException("알 수 없는 구분입니다: " + type);
        }
    }

    private Integer toMask(List<Boolean> flags) {
        if (flags == null) return 0;
        int mask = 0;
        for (int i = 0; i < Math.min(12, flags.size()); i++) {
            if (Boolean.TRUE.equals(flags.get(i))) mask |= (1 << i);
        }
        return mask;
    }
}
