package com.monosun.secportal.code.service;

import com.monosun.secportal.code.dto.CodeDto;
import com.monosun.secportal.code.entity.CodeGroup;
import com.monosun.secportal.code.entity.CodeValue;
import com.monosun.secportal.code.repository.CodeGroupRepository;
import com.monosun.secportal.code.repository.CodeValueRepository;
import com.monosun.secportal.common.exception.BusinessException;
import com.monosun.secportal.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CodeService {

    private final CodeGroupRepository groupRepo;
    private final CodeValueRepository valueRepo;

    @Transactional(readOnly = true)
    public List<CodeDto.GroupResponse> listGroups() {
        List<CodeGroup> groups = groupRepo.findAllByOrderBySortOrderAscGroupNameAsc();
        Map<String, Long> counts = valueRepo.findAll().stream()
                .collect(Collectors.groupingBy(CodeValue::getGroupCode, Collectors.counting()));
        return groups.stream()
                .map(g -> CodeDto.GroupResponse.from(g, counts.getOrDefault(g.getGroupCode(), 0L).intValue()))
                .toList();
    }

    @Transactional
    public CodeDto.GroupResponse createGroup(CodeDto.GroupRequest req) {
        if (groupRepo.existsById(req.getGroupCode())) {
            throw new BusinessException("이미 존재하는 코드 그룹입니다: " + req.getGroupCode());
        }
        CodeGroup group = groupRepo.save(CodeGroup.builder()
                .groupCode(req.getGroupCode().toUpperCase())
                .groupName(req.getGroupName())
                .description(req.getDescription())
                .sortOrder(req.getSortOrder())
                .build());
        return CodeDto.GroupResponse.from(group, 0);
    }

    @Transactional
    public CodeDto.GroupResponse updateGroup(String groupCode, CodeDto.GroupRequest req) {
        CodeGroup group = groupRepo.findById(groupCode)
                .orElseThrow(() -> new ResourceNotFoundException("CodeGroup", groupCode));
        group.setGroupName(req.getGroupName());
        group.setDescription(req.getDescription());
        group.setSortOrder(req.getSortOrder());
        int count = valueRepo.findByGroupCodeOrderBySortOrderAscLabelAsc(groupCode).size();
        return CodeDto.GroupResponse.from(group, count);
    }

    @Transactional
    public void deleteGroup(String groupCode) {
        if (!groupRepo.existsById(groupCode)) {
            throw new ResourceNotFoundException("CodeGroup", groupCode);
        }
        valueRepo.deleteByGroupCode(groupCode);
        groupRepo.deleteById(groupCode);
    }

    @Transactional(readOnly = true)
    public List<CodeDto.ValueResponse> listValues(String groupCode) {
        return valueRepo.findByGroupCodeOrderBySortOrderAscLabelAsc(groupCode)
                .stream().map(CodeDto.ValueResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<CodeDto.SimpleValue> listActiveValues(String groupCode) {
        return valueRepo.findByGroupCodeAndActiveTrueOrderBySortOrderAscLabelAsc(groupCode)
                .stream().map(CodeDto.SimpleValue::from).toList();
    }

    /**
     * 개인정보 항목(PI_*)에 등록된 마스킹 기준 목록.
     * 화면 목록의 개인정보 표시를 마스킹하는 데 쓰이므로 인증된 모든 사용자가 조회한다.
     */
    @Transactional(readOnly = true)
    public List<CodeDto.MaskingRule> listPiMaskingRules() {
        return valueRepo.findByGroupCodeStartingWithAndActiveTrueOrderByGroupCodeAscSortOrderAsc(PI_GROUP_PREFIX)
                .stream()
                .filter(v -> v.getMaskingType() != null && !v.getMaskingType().isBlank())
                .map(CodeDto.MaskingRule::from)
                .toList();
    }

    private static final String PI_GROUP_PREFIX = "PI_";

    @Transactional
    public CodeDto.ValueResponse createValue(String groupCode, CodeDto.ValueRequest req) {
        if (!groupRepo.existsById(groupCode)) {
            throw new ResourceNotFoundException("CodeGroup", groupCode);
        }
        CodeValue value = valueRepo.save(CodeValue.builder()
                .groupCode(groupCode)
                .value(req.getValue())
                .label(req.getLabel())
                .description(req.getDescription())
                .maskingType(req.getMaskingType())
                .maskingRule(req.getMaskingRule())
                .maskingExample(req.getMaskingExample())
                .sortOrder(req.getSortOrder())
                .active(req.getActive() == null || req.getActive())
                .build());
        return CodeDto.ValueResponse.from(value);
    }

    @Transactional
    public CodeDto.ValueResponse updateValue(String groupCode, Long id, CodeDto.ValueRequest req) {
        CodeValue value = valueRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("CodeValue", id));
        if (!value.getGroupCode().equals(groupCode)) {
            throw new BusinessException("해당 그룹의 코드값이 아닙니다");
        }
        value.setValue(req.getValue());
        value.setLabel(req.getLabel());
        value.setDescription(req.getDescription());
        value.setMaskingType(req.getMaskingType());
        value.setMaskingRule(req.getMaskingRule());
        value.setMaskingExample(req.getMaskingExample());
        value.setSortOrder(req.getSortOrder());
        if (req.getActive() != null) value.setActive(req.getActive());
        return CodeDto.ValueResponse.from(value);
    }

    @Transactional
    public void deleteValue(String groupCode, Long id) {
        CodeValue value = valueRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("CodeValue", id));
        if (!value.getGroupCode().equals(groupCode)) {
            throw new BusinessException("해당 그룹의 코드값이 아닙니다");
        }
        valueRepo.delete(value);
    }
}
