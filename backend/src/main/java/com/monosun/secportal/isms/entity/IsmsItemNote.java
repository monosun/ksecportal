package com.monosun.secportal.isms.entity;

import com.monosun.secportal.auth.entity.User;
import com.monosun.secportal.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

/** ISMS-P 인증기준 항목의 연도별 의견·현재 상태 기록.
 *  증적과 마찬가지로 연도 단위로 관리하며, 항목·연도 조합당 1건이다. */
@Entity
@Table(name = "isms_item_notes",
        uniqueConstraints = @UniqueConstraint(name = "uk_isms_note_item_year", columnNames = {"item_id", "year"}),
        indexes = @Index(name = "idx_isms_note_item", columnList = "item_id"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IsmsItemNote extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_id", nullable = false)
    private IsmsItem item;

    @Column(name = "year", nullable = false)
    private int year;

    /** 현재 상태 설명 — 해당 연도 기준으로 이 항목이 어떻게 운영되고 있는지 */
    @Column(name = "status_note", columnDefinition = "TEXT")
    private String statusNote;

    /** 의견 — 담당자·심사 대응 의견, 보완 필요사항 등 */
    @Column(columnDefinition = "TEXT")
    private String opinion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "updater_id")
    private User updater;
}
