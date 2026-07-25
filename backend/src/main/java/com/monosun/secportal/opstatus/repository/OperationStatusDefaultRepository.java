package com.monosun.secportal.opstatus.repository;

import com.monosun.secportal.opstatus.entity.OperationStatusDefault;
import com.monosun.secportal.opstatus.entity.OperationStatusItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OperationStatusDefaultRepository extends JpaRepository<OperationStatusDefault, Long> {

    List<OperationStatusDefault> findAllByOrderByTypeAscSortOrderAscIdAsc();

    List<OperationStatusDefault> findByTypeOrderBySortOrderAscIdAsc(OperationStatusItem.Type type);

    List<OperationStatusDefault> findByTypeAndActiveTrueOrderBySortOrderAscIdAsc(OperationStatusItem.Type type);

    long countByType(OperationStatusItem.Type type);
}
