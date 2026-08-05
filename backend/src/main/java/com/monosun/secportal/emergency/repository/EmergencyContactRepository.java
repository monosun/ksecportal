package com.monosun.secportal.emergency.repository;

import com.monosun.secportal.emergency.entity.EmergencyContact;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmergencyContactRepository extends JpaRepository<EmergencyContact, Long> {

    long countByGroupId(Long groupId);
}
