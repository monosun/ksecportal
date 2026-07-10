package com.monosun.secportal.privacy.entity;

import com.monosun.secportal.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "contractor_checks",
        indexes = {
                @Index(name = "idx_check_contractor", columnList = "contractor_id"),
                @Index(name = "idx_check_year", columnList = "check_year")
        })
// 한 수탁사에 대해 점검 건별로 이력을 남기므로 (contractor_id, check_year) 유니크 제약을 두지 않는다.
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContractorCheck extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contractor_id", nullable = false)
    private Contractor contractor;

    @Column(name = "check_year", nullable = false)
    private int checkYear;

    @Column(name = "check_date")
    private LocalDate checkDate;

    @Column(length = 100)
    private String inspector;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private Status status = Status.PLANNED;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @OneToMany(mappedBy = "check", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ContractorCheckResult> results = new ArrayList<>();

    public enum Status { PLANNED, IN_PROGRESS, COMPLETED }
}
