package com.monosun.secportal.committee.repository;

import com.monosun.secportal.committee.entity.CommitteeFile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CommitteeFileRepository extends JpaRepository<CommitteeFile, Long> {

    List<CommitteeFile> findByMeetingIdOrderByCreatedAtAsc(Long meetingId);
}
