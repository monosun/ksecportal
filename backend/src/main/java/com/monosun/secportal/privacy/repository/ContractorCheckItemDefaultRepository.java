package com.monosun.secportal.privacy.repository;

import com.monosun.secportal.privacy.entity.ContractorCheckItemDefault;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ContractorCheckItemDefaultRepository extends JpaRepository<ContractorCheckItemDefault, Long> {
    List<ContractorCheckItemDefault> findAllByOrderBySortOrderAscIdAsc();
    boolean existsByItemNameAndCategory(String itemName, String category);
}
