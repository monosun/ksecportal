package com.monosun.secportal.phishing.service;

import com.monosun.secportal.audit.service.AuditLogService;
import com.monosun.secportal.auth.entity.User;
import com.monosun.secportal.common.exception.BusinessException;
import com.monosun.secportal.common.exception.ResourceNotFoundException;
import com.monosun.secportal.common.excel.ExcelWriter;
import com.monosun.secportal.common.excel.ExportSupport;
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
import org.apache.poi.ss.usermodel.Sheet;
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
            String reportUrl = trackBase + "/report";
            String body = tmpl.getBodyHtml()
                    .replace("{CLICK_URL}", trackBase + "/click")
                    .replace("{OPEN_URL}", trackBase + "/open")
                    .replace("{REPORT_URL}", reportUrl)
                    .replace("{TARGET_NAME}", ct.getTarget().getName())
                    .replace("{TARGET_EMAIL}", ct.getTarget().getEmail());
            body = appendReportFooter(body, reportUrl);
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

    /**
     * 대상자가 훈련 메일의 "신고하기" 링크를 눌렀을 때 신고 시각을 기록한다.
     * 신고 전에 메일을 열어봤다는 뜻이므로 열람 시각도 함께 채운다(클릭은 채우지 않는다 — 신고는 클릭 실패가 아니다).
     */
    @Transactional
    public void trackReport(String token) {
        campaignTargetRepo.findByTrackingToken(token).ifPresent(ct -> {
            if (ct.getOpenedAt() == null) ct.setOpenedAt(LocalDateTime.now());
            if (ct.getReportedAt() == null) ct.setReportedAt(LocalDateTime.now());
            campaignTargetRepo.save(ct);
        });
    }

    // ── Excel export ──────────────────────────────────────────────────────

    /** 모의훈련(캠페인) 1건의 개요와 대상자별 반응 결과를 엑셀로 만든다. */
    @Transactional(readOnly = true)
    public byte[] exportCampaignExcel(Long id) {
        PhishingCampaign c = campaignRepo.findByIdWithTargets(id)
                .orElseThrow(() -> new ResourceNotFoundException("PhishingCampaign", id));
        List<PhishingCampaignTarget> targets = c.getCampaignTargets();

        long sent = targets.stream().filter(t -> t.getSentAt() != null).count();
        long opened = targets.stream().filter(t -> t.getOpenedAt() != null).count();
        long clicked = targets.stream().filter(t -> t.getClickedAt() != null).count();
        long reported = targets.stream().filter(t -> t.getReportedAt() != null).count();
        long failed = targets.stream()
                .filter(t -> t.getSendStatus() == PhishingCampaignTarget.SendStatus.FAILED).count();

        try (ExcelWriter xw = new ExcelWriter()) {
            Sheet sheet = xw.sheet("모의훈련 결과");
            int r = xw.title(sheet, 0, "모의 악성메일 훈련 결과 — " + c.getName(), 9);
            r++;
            r = xw.meta(sheet, r, new String[][]{
                    {"훈련명", c.getName()},
                    {"템플릿", c.getTemplate() != null ? c.getTemplate().getName() : "-"},
                    {"상태", campaignStatusLabel(c.getStatus())},
                    {"설명", c.getDescription()},
                    {"대상 인원", String.valueOf(targets.size())},
                    {"발송 성공", sent + " (" + pct(sent, targets.size()) + "%)"},
                    {"발송 실패", String.valueOf(failed)},
                    {"열람", opened + " (" + pct(opened, sent) + "%)"},
                    {"클릭", clicked + " (" + pct(clicked, sent) + "%)"},
                    {"신고", reported + " (" + pct(reported, sent) + "%)"},
                    {"생성자", c.getCreatedBy() != null ? c.getCreatedBy().getName() : "-"},
                    {"생성일시", ExportSupport.dt(c.getCreatedAt())},
                    {"내려받은 시각", ExportSupport.now()},
            });
            r++;

            r = xw.header(sheet, r, new String[]{
                    "No", "대상자", "이메일", "부서", "발송 결과", "실패 사유",
                    "발송 시각", "열람 시각", "클릭 시각", "신고 시각"});
            int seq = 1;
            for (PhishingCampaignTarget t : targets) {
                String sendResult = t.getSendStatus() == null ? "미발송"
                        : t.getSendStatus() == PhishingCampaignTarget.SendStatus.SUCCESS ? "성공" : "실패";
                r = xw.row(sheet, r, new Object[]{
                        seq++,
                        t.getTarget() != null ? t.getTarget().getName() : "-",
                        t.getTarget() != null ? t.getTarget().getEmail() : "-",
                        t.getTarget() != null ? t.getTarget().getDepartment() : "-",
                        sendResult,
                        t.getSendError(),
                        ExportSupport.dt(t.getSentAt()),
                        ExportSupport.dt(t.getOpenedAt()),
                        ExportSupport.dt(t.getClickedAt()),
                        ExportSupport.dt(t.getReportedAt()),
                }, 0, 4, 6, 7, 8, 9);
            }
            xw.widths(sheet, 6, 16, 28, 16, 12, 30, 20, 20, 20, 20);

            return xw.toBytes();
        }
    }

    /** 파일명에 쓸 훈련명 */
    @Transactional(readOnly = true)
    public String campaignName(Long id) {
        return campaignRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("PhishingCampaign", id))
                .getName();
    }

    private static String campaignStatusLabel(PhishingCampaign.Status s) {
        if (s == null) return "-";
        return switch (s) {
            case DRAFT -> "대기";
            case RUNNING -> "실시중";
            case COMPLETED -> "완료";
            case CANCELLED -> "취소";
        };
    }

    private static long pct(long n, long d) {
        return d > 0 ? Math.round(n * 100.0 / d) : 0;
    }

    // ── Private helpers ───────────────────────────────────────────────────

    /**
     * 템플릿이 {REPORT_URL} 을 직접 쓰지 않은 경우, 메일 하단에 신고 링크를 자동으로 덧붙인다.
     * 기존 템플릿을 수정하지 않아도 신고율이 집계되도록 하기 위한 처리다.
     */
    private String appendReportFooter(String body, String reportUrl) {
        if (body != null && body.contains(reportUrl)) return body;
        String footer = "<div style=\"margin-top:28px;padding-top:14px;border-top:1px solid #e5e7eb;"
                + "font-family:'Segoe UI',system-ui,sans-serif;font-size:12px;color:#6b7280;\">"
                + "이 메일이 의심스럽다면 "
                + "<a href=\"" + reportUrl + "\" style=\"color:#2563eb;font-weight:600;text-decoration:underline;\">"
                + "악성메일로 신고하기</a> 를 눌러주세요."
                + "</div>";
        return (body != null ? body : "") + footer;
    }

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
