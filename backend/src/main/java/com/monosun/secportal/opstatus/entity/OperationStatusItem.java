package com.monosun.secportal.opstatus.entity;

import com.monosun.secportal.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

/**
 * 운영현황관리 항목 — 연도별로 구성·관리하는 정보보호/개인정보보호 관리체계 운영 점검 항목.
 *
 * "정보보호 관리체계 운영현황표" 서식을 참고했으며, 시트 두 종류를 {@link Type} 으로 구분한다.
 * 월별 계획·이행은 12비트 마스크(bit 0 = 1월)로 저장한다 — 컬럼 24개를 두지 않기 위한 선택이며,
 * DTO 에서 boolean[12] 로 변환해 화면에 전달한다.
 */
@Entity
@Table(name = "operation_status_items",
        indexes = {
                @Index(name = "idx_ops_year_type", columnList = "year,type"),
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OperationStatusItem extends BaseEntity {

    public enum Type {
        /** 정보보호 관리체계 운영현황 */
        ISMS,
        /** 개인정보보호 관리체계 연간 운영 */
        PRIVACY
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "year", nullable = false)
    private Integer year;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Type type;

    /** 구분 (정보보호 정책관리·자산관리·접근통제 등). 개인정보 시트는 구분이 없어 비어 있을 수 있다. */
    @Column(length = 100)
    private String category;

    /** 점검 기준 / 점검항목 */
    @Column(nullable = false, length = 300)
    private String name;

    /** 주기 및 시점 (연1회·반기1회·매월·수시·상시 등) */
    @Column(length = 100)
    private String cycle;

    /** 보안적용 실적 / 상세 내용 — 산출물·근거 목록 */
    @Column(columnDefinition = "TEXT")
    private String deliverable;

    /** 책임자 */
    @Column(length = 100)
    private String owner;

    /** 실무자 */
    @Column(length = 100)
    private String manager;

    /** 비고 */
    @Column(columnDefinition = "TEXT")
    private String note;

    /** 월별 계획 (12비트, bit 0 = 1월) */
    @Column(name = "plan_months", nullable = false)
    @Builder.Default
    private Integer planMonths = 0;

    /** 월별 이행 완료 (12비트, bit 0 = 1월) */
    @Column(name = "done_months", nullable = false)
    @Builder.Default
    private Integer doneMonths = 0;

    @Column(name = "sort_order")
    @Builder.Default
    private Integer sortOrder = 0;

    // ── 비트마스크 헬퍼 (month 는 1~12) ─────────────────────────────────────

    public static boolean has(Integer mask, int month) {
        return mask != null && month >= 1 && month <= 12 && (mask & (1 << (month - 1))) != 0;
    }

    public static Integer set(Integer mask, int month, boolean on) {
        int m = mask == null ? 0 : mask;
        if (month < 1 || month > 12) return m;
        int bit = 1 << (month - 1);
        return on ? (m | bit) : (m & ~bit);
    }

    public static int count(Integer mask) {
        return mask == null ? 0 : Integer.bitCount(mask & 0xFFF);
    }
}
