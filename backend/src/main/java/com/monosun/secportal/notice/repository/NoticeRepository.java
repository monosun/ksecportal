package com.monosun.secportal.notice.repository;

import com.monosun.secportal.notice.entity.Notice;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NoticeRepository extends JpaRepository<Notice, Long> {
    List<Notice> findAllByActiveTrueOrderByPinnedDescCreatedAtDesc();
    List<Notice> findAllByOrderByPinnedDescCreatedAtDesc();
}
