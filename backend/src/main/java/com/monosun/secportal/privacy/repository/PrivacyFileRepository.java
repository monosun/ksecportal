package com.monosun.secportal.privacy.repository;

import com.monosun.secportal.privacy.entity.PrivacyFile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PrivacyFileRepository extends JpaRepository<PrivacyFile, Long> {

    List<PrivacyFile> findAllByOrderByNameAsc();

    long countByStatus(PrivacyFile.Status status);

    long countBySensitiveInfoTrue();

    long countByUniqueIdentifierTrue();
}
