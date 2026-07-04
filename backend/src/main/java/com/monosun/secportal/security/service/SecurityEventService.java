package com.monosun.secportal.security.service;

import com.monosun.secportal.security.dto.SecurityDto;
import com.monosun.secportal.security.entity.SecurityEvent;
import com.monosun.secportal.security.entity.SecurityIntegration;
import com.monosun.secportal.security.repository.SecurityEventRepository;
import com.monosun.secportal.security.repository.SecurityIntegrationRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SecurityEventService {

    private final SecurityIntegrationRepository integrationRepo;
    private final SecurityEventRepository eventRepo;

    public List<SecurityDto.IntegrationResponse> listIntegrations() {
        return integrationRepo.findAll().stream()
                .map(i -> SecurityDto.IntegrationResponse.from(i, eventRepo.countByIntegrationId(i.getId())))
                .toList();
    }

    public SecurityDto.IntegrationResponse getIntegration(Long id) {
        SecurityIntegration i = integrationRepo.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Integration not found"));
        return SecurityDto.IntegrationResponse.from(i, eventRepo.countByIntegrationId(id));
    }

    @Transactional
    public SecurityDto.IntegrationResponse createIntegration(SecurityDto.IntegrationCreateRequest req) {
        SecurityIntegration integration = SecurityIntegration.builder()
                .name(req.getName())
                .solutionType(req.getSolutionType())
                .vendor(req.getVendor())
                .host(req.getHost())
                .apiKey(req.getApiKey())
                .description(req.getDescription())
                .status(SecurityIntegration.ConnectionStatus.DISCONNECTED)
                .build();
        integration = integrationRepo.save(integration);
        return SecurityDto.IntegrationResponse.from(integration, 0);
    }

    @Transactional
    public SecurityDto.IntegrationResponse updateIntegration(Long id, SecurityDto.IntegrationUpdateRequest req) {
        SecurityIntegration integration = integrationRepo.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Integration not found"));
        if (req.getName() != null) integration.setName(req.getName());
        if (req.getSolutionType() != null) integration.setSolutionType(req.getSolutionType());
        if (req.getVendor() != null) integration.setVendor(req.getVendor());
        if (req.getHost() != null) integration.setHost(req.getHost());
        if (req.getApiKey() != null) integration.setApiKey(req.getApiKey());
        if (req.getDescription() != null) integration.setDescription(req.getDescription());
        if (req.getStatus() != null) integration.setStatus(req.getStatus());
        return SecurityDto.IntegrationResponse.from(integrationRepo.save(integration),
                eventRepo.countByIntegrationId(id));
    }

    @Transactional
    public void deleteIntegration(Long id) {
        if (!integrationRepo.existsById(id)) throw new EntityNotFoundException("Integration not found");
        integrationRepo.deleteById(id);
    }

    public Page<SecurityDto.EventResponse> listEvents(Long integrationId, Pageable pageable) {
        if (!integrationRepo.existsById(integrationId)) throw new EntityNotFoundException("Integration not found");
        return eventRepo.findByIntegrationIdOrderByDetectedAtDesc(integrationId, pageable)
                .map(SecurityDto.EventResponse::from);
    }

    @Transactional
    public SecurityDto.EventResponse createEvent(Long integrationId, SecurityDto.EventCreateRequest req) {
        SecurityIntegration integration = integrationRepo.findById(integrationId)
                .orElseThrow(() -> new EntityNotFoundException("Integration not found"));
        SecurityEvent event = SecurityEvent.builder()
                .integration(integration)
                .severity(req.getSeverity())
                .eventType(req.getEventType())
                .sourceIp(req.getSourceIp())
                .destinationIp(req.getDestinationIp())
                .message(req.getMessage())
                .detectedAt(req.getDetectedAt() != null ? req.getDetectedAt() : LocalDateTime.now())
                .build();
        integration.setLastSyncAt(LocalDateTime.now());
        if (integration.getStatus() != SecurityIntegration.ConnectionStatus.CONNECTED) {
            integration.setStatus(SecurityIntegration.ConnectionStatus.CONNECTED);
        }
        return SecurityDto.EventResponse.from(eventRepo.save(event));
    }

    @Transactional
    public void deleteEvent(Long eventId) {
        if (!eventRepo.existsById(eventId)) throw new EntityNotFoundException("Event not found");
        eventRepo.deleteById(eventId);
    }
}
