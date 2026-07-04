package com.monosun.secportal.backup.repository;

import com.monosun.secportal.backup.entity.BackupHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BackupHistoryRepository extends JpaRepository<BackupHistory, Long> {
    List<BackupHistory> findAllByOrderByCreatedAtDesc();
}
