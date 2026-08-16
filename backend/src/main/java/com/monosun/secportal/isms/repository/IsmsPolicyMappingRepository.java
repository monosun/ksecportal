package com.monosun.secportal.isms.repository;

import com.monosun.secportal.isms.entity.IsmsPolicyMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface IsmsPolicyMappingRepository extends JpaRepository<IsmsPolicyMapping, Long> {

    /** 조 단위 매핑은 policyArticle 이 null 일 수 있으므로 LEFT JOIN FETCH 로 함께 읽는다. */
    @Query("SELECT m FROM IsmsPolicyMapping m JOIN FETCH m.policy LEFT JOIN FETCH m.policyArticle " +
           "WHERE m.ismsItem.id IN :itemIds")
    List<IsmsPolicyMapping> findByIsmsItemIdIn(@Param("itemIds") List<Long> itemIds);

    // ── 장(章) 전체 매핑 ─────────────────────────────────────────────────────

    boolean existsByIsmsItemIdAndPolicyIdAndPolicyArticleIsNull(Long ismsItemId, Long policyId);

    void deleteByIsmsItemIdAndPolicyIdAndPolicyArticleIsNull(Long ismsItemId, Long policyId);

    // ── 조(條) 단위 매핑 ─────────────────────────────────────────────────────

    boolean existsByIsmsItemIdAndPolicyArticleId(Long ismsItemId, Long policyArticleId);

    void deleteByIsmsItemIdAndPolicyArticleId(Long ismsItemId, Long policyArticleId);
}
