package com.monosun.secportal.privacy.service;

import com.monosun.secportal.common.exception.ResourceNotFoundException;
import com.monosun.secportal.privacy.dto.ContractorCheckItemDto;
import com.monosun.secportal.privacy.entity.ContractorCheckItem;
import com.monosun.secportal.privacy.entity.ContractorCheckItemDefault;
import com.monosun.secportal.privacy.repository.ContractorCheckItemDefaultRepository;
import com.monosun.secportal.privacy.repository.ContractorCheckItemRepository;
import com.monosun.secportal.privacy.repository.ContractorCheckResultRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ContractorCheckItemService {

    private final ContractorCheckItemRepository itemRepository;
    private final ContractorCheckItemDefaultRepository defaultRepository;
    private final ContractorCheckResultRepository resultRepository;

    @Transactional(readOnly = true)
    public List<ContractorCheckItemDto.ItemResponse> list() {
        return itemRepository.findAllByOrderBySortOrderAscIdAsc().stream()
                .map(ContractorCheckItemDto.ItemResponse::from)
                .collect(Collectors.toList());
    }

    @Transactional
    public ContractorCheckItemDto.ItemResponse create(ContractorCheckItemDto.ItemRequest req) {
        ContractorCheckItem item = ContractorCheckItem.builder()
                .category(req.getCategory())
                .itemName(req.getItemName())
                .checkMethod(req.getCheckMethod())
                .checkStandard(req.getCheckStandard())
                .isRequired(req.isRequired())
                .sortOrder(req.getSortOrder())
                .build();
        return ContractorCheckItemDto.ItemResponse.from(itemRepository.save(item));
    }

    @Transactional
    public ContractorCheckItemDto.ItemResponse update(Long id, ContractorCheckItemDto.ItemRequest req) {
        ContractorCheckItem item = itemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ContractorCheckItem", id));
        item.setCategory(req.getCategory());
        item.setItemName(req.getItemName());
        item.setCheckMethod(req.getCheckMethod());
        item.setCheckStandard(req.getCheckStandard());
        item.setRequired(req.isRequired());
        item.setSortOrder(req.getSortOrder());
        return ContractorCheckItemDto.ItemResponse.from(itemRepository.save(item));
    }

    @Transactional
    public void delete(Long id) {
        if (!itemRepository.existsById(id)) {
            throw new ResourceNotFoundException("ContractorCheckItem", id);
        }
        itemRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public ContractorCheckItemDto.DefaultCheckResponse checkDefaults() {
        List<ContractorCheckItemDefault> defaults = defaultRepository.findAllByOrderBySortOrderAscIdAsc();
        Set<String> existingNames = itemRepository.findAllByOrderBySortOrderAscIdAsc().stream()
                .map(ContractorCheckItem::getItemName)
                .collect(Collectors.toSet());
        boolean alreadyLoaded = !defaults.isEmpty() && defaults.stream()
                .map(ContractorCheckItemDefault::getItemName)
                .allMatch(existingNames::contains);
        return ContractorCheckItemDto.DefaultCheckResponse.builder()
                .alreadyLoaded(alreadyLoaded)
                .existingCount((int) itemRepository.count())
                .defaultCount(defaults.size())
                .build();
    }

    @Transactional
    public int loadDefaults() {
        List<ContractorCheckItemDefault> defaults = defaultRepository.findAllByOrderBySortOrderAscIdAsc();
        Set<String> existingNames = itemRepository.findAllByOrderBySortOrderAscIdAsc().stream()
                .map(ContractorCheckItem::getItemName)
                .collect(Collectors.toSet());
        List<ContractorCheckItem> toSave = new ArrayList<>();
        for (ContractorCheckItemDefault d : defaults) {
            if (!existingNames.contains(d.getItemName())) {
                toSave.add(ContractorCheckItem.builder()
                        .category(d.getCategory())
                        .itemName(d.getItemName())
                        .checkMethod(d.getCheckMethod())
                        .checkStandard(d.getCheckStandard())
                        .isRequired(true)
                        .sortOrder(d.getSortOrder())
                        .build());
            }
        }
        if (!toSave.isEmpty()) {
            itemRepository.saveAll(toSave);
        }
        return toSave.size();
    }

    @Transactional(readOnly = true)
    public List<ContractorCheckItemDto.DefaultItemResponse> listDefaults() {
        return defaultRepository.findAllByOrderBySortOrderAscIdAsc().stream()
                .map(ContractorCheckItemDto.DefaultItemResponse::from)
                .collect(Collectors.toList());
    }

    @Transactional
    public ContractorCheckItemDto.DefaultItemResponse createDefault(ContractorCheckItemDto.DefaultItemRequest req) {
        ContractorCheckItemDefault item = ContractorCheckItemDefault.builder()
                .category(req.getCategory())
                .itemName(req.getItemName())
                .checkMethod(req.getCheckMethod())
                .checkStandard(req.getCheckStandard())
                .sortOrder(req.getSortOrder())
                .build();
        return ContractorCheckItemDto.DefaultItemResponse.from(defaultRepository.save(item));
    }

    @Transactional
    public ContractorCheckItemDto.DefaultItemResponse updateDefault(Long id, ContractorCheckItemDto.DefaultItemRequest req) {
        ContractorCheckItemDefault item = defaultRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ContractorCheckItemDefault", id));
        item.setCategory(req.getCategory());
        item.setItemName(req.getItemName());
        item.setCheckMethod(req.getCheckMethod());
        item.setCheckStandard(req.getCheckStandard());
        item.setSortOrder(req.getSortOrder());
        return ContractorCheckItemDto.DefaultItemResponse.from(defaultRepository.save(item));
    }

    @Transactional
    public void deleteDefault(Long id) {
        if (!defaultRepository.existsById(id)) {
            throw new ResourceNotFoundException("ContractorCheckItemDefault", id);
        }
        defaultRepository.deleteById(id);
    }

    @Transactional
    public int resetToDefaults() {
        // 결과 테이블 먼저 삭제 (FK: check_item_id → contractor_check_items)
        resultRepository.deleteAllInBatch();
        itemRepository.deleteAllInBatch();
        List<ContractorCheckItemDefault> defaults = defaultRepository.findAllByOrderBySortOrderAscIdAsc();
        List<ContractorCheckItem> toSave = defaults.stream()
                .map(d -> ContractorCheckItem.builder()
                        .category(d.getCategory())
                        .itemName(d.getItemName())
                        .checkMethod(d.getCheckMethod())
                        .checkStandard(d.getCheckStandard())
                        .isRequired(true)
                        .sortOrder(d.getSortOrder())
                        .build())
                .collect(Collectors.toList());
        itemRepository.saveAll(toSave);
        return toSave.size();
    }
}
