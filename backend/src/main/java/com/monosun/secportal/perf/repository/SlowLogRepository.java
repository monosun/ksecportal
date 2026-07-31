package com.monosun.secportal.perf.repository;

import com.monosun.secportal.perf.entity.SlowLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface SlowLogRepository extends JpaRepository<SlowLog, Long> {

    @Query("SELECT s FROM SlowLog s WHERE " +
           "(:logType IS NULL OR s.logType = :logType) AND " +
           "(:keyword IS NULL OR LOWER(s.target) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "   OR LOWER(s.detail) LIKE LOWER(CONCAT('%', :keyword, '%'))) AND " +
           "(:minMs IS NULL OR s.durationMs >= :minMs) AND " +
           "(:dateFrom IS NULL OR s.occurredAt >= :dateFrom) AND " +
           "(:dateTo IS NULL OR s.occurredAt <= :dateTo) " +
           "ORDER BY s.occurredAt DESC")
    Page<SlowLog> search(@Param("logType") SlowLog.LogType logType,
                         @Param("keyword") String keyword,
                         @Param("minMs") Long minMs,
                         @Param("dateFrom") LocalDateTime dateFrom,
                         @Param("dateTo") LocalDateTime dateTo,
                         Pageable pageable);

    long countByLogType(SlowLog.LogType logType);

    @Query("SELECT COALESCE(MAX(s.durationMs), 0) FROM SlowLog s")
    long findMaxDuration();

    @Modifying
    @Query("DELETE FROM SlowLog s WHERE s.occurredAt < :before")
    int deleteOlderThan(@Param("before") LocalDateTime before);
}
