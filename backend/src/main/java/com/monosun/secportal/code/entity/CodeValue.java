package com.monosun.secportal.code.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "code_values")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CodeValue {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "group_code", length = 50, nullable = false)
    private String groupCode;

    @Column(name = "value", length = 100, nullable = false)
    private String value;

    @Column(name = "label", length = 100, nullable = false)
    private String label;

    @Column(name = "description", length = 500)
    private String description;

    /** 마스킹 방식 (개인정보 항목에만 사용): 부분 마스킹 / 전체 마스킹 / 암호화 저장 등 */
    @Column(name = "masking_type", length = 30)
    private String maskingType;

    /** 마스킹 기준 — 어느 자리를 어떻게 가리는지에 대한 규칙 */
    @Column(name = "masking_rule", length = 300)
    private String maskingRule;

    /** 마스킹 적용 예시 */
    @Column(name = "masking_example", length = 100)
    private String maskingExample;

    @Column(name = "sort_order", nullable = false)
    @Builder.Default
    private int sortOrder = 0;

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;
}
