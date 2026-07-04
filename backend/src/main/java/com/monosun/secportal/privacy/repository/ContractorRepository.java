package com.monosun.secportal.privacy.repository;

import com.monosun.secportal.privacy.entity.Contractor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ContractorRepository extends JpaRepository<Contractor, Long> {

    @Query("SELECT DISTINCT c FROM Contractor c LEFT JOIN FETCH c.inspections ORDER BY c.name ASC")
    List<Contractor> findAllWithInspections();

    @Query("SELECT c FROM Contractor c LEFT JOIN FETCH c.inspections WHERE c.id = :id")
    Optional<Contractor> findByIdWithInspections(@org.springframework.data.repository.query.Param("id") Long id);
}
