package com.monosun.secportal.detection.repository;

import com.monosun.secportal.detection.entity.DetectionEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface DetectionEventRepository extends JpaRepository<DetectionEvent, Long> {

    Optional<DetectionEvent> findFirstBySourceIpAndCategoryAndStatusInAndLastSeenAtAfterOrderByIdDesc(
            String sourceIp, DetectionEvent.Category category,
            Collection<DetectionEvent.Status> statuses, LocalDateTime after);

    @Query("""
            SELECT e FROM DetectionEvent e
            WHERE (:category IS NULL OR e.category = :category)
              AND (:severity IS NULL OR e.severity = :severity)
              AND (:status IS NULL OR e.status = :status)
              AND (:ip IS NULL OR e.sourceIp LIKE CONCAT('%', :ip, '%'))
              AND (:from IS NULL OR e.lastSeenAt >= :from)
            ORDER BY e.lastSeenAt DESC
            """)
    Page<DetectionEvent> search(@Param("category") DetectionEvent.Category category,
                                @Param("severity") DetectionEvent.Severity severity,
                                @Param("status") DetectionEvent.Status status,
                                @Param("ip") String ip,
                                @Param("from") LocalDateTime from,
                                Pageable pageable);

    List<DetectionEvent> findByLastSeenAtAfter(LocalDateTime after);

    long countByStatus(DetectionEvent.Status status);

    @Modifying
    @Query("DELETE FROM DetectionEvent e WHERE e.lastSeenAt < :before AND e.status <> :keep")
    int deleteOlderThan(@Param("before") LocalDateTime before, @Param("keep") DetectionEvent.Status keep);
}
