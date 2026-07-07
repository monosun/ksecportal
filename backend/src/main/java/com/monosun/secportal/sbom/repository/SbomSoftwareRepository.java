package com.monosun.secportal.sbom.repository;

import com.monosun.secportal.sbom.entity.SbomSoftware;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SbomSoftwareRepository extends JpaRepository<SbomSoftware, Long> {

    @Query("SELECT s FROM SbomSoftware s WHERE " +
           "(:keyword IS NULL OR LOWER(s.name) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           " OR LOWER(s.version) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           " OR LOWER(s.vendor) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<SbomSoftware> search(@Param("keyword") String keyword, Pageable pageable);

    Optional<SbomSoftware> findByNameAndVersion(String name, String version);

    boolean existsByNameAndVersion(String name, String version);

    List<SbomSoftware> findAllByOrderByNameAscVersionAsc();
}
