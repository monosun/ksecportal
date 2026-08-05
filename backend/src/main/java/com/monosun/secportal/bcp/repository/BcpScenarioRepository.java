package com.monosun.secportal.bcp.repository;

import com.monosun.secportal.bcp.entity.BcpScenario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface BcpScenarioRepository extends JpaRepository<BcpScenario, Long> {

    @Query("SELECT DISTINCT s FROM BcpScenario s LEFT JOIN FETCH s.steps ORDER BY s.createdAt DESC")
    List<BcpScenario> findAllWithSteps();

    @Query("SELECT s FROM BcpScenario s LEFT JOIN FETCH s.steps WHERE s.id = :id")
    Optional<BcpScenario> findByIdWithSteps(Long id);
}
