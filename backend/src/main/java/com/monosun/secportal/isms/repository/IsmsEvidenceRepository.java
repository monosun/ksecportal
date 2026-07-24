package com.monosun.secportal.isms.repository;

import com.monosun.secportal.isms.entity.IsmsEvidence;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
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

    /** 대상 연도보다 앞선 연도 중 증적이 등록된 연도 (최신순) */
    @Query("SELECT DISTINCT e.year FROM IsmsEvidence e WHERE e.year < :year ORDER BY e.year DESC")
    List<Integer> findYearsBefore(@Param("year") int year);

    /** 해당 연도에 이미 증적이 있는 항목 ID */
    @Query("SELECT DISTINCT e.item.id FROM IsmsEvidence e WHERE e.year = :year")
    List<Long> findItemIdsByYear(@Param("year") int year);

    /** 전년도 가져오기로 생성된 증적 (가져오기 초기화 대상) */
    List<IsmsEvidence> findByYearAndCopiedFromYearIsNotNull(int year);

    long countByYearAndCopiedFromYearIsNotNull(int year);

    /** 해당 연도 가져오기 증적의 원본 연도 (여러 번 가져온 경우 가장 최근 원본) */
    @Query("SELECT MAX(e.copiedFromYear) FROM IsmsEvidence e WHERE e.year = :year AND e.copiedFromYear IS NOT NULL")
    Integer findCopiedFromYear(@Param("year") int year);

    /** 지정한 증적들을 참조하고 있는 증적 (초기화 시 함께 정리) */
    List<IsmsEvidence> findBySourceEvidenceIdIn(Collection<Long> sourceIds);

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
