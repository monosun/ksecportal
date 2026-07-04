package com.monosun.secportal.incident.service;

import com.monosun.secportal.audit.service.AuditLogService;
import com.monosun.secportal.auth.entity.User;
import com.monosun.secportal.auth.repository.UserRepository;
import com.monosun.secportal.common.exception.BusinessException;
import com.monosun.secportal.common.exception.ResourceNotFoundException;
import com.monosun.secportal.incident.dto.IncidentDto;
import com.monosun.secportal.incident.entity.Incident;
import com.monosun.secportal.incident.repository.IncidentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class IncidentService {

    private final IncidentRepository incidentRepository;
    private final UserRepository userRepository;
    private final AuditLogService auditLogService;

    @Transactional(readOnly = true)
    public Page<IncidentDto.Response> list(String keyword, Incident.Severity severity,
                                           Incident.Status status, Incident.IncidentType type, Pageable pageable) {
        return incidentRepository.search(keyword, severity, status, type, pageable)
                .map(IncidentDto.Response::from);
    }

    @Transactional(readOnly = true)
    public IncidentDto.Response get(Long id) {
        return IncidentDto.Response.from(findById(id));
    }

    @Transactional
    public IncidentDto.Response create(IncidentDto.CreateRequest request, Long reporterId) {
        User reporter = userRepository.findById(reporterId)
                .orElseThrow(() -> new BusinessException("User not found"));
        User assignee = request.getAssigneeId() != null
                ? userRepository.findById(request.getAssigneeId()).orElse(null) : null;

        Incident incident = Incident.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .severity(request.getSeverity())
                .type(request.getType())
                .affectedSystems(request.getAffectedSystems())
                .reporter(reporter)
                .assignee(assignee)
                .assigneeText(assignee == null ? request.getAssigneeText() : null)
                .detectedAt(request.getDetectedAt() != null ? request.getDetectedAt() : LocalDateTime.now())
                .build();

        Incident saved = incidentRepository.save(incident);
        auditLogService.log("INCIDENT_CREATED", "INCIDENT", saved.getId(),
                saved.getSeverity() + " / " + saved.getType() + " / " + saved.getTitle());
        return IncidentDto.Response.from(saved);
    }

    @Transactional
    public IncidentDto.Response update(Long id, IncidentDto.UpdateRequest request) {
        Incident incident = findById(id);
        if (request.getTitle() != null) incident.setTitle(request.getTitle());
        if (request.getDescription() != null) incident.setDescription(request.getDescription());
        if (request.getSeverity() != null) incident.setSeverity(request.getSeverity());
        if (request.getType() != null) incident.setType(request.getType());
        if (request.getAffectedSystems() != null) incident.setAffectedSystems(request.getAffectedSystems());
        if (request.getDetectedAt() != null) incident.setDetectedAt(request.getDetectedAt());
        if (request.getResolvedAt() != null) incident.setResolvedAt(request.getResolvedAt());
        if (request.getAssigneeId() != null) {
            incident.setAssignee(userRepository.findById(request.getAssigneeId()).orElse(null));
            incident.setAssigneeText(null);
        } else if (request.getAssigneeText() != null) {
            incident.setAssignee(null);
            incident.setAssigneeText(request.getAssigneeText().isBlank() ? null : request.getAssigneeText());
        }
        if (request.getStatus() != null) {
            incident.setStatus(request.getStatus());
            if (request.getStatus() == Incident.Status.RESOLVED || request.getStatus() == Incident.Status.CLOSED) {
                if (incident.getResolvedAt() == null) incident.setResolvedAt(LocalDateTime.now());
            }
            auditLogService.log("INCIDENT_STATUS_CHANGED", "INCIDENT", id,
                    incident.getTitle() + " → " + request.getStatus().name());
        }
        return IncidentDto.Response.from(incident);
    }

    @Transactional
    public void delete(Long id) {
        Incident incident = findById(id);
        auditLogService.log("INCIDENT_DELETED", "INCIDENT", id, incident.getTitle());
        incidentRepository.delete(incident);
    }

    private Incident findById(Long id) {
        return incidentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Incident", id));
    }
}
