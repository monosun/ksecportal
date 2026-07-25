package com.monosun.secportal.opstatus.entity;

import com.monosun.secportal.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

/**
 * 운영현황관리 기본 항목 마스터 — 코드 관리 화면에서 관리하고, 연도별 운영현황 구성의 원본이 된다.
 *
 * 연도 데이터({@link OperationStatusItem})와 필드 구성이 같지만 연도·이행 실적이 없다.
 * 최초 기동 시 {@code OperationStatusDefaults} 상수(참고 서식 기준)로 시드된 뒤에는
 * 관리자가 코드 관리에서 직접 추가·수정·삭제한다.
 */
@Entity
@Table(name = "operation_status_defaults",
        indexes = @Index(name = "idx_ops_default_type", columnList = "type"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OperationStatusDefault extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OperationStatusItem.Type type;

    /** 구분 (정보보호 관리체계에만 사용) */
    @Column(length = 100)
    private String category;

    /** 점검 기준 / 점검항목 */
    @Column(nullable = false, length = 300)
    private String name;

    /** 주기 및 시점 / 점검주기 */
    @Column(length = 100)
    private String cycle;

    /** 보안적용 실적 / 상세 내용 */
    @Column(columnDefinition = "TEXT")
    private String deliverable;

    @Column(length = 100)
    private String owner;

    @Column(length = 100)
    private String manager;

    @Column(columnDefinition = "TEXT")
    private String note;

    /** 월별 기본 계획 (12비트, bit 0 = 1월) */
    @Column(name = "plan_months", nullable = false)
    @Builder.Default
    private Integer planMonths = 0;

    @Column(name = "sort_order")
    @Builder.Default
    private Integer sortOrder = 0;

    /** 미사용 처리하면 기본 항목 불러오기·선택 목록에서 제외된다 */
    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;
}
