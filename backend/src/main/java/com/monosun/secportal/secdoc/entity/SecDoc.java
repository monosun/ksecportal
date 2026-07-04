package com.monosun.secportal.secdoc.entity;

import com.monosun.secportal.auth.entity.User;
import com.monosun.secportal.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "sec_docs",
        indexes = {
                @Index(name = "idx_sec_docs_doc_key", columnList = "document_key"),
                @Index(name = "idx_sec_docs_category", columnList = "category")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SecDoc extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "document_key", nullable = false, length = 36)
    private String documentKey;

    @Column(nullable = false, length = 500)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private Category category;

    @Column(nullable = false, length = 50)
    @Builder.Default
    private String version = "1.0";

    @Column(name = "is_latest", nullable = false)
    @Builder.Default
    private boolean latest = true;

    /** 제작기관 — 코드관리(SEC_DOC_ORG) 값 선택 또는 직접 입력 */
    @Column(name = "producing_org", length = 200)
    private String producingOrg;

    @Column(name = "file_name", length = 500)
    private String fileName;

    @Column(name = "file_path", length = 1000)
    private String filePath;

    @Column(name = "file_size")
    private Long fileSize;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uploader_id")
    private User uploader;

    public enum Category {
        GUIDE, POLICY, PROCEDURE, STANDARD, CHECKLIST, TEMPLATE, REPORT, OTHER
    }
}
