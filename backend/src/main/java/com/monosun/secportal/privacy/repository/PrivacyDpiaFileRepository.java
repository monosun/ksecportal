package com.monosun.secportal.privacy.repository;

import com.monosun.secportal.privacy.entity.PrivacyDpiaFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface PrivacyDpiaFileRepository extends JpaRepository<PrivacyDpiaFile, Long> {

    List<PrivacyDpiaFile> findByDpiaIdOrderByIdDesc(Long dpiaId);

    /** 목록 화면의 첨부 건수 표시용 — 평가 건별로 count 를 돌리지 않도록 한 번에 집계한다. */
    @Query("select f.dpia.id, count(f) from PrivacyDpiaFile f group by f.dpia.id")
    List<Object[]> countGroupByDpia();
}
