package com.monosun.secportal.isms.repository;

import com.monosun.secportal.isms.entity.IsmsItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface IsmsItemRepository extends JpaRepository<IsmsItem, Long> {
    List<IsmsItem> findAllByOrderBySortOrderAsc();
    List<IsmsItem> findByDomainCodeOrderBySortOrderAsc(String domainCode);
    List<IsmsItem> findBySectionNumOrderBySortOrderAsc(int sectionNum);
    Optional<IsmsItem> findByItemCode(String itemCode);
    boolean existsByItemCode(String itemCode);
}
