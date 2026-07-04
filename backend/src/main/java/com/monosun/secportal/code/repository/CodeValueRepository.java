package com.monosun.secportal.code.repository;

import com.monosun.secportal.code.entity.CodeValue;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CodeValueRepository extends JpaRepository<CodeValue, Long> {
    List<CodeValue> findByGroupCodeOrderBySortOrderAscLabelAsc(String groupCode);
    List<CodeValue> findByGroupCodeAndActiveTrueOrderBySortOrderAscLabelAsc(String groupCode);
    void deleteByGroupCode(String groupCode);
}
