package com.monosun.secportal.errorlog.repository;

import com.monosun.secportal.errorlog.entity.ErrorLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface ErrorLogRepository extends JpaRepository<ErrorLog, Long> {

    String FILTER = "(:level IS NULL OR e.level = :level) AND " +
            "(:source IS NULL OR e.source = :source) AND " +
            "(:status IS NULL OR e.status = :status) AND " +
            "(:dateFrom IS NULL OR e.occurredAt >= :dateFrom) AND " +
            "(:dateTo IS NULL OR e.occurredAt <= :dateTo) AND " +
            "(:keyword IS NULL OR e.exceptionType LIKE %:keyword% OR e.message LIKE %:keyword% " +
            " OR e.requestUri LIKE %:keyword% OR e.userName LIKE %:keyword%)";

    @Query("SELECT e FROM ErrorLog e WHERE " + FILTER + " ORDER BY e.occurredAt DESC")
    Page<ErrorLog> search(@Param("level") ErrorLog.Level level,
                          @Param("source") ErrorLog.Source source,
                          @Param("status") ErrorLog.Status status,
                          @Param("keyword") String keyword,
                          @Param("dateFrom") LocalDateTime dateFrom,
                          @Param("dateTo") LocalDateTime dateTo,
                          Pageable pageable);

    @Query("SELECT e FROM ErrorLog e WHERE " + FILTER + " ORDER BY e.occurredAt DESC")
    List<ErrorLog> searchAll(@Param("level") ErrorLog.Level level,
                             @Param("source") ErrorLog.Source source,
                             @Param("status") ErrorLog.Status status,
                             @Param("keyword") String keyword,
                             @Param("dateFrom") LocalDateTime dateFrom,
                             @Param("dateTo") LocalDateTime dateTo,
                             Pageable pageable);

    long countByStatus(ErrorLog.Status status);

    long countByLevelAndOccurredAtAfter(ErrorLog.Level level, LocalDateTime from);

    long countByOccurredAtAfter(LocalDateTime from);

    /** 같은 원인(fingerprint)으로 반복되는 오류 상위 목록 — [예외타입, 요청경로, 건수, 최근발생] */
    @Query("SELECT e.exceptionType, e.requestUri, COUNT(e), MAX(e.occurredAt) FROM ErrorLog e " +
           "WHERE e.occurredAt >= :from GROUP BY e.fingerprint, e.exceptionType, e.requestUri " +
           "ORDER BY COUNT(e) DESC")
    List<Object[]> topRepeated(@Param("from") LocalDateTime from, Pageable pageable);

    long deleteByOccurredAtBefore(LocalDateTime before);

    long deleteByStatusIn(List<ErrorLog.Status> statuses);
}
