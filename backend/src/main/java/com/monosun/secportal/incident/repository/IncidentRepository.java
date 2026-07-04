package com.monosun.secportal.incident.repository;

import com.monosun.secportal.incident.entity.Incident;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface IncidentRepository extends JpaRepository<Incident, Long> {

    @Query("SELECT i FROM Incident i WHERE " +
           "(:keyword IS NULL OR LOWER(i.title) LIKE LOWER(CONCAT('%', :keyword, '%'))) AND " +
           "(:severity IS NULL OR i.severity = :severity) AND " +
           "(:status IS NULL OR i.status = :status) AND " +
           "(:type IS NULL OR i.type = :type)")
    Page<Incident> search(
            @Param("keyword") String keyword,
            @Param("severity") Incident.Severity severity,
            @Param("status") Incident.Status status,
            @Param("type") Incident.IncidentType type,
            Pageable pageable);

    long countByStatus(Incident.Status status);
    long countBySeverity(Incident.Severity severity);

    List<Incident> findAllByOrderByCreatedAtDesc();
}
