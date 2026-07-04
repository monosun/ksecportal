package com.monosun.secportal.isms.repository;

import com.monosun.secportal.isms.entity.IsmsPolicyMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface IsmsPolicyMappingRepository extends JpaRepository<IsmsPolicyMapping, Long> {

    @Query("SELECT m FROM IsmsPolicyMapping m JOIN FETCH m.policy WHERE m.ismsItem.id IN :itemIds")
    List<IsmsPolicyMapping> findByIsmsItemIdIn(@Param("itemIds") List<Long> itemIds);

    boolean existsByIsmsItemIdAndPolicyId(Long ismsItemId, Long policyId);

    void deleteByIsmsItemIdAndPolicyId(Long ismsItemId, Long policyId);
}
