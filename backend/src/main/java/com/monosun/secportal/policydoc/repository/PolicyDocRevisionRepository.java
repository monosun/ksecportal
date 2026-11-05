package com.monosun.secportal.policydoc.repository;

import com.monosun.secportal.policydoc.entity.PolicyDocRevision;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface PolicyDocRevisionRepository extends JpaRepository<PolicyDocRevision, Long> {

    /** 최신순 — 첫 번째가 현행본 */
    @Query("SELECT r FROM PolicyDocRevision r LEFT JOIN FETCH r.uploader WHERE r.document.id = :docId " +
           "ORDER BY r.revisionDate DESC, r.id DESC")
    List<PolicyDocRevision> findByDocId(@Param("docId") Long docId);

    /** 목록 화면에서 여러 문서의 개정본을 한 번에 가져온다 (최신순) */
    @Query("SELECT r FROM PolicyDocRevision r WHERE r.document.id IN :docIds " +
           "ORDER BY r.revisionDate DESC, r.id DESC")
    List<PolicyDocRevision> findByDocIds(@Param("docIds") Collection<Long> docIds);

    boolean existsByDocumentIdAndVersion(Long docId, String version);
}
