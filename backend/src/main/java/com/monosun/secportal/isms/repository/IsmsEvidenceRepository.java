package com.monosun.secportal.isms.repository;

import com.monosun.secportal.isms.entity.IsmsEvidence;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Map;

public interface IsmsEvidenceRepository extends JpaRepository<IsmsEvidence, Long> {
    List<IsmsEvidence> findByItemIdAndYearOrderByCreatedAtDesc(Long itemId, int year);
    List<IsmsEvidence> findByItemIdOrderByYearDescCreatedAtDesc(Long itemId);

    @Query("SELECT e.item.id, e.status, COUNT(e) FROM IsmsEvidence e WHERE e.year = :year GROUP BY e.item.id, e.status")
    List<Object[]> countByItemAndStatusForYear(@Param("year") int year);

    long countByItemIdAndYear(Long itemId, int year);

    @Query("SELECT e FROM IsmsEvidence e JOIN FETCH e.item WHERE e.year = :year ORDER BY e.item.sortOrder ASC, e.createdAt DESC")
    List<IsmsEvidence> findByYearOrderByItemSortOrder(@Param("year") int year);

    @Query("""
        SELECT e FROM IsmsEvidence e JOIN FETCH e.item i
        WHERE e.year = :year AND e.item.id <> :excludeItemId
          AND e.sourceEvidence IS NULL AND e.filePath IS NOT NULL
          AND (:kw = '' OR e.title LIKE CONCAT('%', :kw, '%')
               OR e.fileName LIKE CONCAT('%', :kw, '%')
               OR i.itemCode LIKE CONCAT('%', :kw, '%')
               OR i.itemName LIKE CONCAT('%', :kw, '%'))
        ORDER BY i.sortOrder ASC, e.createdAt DESC
        """)
    List<IsmsEvidence> searchForRef(@Param("year") int year,
                                    @Param("excludeItemId") Long excludeItemId,
                                    @Param("kw") String kw);
}
