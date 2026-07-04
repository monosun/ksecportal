package com.monosun.secportal.privacy.repository;

import com.monosun.secportal.privacy.entity.ContractorCheckResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ContractorCheckResultRepository extends JpaRepository<ContractorCheckResult, Long> {

    @Query("SELECT r FROM ContractorCheckResult r JOIN FETCH r.checkItem i WHERE r.check.id = :checkId ORDER BY i.sortOrder ASC, i.id ASC")
    List<ContractorCheckResult> findByCheckIdWithItem(@Param("checkId") Long checkId);

    Optional<ContractorCheckResult> findByCheckIdAndCheckItemId(Long checkId, Long checkItemId);

    @Modifying
    @Query("DELETE FROM ContractorCheckResult r WHERE r.check.id = :checkId")
    void deleteByCheckId(@Param("checkId") Long checkId);

    @Modifying
    @Query("DELETE FROM ContractorCheckResult r WHERE r.check.contractor.id = :contractorId")
    void deleteByContractorId(@Param("contractorId") Long contractorId);
}
