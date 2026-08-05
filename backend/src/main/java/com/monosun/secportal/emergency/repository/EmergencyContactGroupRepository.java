package com.monosun.secportal.emergency.repository;

import com.monosun.secportal.emergency.entity.EmergencyContactGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface EmergencyContactGroupRepository extends JpaRepository<EmergencyContactGroup, Long> {

    @Query("SELECT DISTINCT g FROM EmergencyContactGroup g LEFT JOIN FETCH g.contacts ORDER BY g.sortOrder ASC, g.id ASC")
    List<EmergencyContactGroup> findAllWithContacts();

    @Query("SELECT g FROM EmergencyContactGroup g LEFT JOIN FETCH g.contacts WHERE g.id = :id")
    Optional<EmergencyContactGroup> findByIdWithContacts(Long id);
}
