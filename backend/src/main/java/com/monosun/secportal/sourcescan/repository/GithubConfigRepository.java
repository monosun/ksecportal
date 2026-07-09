package com.monosun.secportal.sourcescan.repository;

import com.monosun.secportal.sourcescan.entity.GithubConfig;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface GithubConfigRepository extends JpaRepository<GithubConfig, Long> {

    Optional<GithubConfig> findFirstByOrderByIdAsc();
}
