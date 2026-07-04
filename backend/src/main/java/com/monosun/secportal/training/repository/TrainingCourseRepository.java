package com.monosun.secportal.training.repository;

import com.monosun.secportal.training.entity.TrainingCourse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TrainingCourseRepository extends JpaRepository<TrainingCourse, Long> {

    @Query("SELECT c FROM TrainingCourse c WHERE c.active = true AND " +
           "(:keyword IS NULL OR LOWER(c.title) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<TrainingCourse> search(@Param("keyword") String keyword, Pageable pageable);
}
