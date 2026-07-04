package com.monosun.secportal.privacy.repository;

import com.monosun.secportal.privacy.entity.ContractorInspection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ContractorInspectionRepository extends JpaRepository<ContractorInspection, Long> {

    @Query("SELECT i FROM ContractorInspection i LEFT JOIN FETCH i.files WHERE i.contractor.id = :contractorId ORDER BY i.inspectionDate DESC")
    List<ContractorInspection> findByContractorIdWithFiles(Long contractorId);

    @Query("SELECT i FROM ContractorInspection i LEFT JOIN FETCH i.files WHERE i.id = :id")
    Optional<ContractorInspection> findByIdWithFiles(Long id);
}
