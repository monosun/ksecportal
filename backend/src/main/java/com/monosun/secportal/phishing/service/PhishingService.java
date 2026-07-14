package com.monosun.secportal.phishing.service;

import com.monosun.secportal.audit.service.AuditLogService;
import com.monosun.secportal.auth.entity.User;
import com.monosun.secportal.common.exception.BusinessException;
import com.monosun.secportal.common.exception.ResourceNotFoundException;
import com.monosun.secportal.notification.service.EmailService;
import com.monosun.secportal.phishing.dto.PhishingDto;
import com.monosun.secportal.phishing.entity.PhishingCampaign;
import com.monosun.secportal.phishing.entity.PhishingCampaignTarget;
import com.monosun.secportal.phishing.entity.PhishingTarget;
import com.monosun.secportal.phishing.entity.PhishingTemplate;
import com.monosun.secportal.phishing.repository.PhishingCampaignRepository;
import com.monosun.secportal.phishing.repository.PhishingCampaignTargetRepository;
import com.monosun.secportal.phishing.repository.PhishingTargetRepository;
import com.monosun.secportal.phishing.repository.PhishingTemplateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PhishingService {

    private final PhishingTemplateRepository templateRepo;
    private final PhishingTargetRepository targetRepo;
    private final PhishingCampaignRepository campaignRepo;
    private final PhishingCampaignTargetRepository campaignTargetRepo;
    private final EmailService emailService;
    private final AuditLogService auditLogService;

    private final com.monosun.secportal.setting.service.AppSettingService appSettingService;

    @Value("${app.base-url:http://localhost:8080/api}")
    private String defaultBaseUrl;

    /** 훈련 메일 추적 링크 기준 URL — 설정관리 app.base_url(도메인 주소) 우선 */
    private String appBaseUrl() {
        return appSettingService.resolveBaseUrl(defaultBaseUrl);
    }

    // ── Templates ─────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<PhishingDto.TemplateResponse> listTemplates() {
        return templateRepo.findAllByOrderByCreatedAtDesc()
                .stream().map(PhishingDto.TemplateResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public PhishingDto.TemplateResponse getTemplate(Long id) {
        return PhishingDto.TemplateResponse.from(findTemplate(id));
    }

    @Transactional
    public PhishingDto.TemplateResponse createTemplate(PhishingDto.TemplateRequest req, User actor) {
        PhishingTemplate t = PhishingTemplate.builder()
                .name(req.getName())
                .category(req.getCategory())
                .difficulty(parseDifficulty(req.getDifficulty()))
                .subject(req.getSubject())
                .senderName(req.getSenderName())
                .senderEmail(req.getSenderEmail())
                .bodyHtml(req.getBodyHtml())
                .description(req.getDescription())
                .createdBy(actor)
                .build();
        PhishingTemplate saved = templateRepo.save(t);
        auditLogService.log("PHISHING_TEMPLATE_CREATED", "PHISHING_TEMPLATE", saved.getId(), "name=" + saved.getName());
        return PhishingDto.TemplateResponse.from(saved);
    }

    @Transactional
    public PhishingDto.TemplateResponse updateTemplate(Long id, PhishingDto.TemplateRequest req) {
        PhishingTemplate t = findTemplate(id);
        t.setName(req.getName());
        t.setCategory(req.getCategory());
        t.setDifficulty(parseDifficulty(req.getDifficulty()));
        t.setSubject(req.getSubject());
        t.setSenderName(req.getSenderName());
        t.setSenderEmail(req.getSenderEmail());
        t.setBodyHtml(req.getBodyHtml());
        t.setDescription(req.getDescription());
        return PhishingDto.TemplateResponse.from(templateRepo.save(t));
    }

    @Transactional
    public void deleteTemplate(Long id) {
        findTemplate(id);
        templateRepo.deleteById(id);
        auditLogService.log("PHISHING_TEMPLATE_DELETED", "PHISHING_TEMPLATE", id, "");
    }

    // ── Targets ───────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<PhishingDto.TargetResponse> listTargets() {
        return targetRepo.findAllByOrderByCreatedAtDesc()
                .stream().map(PhishingDto.TargetResponse::from).toList();
    }

    @Transactional
    public PhishingDto.TargetResponse createTarget(PhishingDto.TargetRequest req) {
        PhishingTarget t = PhishingTarget.builder()
                .name(req.getName())
                .email(req.getEmail())
                .department(req.getDepartment())
                .position(req.getPosition())
                .build();
        return PhishingDto.TargetResponse.from(targetRepo.save(t));
    }

    @Transactional
    public PhishingDto.TargetResponse updateTarget(Long id, PhishingDto.TargetRequest req) {
        PhishingTarget t = findTarget(id);
        t.setName(req.getName());
        t.setEmail(req.getEmail());
        t.setDepartment(req.getDepartment());
        t.setPosition(req.getPosition());
        return PhishingDto.TargetResponse.from(targetRepo.save(t));
    }

    @Transactional
    public void deleteTarget(Long id) {
        findTarget(id);
        targetRepo.deleteById(id);
    }

    @Transactional
    public void toggleTargetActive(Long id) {
        PhishingTarget t = findTarget(id);
        t.setActive(!t.isActive());
        targetRepo.save(t);
    }

    // ── Campaigns ─────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<PhishingDto.CampaignResponse> listCampaigns() {
        return campaignRepo.findAllWithTemplate()
                .stream().map(PhishingDto.CampaignResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public PhishingDto.CampaignDetail getCampaign(Long id) {
        PhishingCampaign c = campaignRepo.findByIdWithTargets(id)
                .orElseThrow(() -> new ResourceNotFoundException("PhishingCampaign", id));
        List<PhishingDto.CampaignTargetResult> results = c.getCampaignTargets()
                .stream().map(PhishingDto.CampaignTargetResult::from).toList();
        return PhishingDto.CampaignDetail.builder()
                .campaign(PhishingDto.CampaignResponse.from(c))
                .results(results)
                .build();
    }

    @Transactional
    public PhishingDto.CampaignResponse createCampaign(PhishingDto.CampaignRequest req, User actor) {
        PhishingTemplate template = findTemplate(req.getTemplateId());
        List<PhishingTarget> targets = targetRepo.findAllById(req.getTargetIds());
        if (targets.isEmpty()) throw new BusinessException("발송대상을 최소 1명 이상 선택해야 합니다.");

        PhishingCampaign campaign = PhishingCampaign.builder()
                .name(req.getName())
                .template(template)
                .description(req.getDescription())
                .scheduledAt(req.getScheduledAt())
                .createdBy(actor)
                .build();

        targets.forEach(target -> {
            PhishingCampaignTarget ct = PhishingCampaignTarget.builder()
                    .campaign(campaign)
                    .target(target)
                    .trackingToken(UUID.randomUUID().toString().replace("-", ""))
                    .build();
            campaign.getCampaignTargets().add(ct);
        });

        PhishingCampaign saved = campaignRepo.save(campaign);
        auditLogService.log("PHISHING_CAMPAIGN_CREATED", "PHISHING_CAMPAIGN", saved.getId(), "name=" + saved.getName());
        return PhishingDto.CampaignResponse.from(saved);
    }

    @Transactional
    public PhishingDto.CampaignResponse launchCampaign(Long id, User actor) {
        PhishingCampaign c = campaignRepo.findByIdWithTargets(id)
                .orElseThrow(() -> new ResourceNotFoundException("PhishingCampaign", id));
        if (c.getStatus() != PhishingCampaign.Status.DRAFT) {
            throw new BusinessException("DRAFT 상태의 훈련만 실시할 수 있습니다.");
        }

        c.setStatus(PhishingCampaign.Status.RUNNING);
        PhishingTemplate tmpl = c.getTemplate();

        int success = 0, failed = 0;
        for (PhishingCampaignTarget ct : c.getCampaignTargets()) {
            String trackBase = appBaseUrl() + "/phishing/track/" + ct.getTrackingToken();
            String body = tmpl.getBodyHtml()
                    .replace("{CLICK_URL}", trackBase + "/click")
                    .replace("{OPEN_URL}", trackBase + "/open")
                    .replace("{TARGET_NAME}", ct.getTarget().getName())
                    .replace("{TARGET_EMAIL}", ct.getTarget().getEmail());
            // 동기 발송하여 처리 결과(성공/실패)를 대상별로 기록한다.
            try {
                emailService.sendSync(ct.getTarget().getEmail(), tmpl.getSubject(), body);
                ct.setSentAt(LocalDateTime.now());
                ct.setSendStatus(PhishingCampaignTarget.SendStatus.SUCCESS);
                ct.setSendError(null);
                success++;
            } catch (Exception e) {
                ct.setSendStatus(PhishingCampaignTarget.SendStatus.FAILED);
                ct.setSendError(e.getMessage() != null ? e.getMessage() : e.toString());
                failed++;
            }
        }

        auditLogService.log("PHISHING_CAMPAIGN_LAUNCHED", "PHISHING_CAMPAIGN", id,
                "actor=" + actor.getName() + ", 성공=" + success + ", 실패=" + failed);
        return PhishingDto.CampaignResponse.from(campaignRepo.save(c));
    }

    @Transactional
    public PhishingDto.CampaignResponse completeCampaign(Long id) {
        PhishingCampaign c = campaignRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("PhishingCampaign", id));
        if (c.getStatus() != PhishingCampaign.Status.RUNNING) {
            throw new BusinessException("RUNNING 상태의 훈련만 완료 처리할 수 있습니다.");
        }
        c.setStatus(PhishingCampaign.Status.COMPLETED);
        auditLogService.log("PHISHING_CAMPAIGN_COMPLETED", "PHISHING_CAMPAIGN", id, "");
        return PhishingDto.CampaignResponse.from(campaignRepo.save(c));
    }

    @Transactional
    public void cancelCampaign(Long id) {
        PhishingCampaign c = campaignRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("PhishingCampaign", id));
        c.setStatus(PhishingCampaign.Status.CANCELLED);
        campaignRepo.save(c);
    }

    @Transactional
    public void deleteCampaign(Long id) {
        campaignRepo.findById(id).orElseThrow(() -> new ResourceNotFoundException("PhishingCampaign", id));
        campaignRepo.deleteById(id);
        auditLogService.log("PHISHING_CAMPAIGN_DELETED", "PHISHING_CAMPAIGN", id, "");
    }

    // ── Send logs (발송 처리 결과) ─────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<PhishingDto.SendLogEntry> listSendLogs() {
        return campaignTargetRepo.findSendLogs()
                .stream().map(PhishingDto.SendLogEntry::from).toList();
    }

    // ── Tracking (click/open) ─────────────────────────────────────────────

    @Transactional
    public void trackOpen(String token) {
        campaignTargetRepo.findByTrackingToken(token).ifPresent(ct -> {
            if (ct.getOpenedAt() == null) {
                ct.setOpenedAt(LocalDateTime.now());
                campaignTargetRepo.save(ct);
            }
        });
    }

    @Transactional
    public void trackClick(String token) {
        campaignTargetRepo.findByTrackingToken(token).ifPresent(ct -> {
            if (ct.getOpenedAt() == null) ct.setOpenedAt(LocalDateTime.now());
            if (ct.getClickedAt() == null) ct.setClickedAt(LocalDateTime.now());
            campaignTargetRepo.save(ct);
        });
    }

    // ── Private helpers ───────────────────────────────────────────────────

    private PhishingTemplate findTemplate(Long id) {
        return templateRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("PhishingTemplate", id));
    }

    private PhishingTarget findTarget(Long id) {
        return targetRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("PhishingTarget", id));
    }


    private PhishingTemplate.Difficulty parseDifficulty(String d) {
        try { return PhishingTemplate.Difficulty.valueOf(d); }
        catch (Exception e) { return PhishingTemplate.Difficulty.MEDIUM; }
    }
}
