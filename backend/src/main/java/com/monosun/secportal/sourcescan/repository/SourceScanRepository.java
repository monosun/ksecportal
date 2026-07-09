package com.monosun.secportal.sourcescan.repository;

import com.monosun.secportal.sourcescan.entity.SourceScan;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SourceScanRepository extends JpaRepository<SourceScan, Long> {

    Page<SourceScan> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
