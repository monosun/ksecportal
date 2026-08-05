package com.monosun.secportal.bcp.repository;

import com.monosun.secportal.bcp.entity.BcpExercise;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface BcpExerciseRepository extends JpaRepository<BcpExercise, Long> {

    @Query("SELECT DISTINCT e FROM BcpExercise e LEFT JOIN FETCH e.scenario LEFT JOIN FETCH e.steps ORDER BY e.createdAt DESC")
    List<BcpExercise> findAllWithSteps();

    @Query("SELECT e FROM BcpExercise e LEFT JOIN FETCH e.scenario LEFT JOIN FETCH e.steps WHERE e.id = :id")
    Optional<BcpExercise> findByIdWithSteps(Long id);

    long countByScenarioId(Long scenarioId);
}
