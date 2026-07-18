package com.monosun.secportal.privacy.entity;

import com.monosun.secportal.auth.entity.User;
import com.monosun.secportal.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

/** 개인정보 영향평가(DPIA)의 보고서 등 첨부파일 — 평가 → 개선 → 완료 단계별 산출물을 보관한다. */
@Entity
@Table(name = "privacy_dpia_files",
        indexes = @Index(name = "idx_pdpia_file_dpia", columnList = "dpia_id"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PrivacyDpiaFile extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dpia_id", nullable = false)
    private PrivacyDpia dpia;

    /** 문서 구분 — REPORT(결과보고서) / IMPROVEMENT(개선이행) / CHECKLIST(체크리스트) / OTHER(기타).
     *  MySQL ENUM 이 아닌 VARCHAR 로 두어 구분 추가 시 마이그레이션이 필요 없도록 한다. */
    @Column(length = 50)
    @Builder.Default
    private String category = "REPORT";

    /** 문서 제목 (미입력 시 파일명 사용) */
    @Column(length = 500)
    private String title;

    @Column(name = "file_name", length = 500)
    private String fileName;

    @Column(name = "file_path", length = 1000)
    private String filePath;

    @Column(name = "file_size")
    private Long fileSize;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uploader_id")
    private User uploader;
}
