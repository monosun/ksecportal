package com.monosun.secportal.privacy.repository;

import com.monosun.secportal.privacy.entity.ContractorCheck;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ContractorCheckRepository extends JpaRepository<ContractorCheck, Long> {

    Optional<ContractorCheck> findByContractorIdAndCheckYear(Long contractorId, int checkYear);

    @Query("SELECT DISTINCT c.checkYear FROM ContractorCheck c ORDER BY c.checkYear DESC")
    List<Integer> findDistinctYears();

    @Query("SELECT c FROM ContractorCheck c JOIN FETCH c.contractor WHERE c.checkYear = :year ORDER BY c.contractor.name ASC")
    List<ContractorCheck> findByYearWithContractor(@Param("year") int year);

    @Query("SELECT c FROM ContractorCheck c JOIN FETCH c.contractor WHERE c.contractor.id = :contractorId ORDER BY c.checkYear DESC")
    List<ContractorCheck> findByContractorIdWithContractor(@Param("contractorId") Long contractorId);

    @org.springframework.data.jpa.repository.Modifying
    @Query("DELETE FROM ContractorCheck c WHERE c.contractor.id = :contractorId")
    void deleteByContractorId(@Param("contractorId") Long contractorId);

    @Query("SELECT c.contractor.id, COUNT(c) FROM ContractorCheck c GROUP BY c.contractor.id")
    List<Object[]> countGroupByContractor();
}
