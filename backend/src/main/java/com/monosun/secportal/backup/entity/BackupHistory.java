package com.monosun.secportal.backup.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "backup_history")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BackupHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String filename;
    private Long fileSize;

    @Builder.Default
    private String backupType = "MANUAL";

    @Builder.Default
    private String status = "SUCCESS";

    private String message;

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
