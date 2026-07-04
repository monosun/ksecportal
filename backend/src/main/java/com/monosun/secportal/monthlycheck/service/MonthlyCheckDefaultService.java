package com.monosun.secportal.monthlycheck.service;

import com.monosun.secportal.common.exception.ResourceNotFoundException;
import com.monosun.secportal.monthlycheck.dto.MonthlyCheckDefaultDto;
import com.monosun.secportal.monthlycheck.entity.MonthlyCheckDefault;
import com.monosun.secportal.monthlycheck.entity.MonthlyCheckItem;
import com.monosun.secportal.monthlycheck.repository.MonthlyCheckDefaultRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MonthlyCheckDefaultService {

    private final MonthlyCheckDefaultRepository defaultRepository;

    @Transactional(readOnly = true)
    public List<MonthlyCheckDefaultDto.Response> list() {
        return defaultRepository.findAllByOrderBySortOrderAsc().stream()
                .map(MonthlyCheckDefaultDto.Response::from)
                .collect(Collectors.toList());
    }

    @Transactional
    public MonthlyCheckDefaultDto.Response create(MonthlyCheckDefaultDto.Request req) {
        MonthlyCheckDefault d = MonthlyCheckDefault.builder()
                .priority(MonthlyCheckItem.Priority.valueOf(req.getPriority()))
                .category(req.getCategory())
                .itemName(req.getItemName())
                .checkMethod(req.getCheckMethod())
                .checkExample(req.getCheckExample())
                .sortOrder(req.getSortOrder())
                .active(req.isActive())
                .build();
        return MonthlyCheckDefaultDto.Response.from(defaultRepository.save(d));
    }

    @Transactional
    public MonthlyCheckDefaultDto.Response update(Long id, MonthlyCheckDefaultDto.Request req) {
        MonthlyCheckDefault d = defaultRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("MonthlyCheckDefault", id));
        d.setPriority(MonthlyCheckItem.Priority.valueOf(req.getPriority()));
        d.setCategory(req.getCategory());
        d.setItemName(req.getItemName());
        d.setCheckMethod(req.getCheckMethod());
        d.setCheckExample(req.getCheckExample());
        d.setSortOrder(req.getSortOrder());
        d.setActive(req.isActive());
        return MonthlyCheckDefaultDto.Response.from(defaultRepository.save(d));
    }

    @Transactional
    public void delete(Long id) {
        if (!defaultRepository.existsById(id)) {
            throw new ResourceNotFoundException("MonthlyCheckDefault", id);
        }
        defaultRepository.deleteById(id);
    }
}
