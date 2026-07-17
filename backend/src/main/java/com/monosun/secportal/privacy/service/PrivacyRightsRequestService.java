package com.monosun.secportal.privacy.service;

import com.monosun.secportal.audit.service.AuditLogService;
import com.monosun.secportal.common.exception.ResourceNotFoundException;
import com.monosun.secportal.privacy.dto.PrivacyRightsRequestDto;
import com.monosun.secportal.privacy.entity.PrivacyRightsRequest;
import com.monosun.secportal.privacy.repository.PrivacyRightsRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PrivacyRightsRequestService {

    /** 정보주체 권리행사 법정 처리기한 — 요청일로부터 10일 (개인정보 보호법 시행령 제41조 등) */
    private static final int SLA_DAYS = 10;

    private final PrivacyRightsRequestRepository repo;
    private final AuditLogService auditLogService;

    @Transactional(readOnly = true)
    public List<PrivacyRightsRequestDto.Response> listAll() {
        return repo.findAllByOrderByRequestDateDesc().stream()
                .map(PrivacyRightsRequestDto.Response::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public PrivacyRightsRequestDto.Response get(Long id) {
        return PrivacyRightsRequestDto.Response.from(find(id));
    }

    @Transactional
    public PrivacyRightsRequestDto.Response create(PrivacyRightsRequestDto.Request req) {
        PrivacyRightsRequest r = PrivacyRightsRequest.builder()
                .requestType(parseType(req.getRequestType()))
                .subjectName(req.getSubjectName().trim())
                .contact(req.getContact())
                .channel(req.getChannel())
                .content(req.getContent())
                .requestDate(req.getRequestDate())
                .dueDate(req.getDueDate() != null ? req.getDueDate() : req.getRequestDate().plusDays(SLA_DAYS))
                .completedDate(req.getCompletedDate())
                .handler(req.getHandler())
                .result(req.getResult())
                .rejectReason(req.getRejectReason())
                .status(parseStatus(req.getStatus()))
                .notes(req.getNotes())
                .build();
        PrivacyRightsRequest saved = repo.save(r);
        auditLogService.log("CREATE", "PrivacyRightsRequest", saved.getId(),
                saved.getRequestType().name() + " / " + saved.getSubjectName());
        return PrivacyRightsRequestDto.Response.from(saved);
    }

    @Transactional
    public PrivacyRightsRequestDto.Response update(Long id, PrivacyRightsRequestDto.Request req) {
        PrivacyRightsRequest r = find(id);
        if (req.getRequestType() != null) r.setRequestType(parseType(req.getRequestType()));
        if (req.getSubjectName() != null && !req.getSubjectName().isBlank()) r.setSubjectName(req.getSubjectName().trim());
        if (req.getContact() != null) r.setContact(req.getContact());
        if (req.getChannel() != null) r.setChannel(req.getChannel());
        if (req.getContent() != null) r.setContent(req.getContent());
        if (req.getRequestDate() != null) {
            r.setRequestDate(req.getRequestDate());
            // 요청일이 바뀌면 처리기한을 다시 산정한다 (명시적으로 준 값이 없을 때만)
            if (req.getDueDate() == null) r.setDueDate(req.getRequestDate().plusDays(SLA_DAYS));
        }
        if (req.getDueDate() != null) r.setDueDate(req.getDueDate());
        if (req.getCompletedDate() != null) r.setCompletedDate(req.getCompletedDate());
        if (req.getHandler() != null) r.setHandler(req.getHandler());
        if (req.getResult() != null) r.setResult(req.getResult());
        if (req.getRejectReason() != null) r.setRejectReason(req.getRejectReason());
        if (req.getStatus() != null) {
            PrivacyRightsRequest.Status next = parseStatus(req.getStatus());
            r.setStatus(next);
            // 완료로 전이하는데 완료일이 없으면 오늘로 채운다
            boolean closing = next == PrivacyRightsRequest.Status.COMPLETED
                    || next == PrivacyRightsRequest.Status.REJECTED;
            if (closing && r.getCompletedDate() == null && req.getCompletedDate() == null) {
                r.setCompletedDate(LocalDate.now());
            }
        }
        if (req.getNotes() != null) r.setNotes(req.getNotes());
        auditLogService.log("UPDATE", "PrivacyRightsRequest", r.getId(),
                r.getRequestType().name() + " / " + r.getSubjectName());
        return PrivacyRightsRequestDto.Response.from(r);
    }

    @Transactional
    public void delete(Long id) {
        PrivacyRightsRequest r = find(id);
        repo.delete(r);
        auditLogService.log("DELETE", "PrivacyRightsRequest", id,
                r.getRequestType().name() + " / " + r.getSubjectName());
    }

    private PrivacyRightsRequest find(Long id) {
        return repo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("PrivacyRightsRequest", id));
    }

    private PrivacyRightsRequest.RequestType parseType(String s) {
        if (s == null || s.isBlank()) return PrivacyRightsRequest.RequestType.ACCESS;
        try {
            return PrivacyRightsRequest.RequestType.valueOf(s);
        } catch (IllegalArgumentException e) {
            return PrivacyRightsRequest.RequestType.ACCESS;
        }
    }

    private PrivacyRightsRequest.Status parseStatus(String s) {
        if (s == null || s.isBlank()) return PrivacyRightsRequest.Status.RECEIVED;
        try {
            return PrivacyRightsRequest.Status.valueOf(s);
        } catch (IllegalArgumentException e) {
            return PrivacyRightsRequest.Status.RECEIVED;
        }
    }
}
