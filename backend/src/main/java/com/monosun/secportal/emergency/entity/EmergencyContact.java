package com.monosun.secportal.emergency.entity;

import com.monosun.secportal.common.crypto.EncryptedStringConverter;
import com.monosun.secportal.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

/**
 * 비상연락망 구성원.
 *
 * 개인 휴대전화·이메일은 직접 식별 가능한 개인정보이므로 저장 시 암호화한다(AES-256-GCM).
 * 사무실 번호는 기관·부서 대표번호로 쓰이는 경우가 많아 평문으로 둔다.
 */
@Entity
@Table(name = "emergency_contacts")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class EmergencyContact extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id", nullable = false)
    private EmergencyContactGroup group;

    /** 담당자명 또는 기관·부서명 */
    @Column(nullable = false)
    private String name;

    /** 소속 기관·회사 (외부기관·협력사인 경우) */
    private String organization;

    private String department;

    private String position;

    /** 비상 시 맡는 역할 — 총괄, 상황 전파, 기술 대응, 대외 신고 등 */
    private String roleName;

    /** 연락 순서 — 1차, 2차… 낮을수록 먼저 연락한다 */
    @Column(nullable = false)
    @Builder.Default
    private Integer contactOrder = 1;

    /** 휴대전화 — 저장 시 암호화 */
    @Convert(converter = EncryptedStringConverter.class)
    @Column(length = 512)
    private String mobile;

    /** 사무실·대표 전화 */
    @Column(length = 100)
    private String officePhone;

    /** 이메일 — 저장 시 암호화 */
    @Convert(converter = EncryptedStringConverter.class)
    @Column(length = 512)
    private String email;

    /** 24시간 연락 가능 여부 */
    @Column(nullable = false)
    @Builder.Default
    private boolean available24h = false;

    @Column(columnDefinition = "TEXT")
    private String note;

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;
}
