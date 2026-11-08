package com.monosun.secportal.detection.repository;

import com.monosun.secportal.detection.entity.BlockedIp;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface BlockedIpRepository extends JpaRepository<BlockedIp, Long> {

    List<BlockedIp> findByActiveTrue();

    List<BlockedIp> findByIpAndActiveTrue(String ip);

    List<BlockedIp> findAllByOrderByCreatedAtDesc();

    @Modifying
    @Query("DELETE FROM BlockedIp b WHERE b.active = false AND b.createdAt < :before")
    int deleteReleasedBefore(@Param("before") LocalDateTime before);
}
