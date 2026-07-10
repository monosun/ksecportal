package com.monosun.secportal.asset.repository;

import com.monosun.secportal.asset.entity.AssetSnapshotItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AssetSnapshotItemRepository extends JpaRepository<AssetSnapshotItem, Long> {
    List<AssetSnapshotItem> findBySnapshotIdOrderByIdAsc(Long snapshotId);

    @Modifying
    @Query("DELETE FROM AssetSnapshotItem i WHERE i.snapshotId = :snapshotId")
    void deleteBySnapshotId(@Param("snapshotId") Long snapshotId);
}
