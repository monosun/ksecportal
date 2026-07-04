package com.monosun.secportal.committee.repository;

import com.monosun.secportal.committee.entity.CommitteeMeeting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface CommitteeMeetingRepository extends JpaRepository<CommitteeMeeting, Long> {

    List<CommitteeMeeting> findByYearOrderBySessionNoAsc(int year);

    @Query("SELECT DISTINCT m.year FROM CommitteeMeeting m ORDER BY m.year DESC")
    List<Integer> findDistinctYears();

    boolean existsByYearAndSessionNo(int year, int sessionNo);
}
