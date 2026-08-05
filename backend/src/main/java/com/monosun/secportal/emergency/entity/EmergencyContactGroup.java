package com.monosun.secportal.emergency.entity;

import com.monosun.secportal.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

/**
 * 비상연락망 그룹 — 상황별·조직별 연락 계통.
 *
 * 침해사고 대응반, 개인정보 유출 대응반, 외부 신고기관 등 연락 목적 단위로 묶고,
 * 그룹 안에서 {@link EmergencyContact} 의 연락 순서(1차·2차…)에 따라 연락한다.
 */
@Entity
@Table(name = "emergency_contact_groups")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class EmergencyContactGroup extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private ContactType contactType = ContactType.INTERNAL;

    @Column(columnDefinition = "TEXT")
    private String description;

    /** 화면 표시 순서 */
    @Column(nullable = false)
    @Builder.Default
    private Integer sortOrder = 0;

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;

    @OneToMany(mappedBy = "group", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("contactOrder ASC, id ASC")
    @Builder.Default
    private List<EmergencyContact> contacts = new ArrayList<>();

    /** 내부 조직 / 외부 신고·문의 기관 / 협력사·유지보수 업체 */
    public enum ContactType { INTERNAL, EXTERNAL, PARTNER }
}
