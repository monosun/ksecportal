package com.monosun.secportal.metrics.service;

import com.monosun.secportal.asset.entity.Asset;
import com.monosun.secportal.asset.repository.AssetRepository;
import com.monosun.secportal.auth.repository.UserRepository;
import com.monosun.secportal.incident.entity.Incident;
import com.monosun.secportal.incident.repository.IncidentRepository;
import com.monosun.secportal.metrics.dto.MetricsDto;
import com.monosun.secportal.policy.entity.Policy;
import com.monosun.secportal.policy.repository.PolicyAcknowledgmentRepository;
import com.monosun.secportal.security.entity.SecurityIntegration;
import com.monosun.secportal.security.repository.SecurityEventRepository;
import com.monosun.secportal.security.repository.SecurityIntegrationRepository;
import com.monosun.secportal.training.repository.TrainingCompletionRepository;
import com.monosun.secportal.vulnerability.entity.Vulnerability;
import com.monosun.secportal.vulnerability.repository.VulnerabilityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MetricsService {

    private final VulnerabilityRepository vulnRepository;
    private final AssetRepository assetRepository;
    private final UserRepository userRepository;
    private final PolicyAcknowledgmentRepository ackRepository;
    private final TrainingCompletionRepository completionRepository;
    private final IncidentRepository incidentRepository;
    private final SecurityIntegrationRepository integrationRepository;
    private final SecurityEventRepository securityEventRepository;

    private static final DateTimeFormatter MONTH_FMT = DateTimeFormatter.ofPattern("yyyy-MM");

    @Transactional(readOnly = true)
    public MetricsDto.Summary summary() {
        long totalUsers = userRepository.count();
        long totalAssets = assetRepository.countByActive(true);
        long highAssets = assetRepository.countByCriticality(Asset.Criticality.HIGH);
        long overdueVulns = vulnRepository.findOverdue(LocalDate.now()).size();
        long openVulns = vulnRepository.countByStatus(Vulnerability.Status.OPEN);
        long openIncidents = incidentRepository.countByStatus(Incident.Status.OPEN)
                + incidentRepository.countByStatus(Incident.Status.INVESTIGATING);
        long criticalIncidents = incidentRepository.countBySeverity(Incident.Severity.CRITICAL);

        double policyAckRate = 0;
        if (totalUsers > 0) {
            long ackedUsers = ackRepository.countDistinctUsersByPolicyStatus(Policy.Status.PUBLISHED);
            policyAckRate = Math.round((ackedUsers * 100.0 / totalUsers) * 10) / 10.0;
        }

        double trainingRate = 0;
        if (totalUsers > 0) {
            long passedUsers = completionRepository.countDistinctPassedUsers();
            trainingRate = Math.round((passedUsers * 100.0 / totalUsers) * 10) / 10.0;
        }

        LocalDateTime since24h = LocalDateTime.now().minusHours(24);
        long totalIntegrations = integrationRepository.count();
        long connectedIntegrations = integrationRepository.countByStatus(SecurityIntegration.ConnectionStatus.CONNECTED);
        long events24h = securityEventRepository.countByCreatedAtAfter(since24h);
        long criticalEvents24h = securityEventRepository.countBySeverityAndCreatedAtAfter(
                com.monosun.secportal.security.entity.SecurityEvent.EventSeverity.CRITICAL, since24h);
        long highEvents24h = securityEventRepository.countBySeverityAndCreatedAtAfter(
                com.monosun.secportal.security.entity.SecurityEvent.EventSeverity.HIGH, since24h);
        List<MetricsDto.SecurityEventSummary> recentEvents = securityEventRepository.findTop5ByOrderByDetectedAtDesc()
                .stream().map(e -> MetricsDto.SecurityEventSummary.builder()
                        .id(e.getId())
                        .integrationName(e.getIntegration().getName())
                        .severity(e.getSeverity().name())
                        .eventType(e.getEventType())
                        .message(e.getMessage())
                        .detectedAt(e.getDetectedAt() != null ? e.getDetectedAt() : e.getCreatedAt())
                        .build())
                .toList();

        return MetricsDto.Summary.builder()
                .totalAssets(totalAssets)
                .highCriticalityAssets(highAssets)
                .overdueVulns(overdueVulns)
                .openVulns(openVulns)
                .openIncidents(openIncidents)
                .criticalIncidents(criticalIncidents)
                .policyAckRate(policyAckRate)
                .trainingCompletionRate(trainingRate)
                .vulnTrend(buildTrend())
                .totalIntegrations(totalIntegrations)
                .connectedIntegrations(connectedIntegrations)
                .events24h(events24h)
                .criticalEvents24h(criticalEvents24h)
                .highEvents24h(highEvents24h)
                .recentSecurityEvents(recentEvents)
                .build();
    }

    private List<MetricsDto.TrendPoint> buildTrend() {
        LocalDateTime since = LocalDateTime.now().minusMonths(5).withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);
        List<com.monosun.secportal.vulnerability.entity.Vulnerability> recent = vulnRepository.findCreatedAfter(since);

        Map<String, Long> byMonth = recent.stream()
                .collect(Collectors.groupingBy(v -> v.getCreatedAt().format(MONTH_FMT), Collectors.counting()));

        List<MetricsDto.TrendPoint> trend = new ArrayList<>();
        for (int i = 5; i >= 0; i--) {
            String month = LocalDateTime.now().minusMonths(i).format(MONTH_FMT);
            trend.add(MetricsDto.TrendPoint.builder().month(month).count(byMonth.getOrDefault(month, 0L)).build());
        }
        return trend;
    }
}
