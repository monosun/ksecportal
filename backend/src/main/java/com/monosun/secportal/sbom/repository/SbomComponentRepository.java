package com.monosun.secportal.sbom.repository;

import com.monosun.secportal.sbom.entity.SbomComponent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SbomComponentRepository extends JpaRepository<SbomComponent, Long> {

    Optional<SbomComponent> findBySoftwareIdAndLibraryNameAndLibraryVersion(Long softwareId, String libraryName, String libraryVersion);
}
