package com.monosun.secportal.privacy.service;

import com.monosun.secportal.privacy.dto.PrivacyReportDto;
import com.monosun.secportal.privacy.entity.*;
import com.monosun.secportal.privacy.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 개인정보 현황보고서 집계.
 * 각 도메인 리포지토리를 읽어 보고서 섹션을 구성한다 (읽기 전용).
 */
@Service
@RequiredArgsConstructor
public class PrivacyReportService {

    private static final int EXPIRY_WINDOW_DAYS = 30;

    private final PrivacyProcessingRepository processingRepo;
    private final PrivacyFileRepository fileRepo;
    private final ContractorRepository contractorRepo;
    private final ContractorCheckRepository contractorCheckRepo;
    private final PrivacyProvisionRepository provisionRepo;
    private final PrivacyRetentionRepository retentionRepo;
    private final PrivacyDisposalRepository disposalRepo;
    private final PrivacyRightsRequestRepository rightsRepo;
    private final PrivacyBreachRepository breachRepo;
    private final PrivacyDpiaRepository dpiaRepo;
    private final PrivacySafeguardRepository safeguardRepo;

    @Transactional(readOnly = true)
    public PrivacyReportDto.Summary generate() {
        return PrivacyReportDto.Summary.builder()
                .generatedAt(LocalDate.now())
                .processing(processingSection())
                .files(fileSection())
                .contractors(contractorSection())
                .provisions(provisionSection())
                .retentions(retentionSection())
                .disposals(disposalSection())
                .rights(rightsSection())
                .breaches(breachSection())
                .compliance(complianceSection())
                .build();
    }

    private PrivacyReportDto.Section processingSection() {
        long total = processingRepo.count();
        long active = processingRepo.countByStatus(PrivacyProcessing.Status.ACTIVE);
        return PrivacyReportDto.Section.builder()
                .total(total).active(active).inactive(total - active)
                .build();
    }

    private PrivacyReportDto.FileSection fileSection() {
        return PrivacyReportDto.FileSection.builder()
                .total(fileRepo.count())
                .active(fileRepo.countByStatus(PrivacyFile.Status.ACTIVE))
                .sensitive(fileRepo.countBySensitiveInfoTrue())
                .uniqueIdentifier(fileRepo.countByUniqueIdentifierTrue())
                .build();
    }

    private PrivacyReportDto.ContractorSection contractorSection() {
        long total = contractorRepo.count();
        // 점검 이력이 있는 수탁사 id 집합 — countGroupByContractor()는 [contractorId, count]
        Set<Long> checkedIds = contractorCheckRepo.countGroupByContractor().stream()
                .map(row -> (Long) row[0])
                .collect(Collectors.toSet());
        long checked = checkedIds.size();
        return PrivacyReportDto.ContractorSection.builder()
                .total(total).checked(checked).unchecked(total - checked)
                .build();
    }

    private PrivacyReportDto.ProvisionSection provisionSection() {
        Map<PrivacyProvision.ProvisionType, Long> byType = provisionRepo.countGroupByType().stream()
                .collect(Collectors.toMap(
                        row -> (PrivacyProvision.ProvisionType) row[0],
                        row -> (Long) row[1]));
        return PrivacyReportDto.ProvisionSection.builder()
                .total(provisionRepo.count())
                .thirdParty(byType.getOrDefault(PrivacyProvision.ProvisionType.THIRD_PARTY, 0L))
                .jointUse(byType.getOrDefault(PrivacyProvision.ProvisionType.JOINT_USE, 0L))
                .overseas(byType.getOrDefault(PrivacyProvision.ProvisionType.OVERSEAS, 0L))
                .build();
    }

    private PrivacyReportDto.RetentionSection retentionSection() {
        LocalDate today = LocalDate.now();
        List<PrivacyRetention> all = retentionRepo.findAll();
        long expiring = all.stream()
                .filter(r -> r.getStatus() != PrivacyRetention.Status.DISPOSED)
                .filter(r -> r.getExpiryDate() != null)
                .filter(r -> !r.getExpiryDate().isBefore(today)
                        && !r.getExpiryDate().isAfter(today.plusDays(EXPIRY_WINDOW_DAYS)))
                .count();
        long overdue = all.stream()
                .filter(r -> r.getStatus() != PrivacyRetention.Status.DISPOSED)
                .filter(r -> r.getExpiryDate() != null && r.getExpiryDate().isBefore(today))
                .count();
        return PrivacyReportDto.RetentionSection.builder()
                .total(all.size())
                .expiringIn30Days(expiring)
                .overdue(overdue)
                .disposed(retentionRepo.countByStatus(PrivacyRetention.Status.DISPOSED))
                .build();
    }

