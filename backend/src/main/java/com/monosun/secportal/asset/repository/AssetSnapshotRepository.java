package com.monosun.secportal.asset.repository;

import com.monosun.secportal.asset.entity.AssetSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AssetSnapshotRepository extends JpaRepository<AssetSnapshot, Long> {
    List<AssetSnapshot> findAllByOrderByCreatedAtDesc();
}
