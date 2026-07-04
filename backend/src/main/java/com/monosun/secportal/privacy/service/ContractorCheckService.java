package com.monosun.secportal.privacy.service;

import com.monosun.secportal.common.exception.BusinessException;
import com.monosun.secportal.common.exception.ResourceNotFoundException;
import com.monosun.secportal.privacy.dto.ContractorCheckDto;
import com.monosun.secportal.privacy.entity.*;
import com.monosun.secportal.privacy.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ContractorCheckService {

    private final ContractorCheckRepository checkRepository;
    private final ContractorCheckResultRepository resultRepository;
    private final ContractorCheckItemRepository itemRepository;
    private final ContractorRepository contractorRepository;

    @Transactional(readOnly = true)
    public List<Integer> listYears() {
        List<Integer> years = checkRepository.findDistinctYears();
        if (years.isEmpty()) {
            years = new ArrayList<>();
            years.add(java.time.Year.now().getValue());
        }
        return years;
    }

    @Transactional(readOnly = true)
    public List<ContractorCheckDto.CheckResponse> listByYear(int year) {
        return checkRepository.findByYearWithContractor(year).stream()
                .map(ContractorCheckDto.CheckResponse::fromWithStats)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ContractorCheckDto.CheckResponse> listByContractor(Long contractorId) {
        return checkRepository.findByContractorIdWithContractor(contractorId).stream()
                .map(ContractorCheckDto.CheckResponse::fromWithStats)
                .collect(Collectors.toList());
    }

    @Transactional
    public ContractorCheckDto.CheckResponse createOrGet(ContractorCheckDto.CheckRequest req) {
        Contractor contractor = contractorRepository.findById(req.getContractorId())
                .orElseThrow(() -> new ResourceNotFoundException("Contractor", req.getContractorId()));

        return checkRepository.findByContractorIdAndCheckYear(req.getContractorId(), req.getCheckYear())
                .map(existing -> {
                    if (req.getCheckDate() != null) existing.setCheckDate(req.getCheckDate());
                    if (req.getInspector() != null) existing.setInspector(req.getInspector());
                    if (req.getStatus() != null) existing.setStatus(ContractorCheck.Status.valueOf(req.getStatus()));
                    if (req.getNotes() != null) existing.setNotes(req.getNotes());
                    return ContractorCheckDto.CheckResponse.from(checkRepository.save(existing));
                })
                .orElseGet(() -> {
                    ContractorCheck check = ContractorCheck.builder()
                            .contractor(contractor)
                            .checkYear(req.getCheckYear())
                            .checkDate(req.getCheckDate())
                            .inspector(req.getInspector())
                            .status(req.getStatus() != null
                                    ? ContractorCheck.Status.valueOf(req.getStatus())
                                    : ContractorCheck.Status.PLANNED)
                            .notes(req.getNotes())
                            .build();
                    ContractorCheck saved = checkRepository.save(check);
                    initResults(saved);
                    return ContractorCheckDto.CheckResponse.from(saved);
                });
    }

    private void initResults(ContractorCheck check) {
        List<ContractorCheckItem> items = itemRepository.findAllByOrderBySortOrderAscIdAsc();
        List<ContractorCheckResult> results = items.stream()
                .map(item -> ContractorCheckResult.builder()
                        .check(check)
                        .checkItem(item)
                        .result(ContractorCheckResult.Result.NOT_CHECKED)
                        .build())
                .collect(Collectors.toList());
        resultRepository.saveAll(results);
    }

    @Transactional
    public ContractorCheckDto.CheckResponse update(Long id, ContractorCheckDto.CheckRequest req) {
        ContractorCheck check = checkRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ContractorCheck", id));
        if (req.getCheckDate() != null) check.setCheckDate(req.getCheckDate());
        if (req.getInspector() != null) check.setInspector(req.getInspector());
        if (req.getStatus() != null) check.setStatus(ContractorCheck.Status.valueOf(req.getStatus()));
        if (req.getNotes() != null) check.setNotes(req.getNotes());
        return ContractorCheckDto.CheckResponse.from(checkRepository.save(check));
    }

    @Transactional
    public void delete(Long id) {
        if (!checkRepository.existsById(id)) {
            throw new ResourceNotFoundException("ContractorCheck", id);
        }
        checkRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public ContractorCheckDto.CheckDetailResponse getDetail(Long id) {
        ContractorCheck check = checkRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ContractorCheck", id));

        List<ContractorCheckResult> results = resultRepository
                .findByCheckIdWithItem(id);

        List<ContractorCheckItem> allItems = itemRepository.findAllByOrderBySortOrderAscIdAsc();
        Map<Long, ContractorCheckResult> resultMap = results.stream()
                .collect(Collectors.toMap(r -> r.getCheckItem().getId(), r -> r));

        List<ContractorCheckDto.ResultResponse> resultResponses = allItems.stream()
                .map(item -> {
                    ContractorCheckResult r = resultMap.get(item.getId());
                    if (r != null) return ContractorCheckDto.ResultResponse.from(r);
                    return ContractorCheckDto.ResultResponse.builder()
                            .checkId(check.getId())
                            .checkItemId(item.getId())
                            .checkItemCategory(item.getCategory())
                            .checkItemName(item.getItemName())
                            .checkMethod(item.getCheckMethod())
                            .checkStandard(item.getCheckStandard())
                            .required(item.isRequired())
                            .sortOrder(item.getSortOrder())
                            .result(ContractorCheckResult.Result.NOT_CHECKED.name())
                            .build();
                })
                .collect(Collectors.toList());

        return ContractorCheckDto.CheckDetailResponse.builder()
                .check(ContractorCheckDto.CheckResponse.from(check))
                .results(resultResponses)
                .build();
    }

    @Transactional
    public ContractorCheckDto.ResultResponse saveResult(Long checkId, ContractorCheckDto.ResultRequest req) {
        ContractorCheck check = checkRepository.findById(checkId)
                .orElseThrow(() -> new ResourceNotFoundException("ContractorCheck", checkId));
        ContractorCheckItem item = itemRepository.findById(req.getCheckItemId())
                .orElseThrow(() -> new ResourceNotFoundException("ContractorCheckItem", req.getCheckItemId()));

        ContractorCheckResult.Result resultEnum;
        try {
            resultEnum = ContractorCheckResult.Result.valueOf(req.getResult());
        } catch (IllegalArgumentException e) {
            throw new BusinessException("유효하지 않은 점검결과: " + req.getResult());
        }

        ContractorCheckResult result = resultRepository.findByCheckIdAndCheckItemId(checkId, req.getCheckItemId())
                .orElseGet(() -> ContractorCheckResult.builder().check(check).checkItem(item).build());
        result.setResult(resultEnum);
        result.setNotes(req.getNotes());
        return ContractorCheckDto.ResultResponse.from(resultRepository.save(result));
    }

    @Transactional
    public void setAllResults(Long checkId, String result) {
        if (!checkRepository.existsById(checkId)) {
            throw new ResourceNotFoundException("ContractorCheck", checkId);
        }
        ContractorCheckResult.Result resultEnum;
        try {
            resultEnum = ContractorCheckResult.Result.valueOf(result);
        } catch (IllegalArgumentException e) {
            throw new BusinessException("유효하지 않은 점검결과: " + result);
        }
        List<ContractorCheckResult> results = resultRepository.findByCheckIdWithItem(checkId);
        results.forEach(r -> {
            r.setResult(resultEnum);
            if (resultEnum == ContractorCheckResult.Result.NOT_CHECKED) r.setNotes(null);
        });
        resultRepository.saveAll(results);
    }

    @Transactional
    public void syncResults(Long checkId) {
        ContractorCheck check = checkRepository.findById(checkId)
                .orElseThrow(() -> new ResourceNotFoundException("ContractorCheck", checkId));
        List<ContractorCheckItem> allItems = itemRepository.findAllByOrderBySortOrderAscIdAsc();
        List<ContractorCheckResult> existing = resultRepository
                .findByCheckIdWithItem(checkId);
        Map<Long, ContractorCheckResult> existingMap = existing.stream()
                .collect(Collectors.toMap(r -> r.getCheckItem().getId(), r -> r));

        List<ContractorCheckResult> toAdd = allItems.stream()
                .filter(item -> !existingMap.containsKey(item.getId()))
                .map(item -> ContractorCheckResult.builder()
                        .check(check)
                        .checkItem(item)
                        .result(ContractorCheckResult.Result.NOT_CHECKED)
                        .build())
                .collect(Collectors.toList());
        if (!toAdd.isEmpty()) resultRepository.saveAll(toAdd);
    }
}