    private PrivacyReportDto.DisposalSection disposalSection() {
        return PrivacyReportDto.DisposalSection.builder()
                .total(disposalRepo.count())
                .planned(disposalRepo.countByStatus(PrivacyDisposal.Status.PLANNED))
                .completed(disposalRepo.countByStatus(PrivacyDisposal.Status.COMPLETED))
                .pendingApproval(disposalRepo.countByApprovalStatus(PrivacyDisposal.ApprovalStatus.PENDING))
                .build();
    }

    private PrivacyReportDto.RightsSection rightsSection() {
        LocalDate today = LocalDate.now();
        List<PrivacyRightsRequest> all = rightsRepo.findAll();

        long breached = all.stream().filter(r -> {
            if (r.getDueDate() == null) return false;
            boolean closed = r.getStatus() == PrivacyRightsRequest.Status.COMPLETED
                    || r.getStatus() == PrivacyRightsRequest.Status.REJECTED;
            return closed
                    ? r.getCompletedDate() != null && r.getCompletedDate().isAfter(r.getDueDate())
                    : r.getDueDate().isBefore(today);
        }).count();

        Map<String, Long> byType = new LinkedHashMap<>();
        for (PrivacyRightsRequest.RequestType t : PrivacyRightsRequest.RequestType.values()) {
            byType.put(t.name(), 0L);
        }
        rightsRepo.countGroupByType().forEach(row ->
                byType.put(((PrivacyRightsRequest.RequestType) row[0]).name(), (Long) row[1]));

        return PrivacyReportDto.RightsSection.builder()
                .total(all.size())
                .inProgress(rightsRepo.countByStatus(PrivacyRightsRequest.Status.RECEIVED)
                        + rightsRepo.countByStatus(PrivacyRightsRequest.Status.IN_PROGRESS))
                .completed(rightsRepo.countByStatus(PrivacyRightsRequest.Status.COMPLETED))
                .slaBreached(breached)
                .byType(byType)
                .build();
    }

    private PrivacyReportDto.BreachSection breachSection() {
        LocalDateTime now = LocalDateTime.now();
        List<PrivacyBreach> all = breachRepo.findAll();
        long open = all.stream()
                .filter(b -> b.getStatus() != PrivacyBreach.Status.CLOSED)
                .count();
        long reportOverdue = all.stream()
                .filter(b -> !Boolean.TRUE.equals(b.getAuthorityReported()))
                .filter(b -> b.getReportDueAt() != null && b.getReportDueAt().isBefore(now))
                .count();
        long affected = all.stream()
                .map(PrivacyBreach::getAffectedCount)
                .filter(java.util.Objects::nonNull)
                .mapToLong(Integer::longValue)
                .sum();
        return PrivacyReportDto.BreachSection.builder()
                .total(all.size())
                .open(open)
                .reportOverdue(reportOverdue)
                .affectedSubjects(affected)
                .build();
    }

    private PrivacyReportDto.ComplianceSection complianceSection() {
        List<PrivacyReportDto.TypeCount> safeguardByType = safeguardRepo.countGroupByType().stream()
                .map(row -> PrivacyReportDto.TypeCount.builder()
                        .type(((PrivacySafeguard.SafeguardType) row[0]).name())
                        .count((Long) row[1])
                        .build())
                .toList();
        return PrivacyReportDto.ComplianceSection.builder()
                .dpiaTotal(dpiaRepo.count())
                .dpiaCompleted(dpiaRepo.countByStatus(PrivacyDpia.Status.COMPLETED))
                .dpiaHighRisk(dpiaRepo.countByRiskLevel(PrivacyDpia.RiskLevel.HIGH))
                .safeguardTotal(safeguardRepo.count())
                .safeguardCompleted(safeguardRepo.countByStatus(PrivacySafeguard.Status.COMPLETED))
                .safeguardByType(safeguardByType)
                .build();
    }
}
