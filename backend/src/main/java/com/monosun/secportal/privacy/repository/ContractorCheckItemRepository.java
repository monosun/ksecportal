package com.monosun.secportal.privacy.repository;

import com.monosun.secportal.privacy.entity.ContractorCheckItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ContractorCheckItemRepository extends JpaRepository<ContractorCheckItem, Long> {
    List<ContractorCheckItem> findAllByOrderBySortOrderAscIdAsc();
    boolean existsByItemNameAndCategory(String itemName, String category);
}
