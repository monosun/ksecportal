package com.monosun.secportal.training.repository;

import com.monosun.secportal.training.entity.TrainingCompletion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface TrainingCompletionRepository extends JpaRepository<TrainingCompletion, Long> {
    boolean existsByCourseIdAndUserId(Long courseId, Long userId);
    Optional<TrainingCompletion> findByCourseIdAndUserId(Long courseId, Long userId);
    List<TrainingCompletion> findByUserId(Long userId);
    long countByCourseId(Long courseId);
    long countByCourseIdAndPassedTrue(Long courseId);

    @Query("SELECT COUNT(DISTINCT c.user.id) FROM TrainingCompletion c WHERE c.passed = true")
    long countDistinctPassedUsers();

    List<TrainingCompletion> findAllByOrderByCompletedAtDesc();
    List<TrainingCompletion> findByCourseIdOrderByCompletedAtDesc(Long courseId);
    List<TrainingCompletion> findByCourseId(Long courseId);
    void deleteByCourseId(Long courseId);
}
