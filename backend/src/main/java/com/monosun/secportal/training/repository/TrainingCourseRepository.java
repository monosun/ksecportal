package com.monosun.secportal.training.repository;

import com.monosun.secportal.training.entity.TrainingCourse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TrainingCourseRepository extends JpaRepository<TrainingCourse, Long> {

    @Query("SELECT c FROM TrainingCourse c WHERE c.active = true AND " +
           "(:keyword IS NULL OR LOWER(c.title) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "   OR LOWER(c.description) LIKE LOWER(CONCAT('%', :keyword, '%'))) AND " +
           "(:mandatory IS NULL OR c.mandatory = :mandatory) AND " +
           "(:contentType IS NULL OR c.contentType = :contentType)")
    Page<TrainingCourse> search(@Param("keyword") String keyword,
                                @Param("mandatory") Boolean mandatory,
                                @Param("contentType") TrainingCourse.ContentType contentType,
                                Pageable pageable);
}
