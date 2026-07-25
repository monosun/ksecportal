package com.monosun.secportal.opstatus.repository;

import com.monosun.secportal.opstatus.entity.OperationStatusItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface OperationStatusItemRepository extends JpaRepository<OperationStatusItem, Long> {

    List<OperationStatusItem> findByYearOrderByTypeAscSortOrderAscIdAsc(Integer year);

    List<OperationStatusItem> findByYearAndTypeOrderBySortOrderAscIdAsc(Integer year, OperationStatusItem.Type type);

    boolean existsByYearAndType(Integer year, OperationStatusItem.Type type);

    long countByYear(Integer year);

    void deleteByYearAndType(Integer year, OperationStatusItem.Type type);

    @Query("SELECT DISTINCT i.year FROM OperationStatusItem i ORDER BY i.year DESC")
    List<Integer> findYears();
}
