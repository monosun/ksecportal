package com.monosun.secportal.privacy.entity;

import com.monosun.secportal.common.crypto.EncryptedStringConverter;
import com.monosun.secportal.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "privacy_contractors",
        indexes = {
                @Index(name = "idx_contractor_status", columnList = "status"),
                @Index(name = "idx_contractor_name", columnList = "name")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Contractor extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String name;

    /** 사업자등록번호 — 저장 시 암호화(AES-256-GCM) */
    @Convert(converter = EncryptedStringConverter.class)
    @Column(name = "business_number", length = 255)
    private String businessNumber;

    @Column(length = 100)
    private String representative;

    @Column(name = "service_type", length = 500)
    private String serviceType;

    @Column(name = "sub_contractor", length = 500)
    private String subContractor;

    @Column(name = "contract_start")
    private LocalDate contractStart;

    @Column(name = "contract_end")
    private LocalDate contractEnd;

    /** 담당자명 — 저장 시 암호화 */
    @Convert(converter = EncryptedStringConverter.class)
    @Column(name = "contact_person", length = 512)
    private String contactPerson;

    /** 담당자 이메일 — 저장 시 암호화 */
    @Convert(converter = EncryptedStringConverter.class)
    @Column(name = "contact_email", length = 512)
    private String contactEmail;

    /** 담당자 연락처 — 저장 시 암호화 */
    @Convert(converter = EncryptedStringConverter.class)
    @Column(name = "contact_phone", length = 512)
    private String contactPhone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private Status status = Status.ACTIVE;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @OneToMany(mappedBy = "contractor", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("inspectionDate DESC")
    @Builder.Default
    private List<ContractorInspection> inspections = new ArrayList<>();

    public enum Status { ACTIVE, INACTIVE }
}
