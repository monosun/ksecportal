package com.monosun.secportal.report.service;

import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.monosun.secportal.asset.entity.Asset;
import com.monosun.secportal.asset.repository.AssetRepository;
import com.monosun.secportal.auth.entity.User;
import com.monosun.secportal.auth.repository.UserRepository;
import com.monosun.secportal.incident.entity.Incident;
import com.monosun.secportal.incident.repository.IncidentRepository;
import com.monosun.secportal.isms.entity.IsmsEvidence;
import com.monosun.secportal.isms.entity.IsmsItem;
import com.monosun.secportal.isms.repository.IsmsEvidenceRepository;
import com.monosun.secportal.isms.repository.IsmsItemRepository;
import com.monosun.secportal.policy.entity.Policy;
import com.monosun.secportal.policy.repository.PolicyRepository;
import com.monosun.secportal.common.exception.ResourceNotFoundException;
import com.monosun.secportal.privacy.dto.PrivacyReportDto;
import com.monosun.secportal.sourcescan.entity.SourceScan;
import com.monosun.secportal.sourcescan.entity.SourceScanFinding;
import com.monosun.secportal.sourcescan.repository.SourceScanRepository;
import com.monosun.secportal.training.entity.TrainingCompletion;
import com.monosun.secportal.training.entity.TrainingCourse;
import com.monosun.secportal.training.repository.TrainingCompletionRepository;
import com.monosun.secportal.training.repository.TrainingCourseRepository;
import com.monosun.secportal.vulnerability.entity.Vulnerability;
import com.monosun.secportal.vulnerability.repository.VulnerabilityRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.awt.*;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReportService {

    private final VulnerabilityRepository vulnerabilityRepository;
    private final AssetRepository assetRepository;
    private final IncidentRepository incidentRepository;
    private final PolicyRepository policyRepository;
    private final TrainingCourseRepository courseRepository;
    private final TrainingCompletionRepository completionRepository;
    private final UserRepository userRepository;
    private final IsmsItemRepository ismsItemRepository;
    private final IsmsEvidenceRepository ismsEvidenceRepository;
    private final com.monosun.secportal.setting.service.AppSettingService appSettingService;
    private final com.monosun.secportal.privacy.service.PrivacyReportService privacyReportService;
    private final SourceScanRepository sourceScanRepository;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private BaseFont koreanBaseFont;

    @PostConstruct
    private void initFonts() {
        try (InputStream is = getClass().getResourceAsStream("/fonts/NanumGothic.ttf")) {
            if (is != null) {
                byte[] fontBytes = is.readAllBytes();
                koreanBaseFont = BaseFont.createFont("NanumGothic.ttf", BaseFont.IDENTITY_H,
                        BaseFont.EMBEDDED, true, fontBytes, null);
                log.info("Korean font (NanumGothic) loaded successfully for PDF generation");
            } else {
                log.warn("Korean font not found in classpath (/fonts/NanumGothic.ttf)");
            }
        } catch (Exception e) {
            log.warn("Failed to load Korean font: {}", e.getMessage());
        }
    }

    private Font kFont(float size, int style) {
        if (koreanBaseFont != null) return new Font(koreanBaseFont, size, style);
        return new Font(Font.HELVETICA, size, style);
    }

    private Font kFont(float size, int style, Color color) {
        if (koreanBaseFont != null) {
            Font f = new Font(koreanBaseFont, size, style);
            f.setColor(color);
            return f;
        }
        return new Font(Font.HELVETICA, size, style, color);
    }

    private String t(String lang, String ko, String en) {
        return "ko".equalsIgnoreCase(lang) ? ko : en;
    }

    private String tVulnSeverity(String lang, Vulnerability.Severity s) {
        if (!"ko".equalsIgnoreCase(lang)) return s.name();
        return switch (s) {
            case CRITICAL -> "심각"; case HIGH -> "높음"; case MEDIUM -> "중간"; case LOW -> "낮음"; case INFO -> "정보";
        };
    }
    private String tVulnStatus(String lang, Vulnerability.Status s) {
        if (!"ko".equalsIgnoreCase(lang)) return s.name();
        return switch (s) {
            case OPEN -> "미처리"; case IN_PROGRESS -> "처리중"; case RESOLVED -> "해결됨";
            case ACCEPTED -> "수용"; case FALSE_POSITIVE -> "오탐";
        };
    }
    private String tPolicyCat(String lang, Policy.Category c) {
        if (!"ko".equalsIgnoreCase(lang)) return c.name();
        return switch (c) {
            case GENERAL -> "일반"; case ACCESS_CONTROL -> "접근제어"; case DATA_PROTECTION -> "데이터보호";
            case INCIDENT_RESPONSE -> "침해대응"; case NETWORK -> "네트워크";
            case PHYSICAL -> "물리보안"; case VENDOR -> "공급업체"; case OTHER -> "기타";
        };
    }
    private String tPolicyStatus(String lang, Policy.Status s) {
        if (!"ko".equalsIgnoreCase(lang)) return s.name();
        return switch (s) {
            case DRAFT -> "초안"; case REVIEW -> "검토중"; case PUBLISHED -> "공표됨"; case ARCHIVED -> "보관됨";
        };
    }
    private String tAssetEnv(String lang, Asset.Environment e) {
        if (!"ko".equalsIgnoreCase(lang)) return e.name();
        return switch (e) {
            case PRODUCTION -> "운영"; case STAGING -> "스테이징"; case DEVELOPMENT -> "개발"; case TEST -> "테스트";
        };
    }
    private String tAssetCrit(String lang, Asset.Criticality c) {
        if (!"ko".equalsIgnoreCase(lang)) return c.name();
        return switch (c) {
            case HIGH -> "높음"; case MEDIUM -> "중간"; case LOW -> "낮음";
        };
    }
    private String tIncidentType(String lang, Incident.IncidentType t) {
        if (!"ko".equalsIgnoreCase(lang)) return t.name();
        return switch (t) {
            case MALWARE -> "악성코드"; case PHISHING -> "피싱"; case DATA_BREACH -> "데이터유출";
            case UNAUTHORIZED_ACCESS -> "무단접근"; case DDOS -> "DDoS";
            case INSIDER_THREAT -> "내부위협"; case PHYSICAL -> "물리적보안"; case OTHER -> "기타";
        };
    }
    private String tIncidentSeverity(String lang, Incident.Severity s) {
        if (!"ko".equalsIgnoreCase(lang)) return s.name();
        return switch (s) {
            case CRITICAL -> "심각"; case HIGH -> "높음"; case MEDIUM -> "중간"; case LOW -> "낮음";
        };
    }
    private String tIncidentStatus(String lang, Incident.Status s) {
        if (!"ko".equalsIgnoreCase(lang)) return s.name();
        return switch (s) {
            case OPEN -> "미처리"; case INVESTIGATING -> "조사중"; case CONTAINED -> "격리됨";
            case RESOLVED -> "해결됨"; case CLOSED -> "종료";
        };
    }
    private String tUserRole(String lang, User.Role r) {
        if (!"ko".equalsIgnoreCase(lang)) return r.name();
        return switch (r) {
            case ADMIN -> "관리자"; case MANAGER -> "매니저"; case USER -> "사용자";
        };
    }
    private String tIsmsStatus(String lang, IsmsEvidence.Status s) {
        if (!"ko".equalsIgnoreCase(lang)) return s.name();
        return switch (s) {
            case COMPLIANT -> "준수"; case PARTIAL -> "부분준수"; case NON_COMPLIANT -> "미준수"; case NA -> "해당없음";
        };
    }

    // ── PDF: Vulnerability Report ─────────────────────────────────────────────

    @Transactional(readOnly = true)
    public byte[] generateVulnerabilityReport(String lang) {
        return buildVulnPdf(vulnerabilityRepository.findAll(), lang);
    }

    private byte[] buildVulnPdf(List<Vulnerability> vulns, String lang) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document doc = new Document(PageSize.A4.rotate());
        try {
            PdfWriter.getInstance(doc, out);
            doc.open();
            Font titleFont  = kFont(18, Font.BOLD);
            Font headerFont = kFont(10, Font.BOLD, Color.WHITE);
            Font bodyFont   = kFont(9,  Font.NORMAL);

            addCenteredTitle(doc, t(lang, "취약점 관리 보고서", "Vulnerability Report"), titleFont);
            addCompanyLine(doc);
            addCenteredSubtitle(doc, t(lang, "생성일: ", "Generated: ") + LocalDate.now().format(DATE_FMT));
            doc.add(new Paragraph(" "));
            addVulnStats(doc, vulns, bodyFont, lang);
            doc.add(new Paragraph(" "));

            PdfPTable table = new PdfPTable(7);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{3, 1.5f, 1.5f, 1.5f, 2, 2, 1.5f});
            for (String h : new String[]{
                    t(lang, "제목", "Title"),
                    t(lang, "심각도", "Severity"),
                    t(lang, "상태", "Status"),
                    "CVE ID",
                    t(lang, "담당자", "Assignee"),
                    t(lang, "자산", "Asset"),
                    t(lang, "조치 기한", "Due Date")}) {
                addHeaderCell(table, h, headerFont);
            }
            for (Vulnerability v : vulns) {
                table.addCell(new Phrase(v.getTitle(), bodyFont));
                table.addCell(new Phrase(tVulnSeverity(lang, v.getSeverity()), bodyFont));
                table.addCell(new Phrase(tVulnStatus(lang, v.getStatus()), bodyFont));
                table.addCell(new Phrase(nvl(v.getCveId()), bodyFont));
                table.addCell(new Phrase(v.getAssignee() != null ? v.getAssignee().getName() : "-", bodyFont));
                table.addCell(new Phrase(nvl(v.getAssetName()), bodyFont));
                table.addCell(new Phrase(v.getDueDate() != null ? v.getDueDate().format(DATE_FMT) : "-", bodyFont));
            }
            doc.add(table);
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate vulnerability PDF", e);
        } finally {
            doc.close();
        }
        return out.toByteArray();
    }

    // ── PDF: Policy Report ───────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public byte[] generatePolicyReport(String lang) {
        List<Policy> policies = policyRepository.findAll(Sort.by("createdAt").descending());
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document doc = new Document(PageSize.A4.rotate());
        try {
            PdfWriter.getInstance(doc, out);
            doc.open();
            Font titleFont  = kFont(18, Font.BOLD);
            Font headerFont = kFont(10, Font.BOLD, Color.WHITE);
            Font bodyFont   = kFont(9,  Font.NORMAL);

            addCenteredTitle(doc, t(lang, "보안 정책 보고서", "Security Policy Report"), titleFont);
            addCompanyLine(doc);
            addCenteredSubtitle(doc, t(lang, "생성일: ", "Generated: ") + LocalDate.now().format(DATE_FMT));
            doc.add(new Paragraph(" "));

            PdfPTable statsTable = new PdfPTable(4);
            statsTable.setWidthPercentage(60);
            String[] statLabels = {
                    t(lang, "전체", "Total"),
                    t(lang, "공표됨", "Published"),
                    t(lang, "검토중", "In Review"),
                    t(lang, "초안", "Draft")
            };
            long[] statVals = {
                    policies.size(),
                    policies.stream().filter(p -> p.getStatus() == Policy.Status.PUBLISHED).count(),
                    policies.stream().filter(p -> p.getStatus() == Policy.Status.REVIEW).count(),
                    policies.stream().filter(p -> p.getStatus() == Policy.Status.DRAFT).count()
            };
            for (int i = 0; i < statLabels.length; i++) {
                PdfPCell cell = new PdfPCell();
                cell.addElement(new Phrase(String.valueOf(statVals[i]), kFont(16, Font.BOLD)));
                cell.addElement(new Phrase(statLabels[i], bodyFont));
                cell.setPadding(8);
                cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                statsTable.addCell(cell);
            }
            doc.add(statsTable);
            doc.add(new Paragraph(" "));

            PdfPTable table = new PdfPTable(5);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{4, 2, 1.5f, 1.5f, 2});
            for (String h : new String[]{
                    t(lang, "제목", "Title"),
                    t(lang, "카테고리", "Category"),
                    t(lang, "상태", "Status"),
                    t(lang, "버전", "Version"),
                    t(lang, "시행일", "Effective Date")}) {
                addHeaderCell(table, h, headerFont);
            }
            for (Policy p : policies) {
                table.addCell(new Phrase(p.getTitle(), bodyFont));
                table.addCell(new Phrase(tPolicyCat(lang, p.getCategory()), bodyFont));
                table.addCell(new Phrase(tPolicyStatus(lang, p.getStatus()), bodyFont));
                table.addCell(new Phrase("v" + p.getVersion(), bodyFont));
                table.addCell(new Phrase(p.getEffectiveDate() != null ? p.getEffectiveDate().format(DATE_FMT) : "-", bodyFont));
            }
            doc.add(table);
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate policy PDF", e);
        } finally {
            doc.close();
        }
        return out.toByteArray();
    }

    // ── PDF: Asset Report ─────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public byte[] generateAssetReport(String lang) {
        List<Asset> assets = assetRepository.findAll(Sort.by("createdAt").descending());
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document doc = new Document(PageSize.A4.rotate());
        try {
            PdfWriter.getInstance(doc, out);
            doc.open();
            Font titleFont  = kFont(18, Font.BOLD);
            Font headerFont = kFont(10, Font.BOLD, Color.WHITE);
            Font bodyFont   = kFont(9,  Font.NORMAL);

            addCenteredTitle(doc, t(lang, "자산 관리 보고서", "Asset Management Report"), titleFont);
            addCompanyLine(doc);
            addCenteredSubtitle(doc, t(lang, "생성일: ", "Generated: ") + LocalDate.now().format(DATE_FMT));
            doc.add(new Paragraph(" "));

            PdfPTable stats = new PdfPTable(3);
            stats.setWidthPercentage(50);
            String[] sl = {t(lang, "전체", "Total"), t(lang, "고중요도", "High Crit."), t(lang, "운영중", "Active")};
            long[] sv = {
                    assets.size(),
                    assets.stream().filter(a -> a.getCriticality() == Asset.Criticality.HIGH).count(),
                    assets.stream().filter(Asset::isActive).count()
            };
            for (int i = 0; i < sl.length; i++) {
                PdfPCell cell = new PdfPCell();
                cell.addElement(new Phrase(String.valueOf(sv[i]), kFont(16, Font.BOLD)));
                cell.addElement(new Phrase(sl[i], bodyFont));
                cell.setPadding(8);
                cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                stats.addCell(cell);
            }
            doc.add(stats);
            doc.add(new Paragraph(" "));

            PdfPTable table = new PdfPTable(7);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{3, 1.5f, 1.5f, 1.5f, 2, 1.5f, 1.5f});
            for (String h : new String[]{
                    t(lang, "자산명", "Name"),
                    t(lang, "유형", "Type"),
                    t(lang, "환경", "Env"),
                    t(lang, "중요도", "Criticality"),
                    t(lang, "IP / 리전", "IP / Region"),
                    t(lang, "담당자", "Owner"),
                    t(lang, "상태", "Status")}) {
                addHeaderCell(table, h, headerFont);
            }
            for (Asset a : assets) {
                table.addCell(new Phrase(a.getName(), bodyFont));
                table.addCell(new Phrase(a.getType() != null ? a.getType() : "-", bodyFont));
                table.addCell(new Phrase(a.getEnvironment() != null ? tAssetEnv(lang, a.getEnvironment()) : "-", bodyFont));
                table.addCell(new Phrase(tAssetCrit(lang, a.getCriticality()), bodyFont));
                table.addCell(new Phrase(a.getIpAddress() != null ? a.getIpAddress() : nvl(a.getRegion()), bodyFont));
                table.addCell(new Phrase(nvl(a.getOwner()), bodyFont));
                table.addCell(new Phrase(a.isActive() ? t(lang, "운영중", "Active") : t(lang, "폐기", "Inactive"), bodyFont));
            }
            doc.add(table);
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate asset PDF", e);
        } finally {
            doc.close();
        }
        return out.toByteArray();
    }

    // ── PDF: Incident Report ──────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public byte[] generateIncidentReport(String lang) {
        List<Incident> incidents = incidentRepository.findAllByOrderByCreatedAtDesc();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document doc = new Document(PageSize.A4.rotate());
        try {
            PdfWriter.getInstance(doc, out);
            doc.open();
            Font titleFont  = kFont(18, Font.BOLD);
            Font headerFont = kFont(10, Font.BOLD, Color.WHITE);
            Font bodyFont   = kFont(9,  Font.NORMAL);

            addCenteredTitle(doc, t(lang, "보안 인시던트 보고서", "Security Incident Report"), titleFont);
            addCompanyLine(doc);
            addCenteredSubtitle(doc, t(lang, "생성일: ", "Generated: ") + LocalDate.now().format(DATE_FMT));
            doc.add(new Paragraph(" "));

            PdfPTable stats = new PdfPTable(4);
            stats.setWidthPercentage(60);
            String[] sl = {t(lang, "전체", "Total"), t(lang, "심각", "Critical"), t(lang, "미처리", "Open"), t(lang, "해결됨", "Resolved")};
            long[] sv = {
                    incidents.size(),
                    incidents.stream().filter(i -> i.getSeverity() == Incident.Severity.CRITICAL).count(),
                    incidents.stream().filter(i -> i.getStatus() == Incident.Status.OPEN).count(),
                    incidents.stream().filter(i -> i.getStatus() == Incident.Status.RESOLVED).count()
            };
            for (int i = 0; i < sl.length; i++) {
                PdfPCell cell = new PdfPCell();
                cell.addElement(new Phrase(String.valueOf(sv[i]), kFont(16, Font.BOLD)));
                cell.addElement(new Phrase(sl[i], bodyFont));
                cell.setPadding(8);
                cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                stats.addCell(cell);
            }
            doc.add(stats);
            doc.add(new Paragraph(" "));

            PdfPTable table = new PdfPTable(6);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{3.5f, 1.5f, 1.5f, 1.5f, 2, 2});
            for (String h : new String[]{
                    t(lang, "제목", "Title"),
                    t(lang, "유형", "Type"),
                    t(lang, "심각도", "Severity"),
                    t(lang, "상태", "Status"),
                    t(lang, "담당자", "Assignee"),
                    t(lang, "탐지 시각", "Detected At")}) {
                addHeaderCell(table, h, headerFont);
            }
            for (Incident i : incidents) {
                table.addCell(new Phrase(i.getTitle(), bodyFont));
                table.addCell(new Phrase(tIncidentType(lang, i.getType()), bodyFont));
                table.addCell(new Phrase(tIncidentSeverity(lang, i.getSeverity()), bodyFont));
                table.addCell(new Phrase(tIncidentStatus(lang, i.getStatus()), bodyFont));
                table.addCell(new Phrase(i.getAssignee() != null ? i.getAssignee().getName() : "-", bodyFont));
                table.addCell(new Phrase(i.getDetectedAt() != null ? i.getDetectedAt().format(DT_FMT) : "-", bodyFont));
            }
            doc.add(table);
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate incident PDF", e);
        } finally {
            doc.close();
        }
        return out.toByteArray();
    }

    // ── PDF: User Report ──────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public byte[] generateUserReport(String lang) {
        List<User> users = userRepository.findAll(Sort.by("createdAt").descending());
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document doc = new Document(PageSize.A4);
        try {
            PdfWriter.getInstance(doc, out);
            doc.open();
            Font titleFont  = kFont(18, Font.BOLD);
            Font headerFont = kFont(10, Font.BOLD, Color.WHITE);
            Font bodyFont   = kFont(9,  Font.NORMAL);

            addCenteredTitle(doc, t(lang, "사용자 관리 보고서", "User Management Report"), titleFont);
            addCompanyLine(doc);
            addCenteredSubtitle(doc, t(lang, "생성일: ", "Generated: ") + LocalDate.now().format(DATE_FMT));
            doc.add(new Paragraph(" "));

            PdfPTable stats = new PdfPTable(3);
            stats.setWidthPercentage(60);
            String[] sl = {t(lang, "전체", "Total"), t(lang, "활성", "Active"), t(lang, "관리자", "Admin")};
            long[] sv = {
                    users.size(),
                    users.stream().filter(User::isActive).count(),
                    users.stream().filter(u -> u.getRole() == User.Role.ADMIN).count()
            };
            for (int i = 0; i < sl.length; i++) {
                PdfPCell cell = new PdfPCell();
                cell.addElement(new Phrase(String.valueOf(sv[i]), kFont(16, Font.BOLD)));
                cell.addElement(new Phrase(sl[i], bodyFont));
                cell.setPadding(8);
                cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                stats.addCell(cell);
            }
            doc.add(stats);
            doc.add(new Paragraph(" "));

            PdfPTable table = new PdfPTable(5);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{2.5f, 3, 2, 1.5f, 1.5f});
            for (String h : new String[]{
                    t(lang, "이름", "Name"),
                    t(lang, "이메일", "Email"),
                    t(lang, "부서", "Department"),
                    t(lang, "역할", "Role"),
                    t(lang, "상태", "Status")}) {
                addHeaderCell(table, h, headerFont);
            }
            for (User u : users) {
                table.addCell(new Phrase(u.getName(), bodyFont));
                table.addCell(new Phrase(u.getEmail(), bodyFont));
                table.addCell(new Phrase(nvl(u.getDepartment()), bodyFont));
                table.addCell(new Phrase(tUserRole(lang, u.getRole()), bodyFont));
                table.addCell(new Phrase(u.isActive() ? t(lang, "활성", "Active") : t(lang, "비활성", "Inactive"), bodyFont));
            }
            doc.add(table);
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate user PDF", e);
        } finally {
            doc.close();
        }
        return out.toByteArray();
    }

    // ── PDF: ISMS-P Report ────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public byte[] generateIsmsReport(int year, String lang) {
        List<IsmsItem> items = ismsItemRepository.findAllByOrderBySortOrderAsc();
        List<IsmsEvidence> evidences = ismsEvidenceRepository.findByYearOrderByItemSortOrder(year);
        Map<Long, List<IsmsEvidence>> byItem = evidences.stream()
                .collect(Collectors.groupingBy(e -> e.getItem().getId()));

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document doc = new Document(PageSize.A4.rotate());
        try {
            PdfWriter.getInstance(doc, out);
            doc.open();
            Font titleFont  = kFont(18, Font.BOLD);
            Font headerFont = kFont(10, Font.BOLD, Color.WHITE);
            Font bodyFont   = kFont(9,  Font.NORMAL);

            addCenteredTitle(doc, t(lang, "ISMS-P 증적관리 보고서", "ISMS-P Evidence Report"), titleFont);
            addCompanyLine(doc);
            addCenteredSubtitle(doc,
                    t(lang, year + "년도 | 생성일: ", year + " | Generated: ") + LocalDate.now().format(DATE_FMT));
            doc.add(new Paragraph(" "));

            long compliant = items.stream().filter(it -> {
                List<IsmsEvidence> ev = byItem.get(it.getId());
                return ev != null && ev.stream().anyMatch(e -> e.getStatus() == IsmsEvidence.Status.COMPLIANT);
            }).count();

            PdfPTable stats = new PdfPTable(3);
            stats.setWidthPercentage(50);
            String[] sl = {t(lang, "전체 항목", "Total Items"), t(lang, "준수", "Compliant"), t(lang, "증적 없음", "No Evidence")};
            long[] sv = {items.size(), compliant, items.size() - byItem.size()};
            for (int i = 0; i < sl.length; i++) {
                PdfPCell cell = new PdfPCell();
                cell.addElement(new Phrase(String.valueOf(sv[i]), kFont(16, Font.BOLD)));
                cell.addElement(new Phrase(sl[i], bodyFont));
                cell.setPadding(8);
                cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                stats.addCell(cell);
            }
            doc.add(stats);
            doc.add(new Paragraph(" "));

            PdfPTable table = new PdfPTable(5);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{1.5f, 4, 2, 1.5f, 2});
            for (String h : new String[]{
                    t(lang, "항목코드", "Item Code"),
                    t(lang, "항목명", "Item Name"),
                    t(lang, "도메인", "Domain"),
                    t(lang, "증적 수", "Evidence"),
                    t(lang, "상태", "Status")}) {
                addHeaderCell(table, h, headerFont);
            }
            for (IsmsItem item : items) {
                List<IsmsEvidence> ev = byItem.getOrDefault(item.getId(), List.of());
                String status = ev.isEmpty() ? t(lang, "증적 없음", "No Evidence")
                        : tIsmsStatus(lang, ev.get(0).getStatus());
                table.addCell(new Phrase(item.getItemCode(), bodyFont));
                table.addCell(new Phrase(item.getItemName(), bodyFont));
                table.addCell(new Phrase(item.getDomainName(), bodyFont));
                table.addCell(new Phrase(String.valueOf(ev.size()), bodyFont));
                table.addCell(new Phrase(status, bodyFont));
            }
            doc.add(table);
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate ISMS PDF", e);
        } finally {
            doc.close();
        }
        return out.toByteArray();
    }

    // ── PDF: Training Report ─────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public byte[] generateTrainingReport(String lang) {
        boolean ko = "ko".equalsIgnoreCase(lang);
        List<TrainingCourse> courses = courseRepository.findAll();
        List<TrainingCompletion> completions = completionRepository.findAllByOrderByCompletedAtDesc();

        Map<Long, List<TrainingCompletion>> byCourse = completions.stream()
                .collect(Collectors.groupingBy(c -> c.getCourse().getId()));

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document doc = new Document(PageSize.A4.rotate());
        try {
            PdfWriter.getInstance(doc, out);
            doc.open();
            Font titleFont  = kFont(18, Font.BOLD);
            Font headerFont = kFont(10, Font.BOLD, Color.WHITE);
            Font bodyFont   = kFont(9,  Font.NORMAL);

            addCenteredTitle(doc, ko ? "교육 이수 현황 보고서" : "Training Completion Report", titleFont);
            addCompanyLine(doc);
            addCenteredSubtitle(doc, (ko ? "생성일: " : "Generated: ") + LocalDate.now().format(DATE_FMT));
            doc.add(new Paragraph(" "));

            PdfPTable table = new PdfPTable(6);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{4, 1.5f, 1.5f, 1.5f, 1.5f, 1.5f});
            String[] summaryHeaders = ko
                    ? new String[]{"과정명", "필수여부", "수료인원", "합격인원", "평균점수", "합격률"}
                    : new String[]{"Course Title", "Mandatory", "Completed", "Passed", "Avg Score", "Pass Rate"};
            for (String h : summaryHeaders) {
                addHeaderCell(table, h, headerFont);
            }
            for (TrainingCourse course : courses) {
                List<TrainingCompletion> cs = byCourse.getOrDefault(course.getId(), List.of());
                long passed = cs.stream().filter(c -> Boolean.TRUE.equals(c.getPassed())).count();
                double avg = cs.stream().filter(c -> c.getScore() != null)
                        .mapToInt(TrainingCompletion::getScore).average().orElse(0);
                double passRate = cs.isEmpty() ? 0 : (passed * 100.0 / cs.size());
                table.addCell(new Phrase(course.getTitle(), bodyFont));
                table.addCell(new Phrase(course.isMandatory() ? (ko ? "필수" : "Yes") : (ko ? "선택" : "No"), bodyFont));
                table.addCell(new Phrase(String.valueOf(cs.size()), bodyFont));
                table.addCell(new Phrase(String.valueOf(passed), bodyFont));
                table.addCell(new Phrase(String.format("%.1f", avg), bodyFont));
                table.addCell(new Phrase(String.format("%.1f%%", passRate), bodyFont));
            }
            doc.add(table);

            doc.add(new Paragraph(" "));
            doc.add(new Paragraph(ko ? "최근 이수 내역" : "Recent Completion Details", kFont(12, Font.BOLD)));
            doc.add(new Paragraph(" "));

            PdfPTable detailTable = new PdfPTable(5);
            detailTable.setWidthPercentage(100);
            detailTable.setWidths(new float[]{3, 3, 1.5f, 1.5f, 2});
            String[] detailHeaders = ko
                    ? new String[]{"사용자", "과정명", "점수", "합격여부", "이수일시"}
                    : new String[]{"User", "Course", "Score", "Passed", "Completed At"};
            for (String h : detailHeaders) {
                addHeaderCell(detailTable, h, headerFont);
            }
            for (TrainingCompletion c : completions) {
                detailTable.addCell(new Phrase(c.getUser().getName(), bodyFont));
                detailTable.addCell(new Phrase(c.getCourse().getTitle(), bodyFont));
                detailTable.addCell(new Phrase(c.getScore() != null ? c.getScore() + (ko ? "점" : "") : "-", bodyFont));
                detailTable.addCell(new Phrase(Boolean.TRUE.equals(c.getPassed()) ? (ko ? "합격" : "Pass") : (ko ? "불합격" : "Fail"), bodyFont));
                detailTable.addCell(new Phrase(c.getCompletedAt() != null ? c.getCompletedAt().format(DT_FMT) : "-", bodyFont));
            }
            doc.add(detailTable);
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate training PDF", e);
        } finally {
            doc.close();
        }
        return out.toByteArray();
    }

    // ── CSV Exports ──────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public byte[] exportVulnerabilityCsv(String lang) {
        List<Vulnerability> vulns = vulnerabilityRepository.findAll(Sort.by("createdAt").descending());
        StringBuilder sb = new StringBuilder("﻿");
        if ("ko".equalsIgnoreCase(lang)) {
            sb.append("ID,제목,심각도,상태,CVE ID,CVSS 점수,자산명,담당자,보고자,조치 기한,등록일\n");
        } else {
            sb.append("ID,Title,Severity,Status,CVE ID,CVSS Score,Asset,Assignee,Reporter,Due Date,Created At\n");
        }
        for (Vulnerability v : vulns) {
            sb.append(row(
                    v.getId(), csv(v.getTitle()), v.getSeverity(), v.getStatus(),
                    nvl(v.getCveId()), nvl(v.getCvssScore()), nvl(v.getAssetName()),
                    v.getAssignee() != null ? v.getAssignee().getName() : "",
                    v.getReporter().getName(),
                    v.getDueDate() != null ? v.getDueDate().format(DATE_FMT) : "",
                    v.getCreatedAt() != null ? v.getCreatedAt().format(DT_FMT) : ""
            ));
        }
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    @Transactional(readOnly = true)
    public byte[] exportAssetCsv(String lang) {
        List<Asset> assets = assetRepository.findAll(Sort.by("createdAt").descending());
        StringBuilder sb = new StringBuilder("﻿");
        if ("ko".equalsIgnoreCase(lang)) {
            sb.append("ID,자산명,유형,환경,클라우드 공급자,클라우드 리소스 ID,리전,IP 주소,운영체제,사양,담당자,부서,중요도,월 비용,계약 만료일,최근 점검일,다음 점검일,활성,등록일\n");
        } else {
            sb.append("ID,Name,Type,Environment,Cloud Provider,Cloud Resource ID,Region,IP Address,OS Type,Spec,Owner,Department,Criticality,Monthly Cost,Contract Expiry,Last Inspection,Next Inspection,Active,Created At\n");
        }
        for (Asset a : assets) {
            sb.append(row(
                    a.getId(), csv(a.getName()), a.getType(),
                    nvl(a.getEnvironment()), nvl(a.getCloudProvider()),
                    nvl(a.getCloudResourceId()), nvl(a.getRegion()),
                    nvl(a.getIpAddress()), nvl(a.getOsType()), csv(nvl(a.getSpec())),
                    nvl(a.getOwner()), nvl(a.getDepartment()), a.getCriticality(),
                    a.getMonthlyCost() != null ? a.getMonthlyCost().toPlainString() : "",
                    a.getContractExpiry() != null ? a.getContractExpiry().format(DATE_FMT) : "",
                    a.getLastInspectionDate() != null ? a.getLastInspectionDate().format(DATE_FMT) : "",
                    a.getNextInspectionDate() != null ? a.getNextInspectionDate().format(DATE_FMT) : "",
                    a.isActive() ? "Y" : "N",
                    a.getCreatedAt() != null ? a.getCreatedAt().format(DT_FMT) : ""
            ));
        }
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    @Transactional(readOnly = true)
    public byte[] exportPolicyCsv(String lang) {
        List<Policy> policies = policyRepository.findAll(Sort.by("createdAt").descending());
        StringBuilder sb = new StringBuilder("﻿");
        if ("ko".equalsIgnoreCase(lang)) {
            sb.append("ID,제목,카테고리,상태,버전,작성자,시행일,등록일\n");
        } else {
            sb.append("ID,Title,Category,Status,Version,Author,Effective Date,Created At\n");
        }
        for (Policy p : policies) {
            sb.append(row(
                    p.getId(), csv(p.getTitle()), p.getCategory(), p.getStatus(), p.getVersion(),
                    p.getAuthor().getName(),
                    p.getEffectiveDate() != null ? p.getEffectiveDate().format(DATE_FMT) : "",
                    p.getCreatedAt() != null ? p.getCreatedAt().format(DT_FMT) : ""
            ));
        }
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    @Transactional(readOnly = true)
    public byte[] exportIncidentCsv(String lang) {
        List<Incident> incidents = incidentRepository.findAllByOrderByCreatedAtDesc();
        StringBuilder sb = new StringBuilder("﻿");
        if ("ko".equalsIgnoreCase(lang)) {
            sb.append("ID,제목,심각도,상태,유형,보고자,담당자,탐지 시각,해결 시각,등록일\n");
        } else {
            sb.append("ID,Title,Severity,Status,Type,Reporter,Assignee,Detected At,Resolved At,Created At\n");
        }
        for (Incident i : incidents) {
            sb.append(row(
                    i.getId(), csv(i.getTitle()), i.getSeverity(), i.getStatus(), i.getType(),
                    i.getReporter().getName(),
                    i.getAssignee() != null ? i.getAssignee().getName() : "",
                    i.getDetectedAt() != null ? i.getDetectedAt().format(DT_FMT) : "",
                    i.getResolvedAt() != null ? i.getResolvedAt().format(DT_FMT) : "",
                    i.getCreatedAt() != null ? i.getCreatedAt().format(DT_FMT) : ""
            ));
        }
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    @Transactional(readOnly = true)
    public byte[] exportUserCsv(String lang) {
        List<User> users = userRepository.findAll(Sort.by("createdAt").descending());
        StringBuilder sb = new StringBuilder("﻿");
        if ("ko".equalsIgnoreCase(lang)) {
            sb.append("ID,이름,이메일,역할,부서,활성,등록일\n");
        } else {
            sb.append("ID,Name,Email,Role,Department,Active,Created At\n");
        }
        for (User u : users) {
            sb.append(row(
                    u.getId(), csv(u.getName()), u.getEmail(),
                    u.getRole().name(), nvl(u.getDepartment()),
                    u.isActive() ? "Y" : "N",
                    u.getCreatedAt() != null ? u.getCreatedAt().format(DT_FMT) : ""
            ));
        }
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private String row(Object... fields) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < fields.length; i++) {
            if (i > 0) sb.append(',');
            sb.append(fields[i] != null ? fields[i].toString() : "");
        }
        return sb.append('\n').toString();
    }

    private String csv(String s) {
        if (s == null) return "";
        if (s.contains(",") || s.contains("\"") || s.contains("\n")) {
            return "\"" + s.replace("\"", "\"\"") + "\"";
        }
        return s;
    }

    private String nvl(Object o) { return o != null ? o.toString() : ""; }

    // ── 소스 취약점 점검(SAST) 보고서 ────────────────────────────────────────

    /**
     * 소스 취약점 점검 결과 PDF — 화면(보안 운영 > 소스 취약점 점검)의 점검 1건을 그대로 옮긴다.
     * 요약(심각도·카테고리) → 심각도 분포 막대 → 발견 목록 순서.
     */
    @Transactional(readOnly = true)
    public byte[] generateSourceScanReport(Long scanId, String lang) {
        SourceScan scan = sourceScanRepository.findById(scanId)
                .orElseThrow(() -> new ResourceNotFoundException("SourceScan", scanId));
        List<SourceScanFinding> findings = scan.getFindings();

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document doc = new Document(PageSize.A4.rotate(), 30, 30, 32, 30);
        try {
            PdfWriter.getInstance(doc, out);
            doc.open();

            addCenteredTitle(doc, t(lang, "소스 취약점 점검 보고서", "Source Code Scan Report"), kFont(17, Font.BOLD));
            addCompanyLine(doc);
            addCenteredSubtitle(doc, scan.getRepository() + "   ·   "
                    + t(lang, "점검일시 ", "Scanned at ")
                    + scan.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
            doc.add(new Paragraph(" ", kFont(6, Font.NORMAL)));

            // 점검 상태 (일부 카테고리 실패 시 사유를 함께 싣는다)
            if (scan.getStatus() != SourceScan.Status.SUCCESS && notBlankStr(scan.getMessage())) {
                PdfPTable note = new PdfPTable(1);
                note.setWidthPercentage(100);
                PdfPCell nc = new PdfPCell();
                nc.setPadding(7);
                nc.setBackgroundColor(new Color(0xFF, 0xFB, 0xEB));
                nc.setBorderColor(new Color(0xFD, 0xE6, 0x8A));
                nc.addElement(new Paragraph(t(lang, "점검 상태: ", "Status: ") + scan.getStatus().name(),
                        kFont(8.5f, Font.BOLD, new Color(0x92, 0x40, 0x0E))));
                nc.addElement(new Paragraph(scan.getMessage(), kFont(8, Font.NORMAL, C_INK2)));
                note.addCell(nc);
                doc.add(note);
                doc.add(new Paragraph(" ", kFont(6, Font.NORMAL)));
            }

            int total = scan.getDependencyCount() + scan.getCodeCount() + scan.getSecretCount() + scan.getSastCount();

            // 심각도 요약 타일
            String[][] tiles = {
                    {t(lang, "전체", "Total"), String.valueOf(total)},
                    {t(lang, "심각(Critical)", "Critical"), String.valueOf(scan.getCriticalCount())},
                    {t(lang, "높음(High)", "High"), String.valueOf(scan.getHighCount())},
                    {t(lang, "중간(Medium)", "Medium"), String.valueOf(scan.getMediumCount())},
                    {t(lang, "낮음(Low)", "Low"), String.valueOf(scan.getLowCount())},
            };
            PdfPTable tileTable = new PdfPTable(tiles.length);
            tileTable.setWidthPercentage(100);
            for (String[] tile : tiles) {
                PdfPCell cell = new PdfPCell();
                Paragraph v = new Paragraph(tile[1], kFont(17, Font.BOLD));
                v.setAlignment(Element.ALIGN_CENTER);
                Paragraph l = new Paragraph(tile[0], kFont(8, Font.NORMAL, C_INK2));
                l.setAlignment(Element.ALIGN_CENTER);
                cell.addElement(v);
                cell.addElement(l);
                cell.setPadding(7);
                cell.setBackgroundColor(new Color(0xF9, 0xFA, 0xFB));
                cell.setBorderColor(C_LINE);
                tileTable.addCell(cell);
            }
            doc.add(tileTable);
            doc.add(new Paragraph(" ", kFont(7, Font.NORMAL)));

            // 심각도 분포 · 카테고리 분포 (화면과 같은 색)
            PdfPTable charts = new PdfPTable(2);
            charts.setWidthPercentage(100);
            addStackCell(charts, t(lang, "심각도 분포", "By severity"), List.of(
                    new Seg(t(lang, "심각", "Critical"), scan.getCriticalCount(), C_CRITICAL),
                    new Seg(t(lang, "높음", "High"), scan.getHighCount(), C_SERIOUS),
                    new Seg(t(lang, "중간", "Medium"), scan.getMediumCount(), C_WARNING),
                    new Seg(t(lang, "낮음", "Low"), scan.getLowCount(), C_SEQ)));
            addStackCell(charts, t(lang, "카테고리 분포", "By category"), List.of(
                    new Seg(t(lang, "의존성", "Dependency"), scan.getDependencyCount(), C_SEQ),
                    new Seg(t(lang, "코드스캔", "Code scanning"), scan.getCodeCount(), C_SERIOUS),
                    new Seg(t(lang, "시크릿", "Secret"), scan.getSecretCount(), C_CRITICAL),
                    new Seg("SAST", scan.getSastCount(), C_NEUTRAL)));
            doc.add(charts);
            doc.add(new Paragraph(" ", kFont(9, Font.NORMAL)));

            // 발견 목록
            doc.add(new Paragraph(t(lang, "발견 내역 (" + findings.size() + "건)",
                    "Findings (" + findings.size() + ")"), kFont(11, Font.BOLD)));
            doc.add(new Paragraph(" ", kFont(4, Font.NORMAL)));

            if (findings.isEmpty()) {
                doc.add(new Paragraph(t(lang, "발견된 취약점이 없습니다.", "No findings."),
                        kFont(9, Font.NORMAL, C_INK2)));
            } else {
                PdfPTable table = new PdfPTable(new float[]{1.1f, 1.4f, 5f, 2.6f, 3.2f, 1.4f});
                table.setWidthPercentage(100);
                table.setHeaderRows(1);
                Font hf = kFont(8.5f, Font.BOLD, Color.WHITE);
                addHeaderCell(table, t(lang, "심각도", "Severity"), hf);
                addHeaderCell(table, t(lang, "구분", "Category"), hf);
                addHeaderCell(table, t(lang, "제목", "Title"), hf);
                addHeaderCell(table, t(lang, "식별자", "Identifier"), hf);
                addHeaderCell(table, t(lang, "위치", "Location"), hf);
                addHeaderCell(table, "CVE", hf);

                for (SourceScanFinding f : findings) {
                    PdfPCell sev = new PdfPCell(new Phrase(tScanSeverity(lang, f.getSeverity()),
                            kFont(8, Font.BOLD, severityColor(f.getSeverity()))));
                    table.addCell(padded(sev));
                    table.addCell(padded(new PdfPCell(new Phrase(
                            tScanCategory(lang, f.getCategory()), kFont(8, Font.NORMAL, C_INK2)))));
                    table.addCell(padded(new PdfPCell(new Phrase(nvl(f.getTitle()), kFont(8, Font.NORMAL)))));
                    table.addCell(padded(new PdfPCell(new Phrase(nvl(f.getIdentifier()), kFont(8, Font.NORMAL, C_INK2)))));
                    table.addCell(padded(new PdfPCell(new Phrase(nvl(f.getLocation()), kFont(8, Font.NORMAL, C_INK2)))));
                    table.addCell(padded(new PdfPCell(new Phrase(nvl(f.getCveId()), kFont(8, Font.NORMAL, C_INK2)))));
                }
                doc.add(table);
            }

            doc.close();
        } catch (Exception e) {
            log.error("Failed to generate source scan report", e);
            throw new RuntimeException("Failed to generate source scan report", e);
        }
        return out.toByteArray();
    }

    private PdfPCell padded(PdfPCell cell) {
        cell.setPadding(4);
        cell.setBorderColor(C_LINE);
        return cell;
    }

    private Color severityColor(SourceScanFinding.Severity s) {
        return switch (s) {
            case CRITICAL -> C_CRITICAL;
            case HIGH -> C_SERIOUS;
            case MEDIUM -> new Color(0xB4, 0x7C, 0x00);   // 노랑은 흰 배경에서 흐려 어둡게 조정
            case LOW -> C_SEQ;
            default -> C_INK2;
        };
    }

    private String tScanSeverity(String lang, SourceScanFinding.Severity s) {
        if (!"ko".equalsIgnoreCase(lang)) return s.name();
        return switch (s) {
            case CRITICAL -> "심각";
            case HIGH -> "높음";
            case MEDIUM -> "중간";
            case LOW -> "낮음";
            default -> "정보";
        };
    }

    private String tScanCategory(String lang, SourceScanFinding.Category c) {
        if (!"ko".equalsIgnoreCase(lang)) return c.name();
        return switch (c) {
            case DEPENDENCY -> "의존성";
            case CODE_SCANNING -> "코드스캔";
            case SECRET -> "시크릿";
            case SAST -> "SAST";
        };
    }

    private static boolean notBlankStr(String s) { return s != null && !s.isBlank(); }

    // ── 개인정보 현황보고서 ──────────────────────────────────────────────────

    // 화면(PrivacyReportView)과 같은 색 — 상태색은 상태 전용, SEQ(파랑)는 크기 비교용 단일 색
    private static final Color C_GOOD = new Color(0x0C, 0xA3, 0x0C);
    private static final Color C_WARNING = new Color(0xFA, 0xB2, 0x19);
    private static final Color C_SERIOUS = new Color(0xEC, 0x83, 0x5A);
    private static final Color C_CRITICAL = new Color(0xD0, 0x3B, 0x3B);
    private static final Color C_NEUTRAL = new Color(0xB9, 0xB8, 0xB2);
    private static final Color C_SEQ = new Color(0x2A, 0x78, 0xD6);
    private static final Color C_TRACK = new Color(0xF1, 0xF2, 0xF4);
    private static final Color C_LINE = new Color(0xE5, 0xE7, 0xEB);
    private static final Color C_INK2 = new Color(0x6B, 0x72, 0x80);

    /** 막대 한 조각 — 화면의 스택 막대 세그먼트와 같은 의미 */
    private record Seg(String label, long value, Color color) {}

    /**
     * 개인정보 현황보고서 PDF — 화면(개인정보보호 > 개인정보 현황보고서)을 그대로 옮긴다.
     * 조치 필요 → 핵심 지표 → 이행률 미터 → 영역별 누적 막대 → 유형별 막대 → 상세 수치(표 보기).
     * 집계는 PrivacyReportService를 재사용하므로 화면과 값이 항상 일치한다.
     */
    @Transactional(readOnly = true)
    public byte[] generatePrivacyReport(String lang) {
        var s = privacyReportService.generate();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document doc = new Document(PageSize.A4, 36, 36, 36, 36);
        try {
            PdfWriter.getInstance(doc, out);
            doc.open();

            addCenteredTitle(doc, t(lang, "개인정보 현황보고서", "Privacy Status Report"), kFont(18, Font.BOLD));
            addCompanyLine(doc);
            addCenteredSubtitle(doc, t(lang, "기준일: ", "As of: ") + s.getGeneratedAt().format(DATE_FMT));
            doc.add(new Paragraph(" ", kFont(6, Font.NORMAL)));

            addPrivacyAlerts(doc, lang, s);
            addPrivacyKpis(doc, lang, s);
            addPrivacyMeters(doc, lang, s);
            addPrivacyComposition(doc, lang, s);
            addPrivacyByType(doc, lang, s);
            addPrivacyDetailTable(doc, lang, s);

            doc.close();
        } catch (Exception e) {
            log.error("Failed to generate privacy report", e);
            throw new RuntimeException("Failed to generate privacy report", e);
        }
        return out.toByteArray();
    }

    /** 즉시 조치 필요 — 0이 아닌 항목만 (화면 상단 배너와 동일) */
    private void addPrivacyAlerts(Document doc, String lang, PrivacyReportDto.Summary s) throws DocumentException {
        String[][] candidates = {
                {t(lang, "보유기간 만료 경과", "Retention overdue"), String.valueOf(s.getRetentions().getOverdue())},
                {t(lang, "유출 신고기한 경과", "Breach report overdue"), String.valueOf(s.getBreaches().getReportOverdue())},
                {t(lang, "권리행사 처리기한 초과", "Rights SLA breached"), String.valueOf(s.getRights().getSlaBreached())},
                {t(lang, "유출사고 미종결", "Breaches open"), String.valueOf(s.getBreaches().getOpen())},
                {t(lang, "수탁사 미점검", "Contractors not inspected"), String.valueOf(s.getContractors().getUnchecked())},
                {t(lang, "영향평가 위험 높음", "DPIA high risk"), String.valueOf(s.getCompliance().getDpiaHighRisk())},
        };
        StringBuilder sb = new StringBuilder();
        for (String[] c : candidates) {
            if (Long.parseLong(c[1]) > 0) {
                if (sb.length() > 0) sb.append("    ");
                sb.append(c[0]).append(' ').append(c[1]).append(t(lang, "건", ""));
            }
        }
        boolean clean = sb.length() == 0;

        PdfPTable box = new PdfPTable(1);
        box.setWidthPercentage(100);
        PdfPCell cell = new PdfPCell();
        cell.setPadding(8);
        cell.setBackgroundColor(clean ? new Color(0xEC, 0xFD, 0xF5) : new Color(0xFE, 0xF2, 0xF2));
        cell.setBorderColor(clean ? new Color(0xA7, 0xF3, 0xD0) : new Color(0xFE, 0xCA, 0xCA));
        if (clean) {
            cell.addElement(new Paragraph(
                    t(lang, "기한 초과·미조치 항목이 없습니다.", "No overdue or unhandled items."),
                    kFont(9, Font.BOLD, new Color(0x04, 0x78, 0x57))));
        } else {
            cell.addElement(new Paragraph(t(lang, "즉시 조치 필요", "Action required"),
                    kFont(9, Font.BOLD, new Color(0xB9, 0x1C, 0x1C))));
            cell.addElement(new Paragraph(sb.toString(), kFont(9, Font.NORMAL, new Color(0x37, 0x41, 0x51))));
        }
        box.addCell(cell);
        doc.add(box);
        doc.add(new Paragraph(" ", kFont(7, Font.NORMAL)));
    }

    /** 핵심 지표 4타일 (화면 상단 KPI 행) */
    private void addPrivacyKpis(Document doc, String lang, PrivacyReportDto.Summary s) throws DocumentException {
        String[][] kpis = {
                {t(lang, "개인정보파일", "Personal data files"), String.valueOf(s.getFiles().getTotal()),
                        t(lang, "운영중 " + s.getFiles().getActive() + " · 민감 " + s.getFiles().getSensitive()
                                        + " · 고유식별 " + s.getFiles().getUniqueIdentifier(),
                                "Active " + s.getFiles().getActive() + " · Sensitive " + s.getFiles().getSensitive()
                                        + " · Unique ID " + s.getFiles().getUniqueIdentifier())},
                {t(lang, "개인정보 처리업무", "Processing activities"), String.valueOf(s.getProcessing().getTotal()),
                        t(lang, "운영중 " + s.getProcessing().getActive() + " · 중단 " + s.getProcessing().getInactive(),
                                "Active " + s.getProcessing().getActive() + " · Inactive " + s.getProcessing().getInactive())},
                {t(lang, "수탁사", "Contractors"), String.valueOf(s.getContractors().getTotal()),
                        t(lang, "점검 " + s.getContractors().getChecked() + " · 미점검 " + s.getContractors().getUnchecked(),
                                "Inspected " + s.getContractors().getChecked() + " · Not " + s.getContractors().getUnchecked())},
                {t(lang, "정보주체 권리행사", "Data subject rights"), String.valueOf(s.getRights().getTotal()),
                        t(lang, "처리중 " + s.getRights().getInProgress() + " · 완료 " + s.getRights().getCompleted(),
                                "In progress " + s.getRights().getInProgress() + " · Completed " + s.getRights().getCompleted())},
        };

        PdfPTable table = new PdfPTable(kpis.length);
        table.setWidthPercentage(100);
        for (String[] k : kpis) {
            PdfPCell cell = new PdfPCell();
            cell.addElement(new Paragraph(k[0], kFont(8, Font.NORMAL, C_INK2)));
            cell.addElement(new Paragraph(k[1], kFont(19, Font.BOLD)));
            cell.addElement(new Paragraph(k[2], kFont(7, Font.NORMAL, C_INK2)));
            cell.setPadding(8);
            cell.setBackgroundColor(new Color(0xF9, 0xFA, 0xFB));
            cell.setBorderColor(C_LINE);
            table.addCell(cell);
        }
        doc.add(table);
        doc.add(new Paragraph(" ", kFont(8, Font.NORMAL)));
    }

    /** 이행률 — 화면의 비율 미터 4종 (90%↑ 녹색 / 70%↑ 노랑 / 그 미만 빨강) */
    private void addPrivacyMeters(Document doc, String lang, PrivacyReportDto.Summary s) throws DocumentException {
        doc.add(new Paragraph(t(lang, "이행률", "Completion rate"), kFont(11, Font.BOLD)));
        doc.add(new Paragraph(" ", kFont(4, Font.NORMAL)));

        PdfPTable grid = new PdfPTable(2);
        grid.setWidthPercentage(100);
        grid.getDefaultCell().setBorder(com.lowagie.text.Rectangle.NO_BORDER);
        addMeterCell(grid, t(lang, "수탁사 점검", "Contractor inspection"),
                s.getContractors().getChecked(), s.getContractors().getTotal());
        addMeterCell(grid, t(lang, "파기 완료", "Disposal completed"),
                s.getDisposals().getCompleted(), s.getDisposals().getTotal());
        addMeterCell(grid, t(lang, "영향평가(DPIA) 완료", "DPIA completed"),
                s.getCompliance().getDpiaCompleted(), s.getCompliance().getDpiaTotal());
        addMeterCell(grid, t(lang, "안전조치 완료", "Safeguards completed"),
                s.getCompliance().getSafeguardCompleted(), s.getCompliance().getSafeguardTotal());
        doc.add(grid);
        doc.add(new Paragraph(" ", kFont(8, Font.NORMAL)));
    }

    private void addMeterCell(PdfPTable grid, String label, long value, long total) throws DocumentException {
        int pct = total > 0 ? Math.round(value * 100f / total) : 0;
        Color fill = total == 0 ? C_NEUTRAL : pct >= 90 ? C_GOOD : pct >= 70 ? C_WARNING : C_CRITICAL;

        PdfPTable head = new PdfPTable(2);
        head.setWidthPercentage(100);
        head.addCell(textCell(label, kFont(8.5f, Font.NORMAL, C_INK2), Element.ALIGN_LEFT));
        head.addCell(textCell(total > 0 ? pct + "%" : "—", kFont(9.5f, Font.BOLD), Element.ALIGN_RIGHT));

        PdfPCell cell = new PdfPCell();
        cell.setBorder(com.lowagie.text.Rectangle.NO_BORDER);
        cell.setPadding(4);
        cell.addElement(head);
        cell.addElement(barTable(List.of(
                new Seg(label, Math.max(value, 0), fill),
                new Seg("", Math.max(total - value, 0), C_TRACK)), 7f));
        cell.addElement(new Paragraph(value + " / " + total, kFont(7, Font.NORMAL, C_INK2)));
        grid.addCell(cell);
    }

    /** 영역별 구성 — 화면의 가로 누적 막대 7종 (2열 배치) */
    private void addPrivacyComposition(Document doc, String lang, PrivacyReportDto.Summary s) throws DocumentException {
        doc.add(new Paragraph(t(lang, "영역별 구성", "Composition by area"), kFont(11, Font.BOLD)));
        doc.add(new Paragraph(" ", kFont(4, Font.NORMAL)));

        var ret = s.getRetentions();
        long retNormal = Math.max(ret.getTotal() - ret.getExpiringIn30Days() - ret.getOverdue() - ret.getDisposed(), 0);

        PdfPTable grid = new PdfPTable(2);
        grid.setWidthPercentage(100);
        addStackCell(grid, t(lang, "개인정보 처리업무", "Processing activities"), List.of(
                new Seg(t(lang, "운영중", "Active"), s.getProcessing().getActive(), C_GOOD),
                new Seg(t(lang, "중단", "Inactive"), s.getProcessing().getInactive(), C_NEUTRAL)));
        addStackCell(grid, t(lang, "보유기간", "Retention"), List.of(
                new Seg(t(lang, "기간 내", "Within period"), retNormal, C_GOOD),
                new Seg(t(lang, "30일 내 만료", "Expiring in 30d"), ret.getExpiringIn30Days(), C_WARNING),
                new Seg(t(lang, "만료 경과", "Overdue"), ret.getOverdue(), C_CRITICAL),
                new Seg(t(lang, "파기 완료", "Disposed"), ret.getDisposed(), C_NEUTRAL)));
        addStackCell(grid, t(lang, "개인정보파일", "Personal data files"), List.of(
                new Seg(t(lang, "운영중", "Active"), s.getFiles().getActive(), C_GOOD),
                new Seg(t(lang, "그 외", "Others"),
                        Math.max(s.getFiles().getTotal() - s.getFiles().getActive(), 0), C_NEUTRAL)));
        addStackCell(grid, t(lang, "파기", "Disposal"), List.of(
                new Seg(t(lang, "완료", "Completed"), s.getDisposals().getCompleted(), C_GOOD),
                new Seg(t(lang, "승인대기", "Pending approval"), s.getDisposals().getPendingApproval(), C_WARNING),
                new Seg(t(lang, "계획", "Planned"), s.getDisposals().getPlanned(), C_NEUTRAL)));
        addStackCell(grid, t(lang, "제3자 제공·위탁", "Data provision"), List.of(
                new Seg(t(lang, "제3자 제공", "Third party"), s.getProvisions().getThirdParty(), C_SEQ),
                new Seg(t(lang, "공동이용", "Joint use"), s.getProvisions().getJointUse(), C_NEUTRAL),
                new Seg(t(lang, "국외이전", "Overseas"), s.getProvisions().getOverseas(), C_SERIOUS)));
        addStackCell(grid, t(lang, "정보주체 권리행사", "Data subject rights"), List.of(
                new Seg(t(lang, "완료", "Completed"), s.getRights().getCompleted(), C_GOOD),
                new Seg(t(lang, "처리중", "In progress"), s.getRights().getInProgress(), C_SEQ),
                new Seg(t(lang, "기한 초과", "SLA breached"), s.getRights().getSlaBreached(), C_CRITICAL)));
        addStackCell(grid, t(lang, "유출사고", "Breaches"), List.of(
                new Seg(t(lang, "종결", "Closed"),
                        Math.max(s.getBreaches().getTotal() - s.getBreaches().getOpen(), 0), C_GOOD),
                new Seg(t(lang, "미종결", "Open"), s.getBreaches().getOpen(), C_CRITICAL)));
        // 2열 격자를 채우기 위한 빈 칸 (홀수 개일 때)
        PdfPCell filler = new PdfPCell(new Phrase(" "));
        filler.setBorder(com.lowagie.text.Rectangle.NO_BORDER);
        grid.addCell(filler);

        doc.add(grid);
        doc.add(new Paragraph(" ", kFont(6, Font.NORMAL)));
        doc.add(new Paragraph(t(lang,
                        "유출 정보주체 누계 " + s.getBreaches().getAffectedSubjects() + "명 · 영향평가 위험 높음 "
                                + s.getCompliance().getDpiaHighRisk() + "건",
                        "Affected subjects " + s.getBreaches().getAffectedSubjects() + " · DPIA high risk "
                                + s.getCompliance().getDpiaHighRisk()),
                kFont(7.5f, Font.NORMAL, C_INK2)));
        doc.add(new Paragraph(" ", kFont(8, Font.NORMAL)));
    }

    /** 누적 막대 한 칸 — 제목 + 총계 / 막대 / 색 범례 (화면과 동일 구성) */
    private void addStackCell(PdfPTable grid, String title, List<Seg> segs) throws DocumentException {
        long total = segs.stream().mapToLong(Seg::value).sum();

        PdfPTable head = new PdfPTable(2);
        head.setWidthPercentage(100);
        head.addCell(textCell(title, kFont(8.5f, Font.BOLD), Element.ALIGN_LEFT));
        head.addCell(textCell("총 " + total, kFont(7.5f, Font.NORMAL, C_INK2), Element.ALIGN_RIGHT));

        PdfPCell cell = new PdfPCell();
        cell.setBorder(com.lowagie.text.Rectangle.NO_BORDER);
        cell.setPadding(4);
        cell.addElement(head);
        cell.addElement(barTable(segs, 9f));
        cell.addElement(legendTable(segs));
        grid.addCell(cell);
    }

    /** 색 범례 — 색 스와치 + 라벨 + 건수 (색만으로 구분하지 않는다) */
    private PdfPTable legendTable(List<Seg> segs) {
        PdfPTable legend = new PdfPTable(segs.size() * 2);
        legend.setWidthPercentage(100);
        float[] widths = new float[segs.size() * 2];
        for (int i = 0; i < segs.size(); i++) {
            widths[i * 2] = 1f;
            widths[i * 2 + 1] = 7f;
        }
        try {
            legend.setWidths(widths);
        } catch (DocumentException ignored) {
            // 폭 계산 실패 시 균등 분할로 둔다
        }
        for (Seg seg : segs) {
            PdfPCell swatch = new PdfPCell(new Phrase(" "));
            swatch.setBackgroundColor(seg.color());
            swatch.setBorder(com.lowagie.text.Rectangle.NO_BORDER);
            swatch.setFixedHeight(6f);
            swatch.setPaddingTop(2f);
            legend.addCell(swatch);
            legend.addCell(textCell(" " + seg.label() + " " + seg.value(),
                    kFont(7, Font.NORMAL, C_INK2), Element.ALIGN_LEFT));
        }
        return legend;
    }

    /** 유형별 — 건수 순 가로 막대 (단일 색, 값은 막대 끝에 표기) */
    private void addPrivacyByType(Document doc, String lang, PrivacyReportDto.Summary s) throws DocumentException {
        PdfPTable grid = new PdfPTable(2);
        grid.setWidthPercentage(100);

        List<Seg> rights = s.getRights().getByType() == null ? List.of()
                : s.getRights().getByType().entrySet().stream()
                .map(e -> new Seg(tRightsType(lang, e.getKey()), e.getValue(), C_SEQ))
                .sorted((a, b) -> Long.compare(b.value(), a.value()))
                .collect(Collectors.toList());
        List<Seg> safeguards = s.getCompliance().getSafeguardByType() == null ? List.of()
                : s.getCompliance().getSafeguardByType().stream()
                .map(x -> new Seg(tSafeguardType(lang, x.getType()), x.getCount(), C_SEQ))
                .sorted((a, b) -> Long.compare(b.value(), a.value()))
                .collect(Collectors.toList());

        addRankCell(grid, t(lang, "권리행사 유형별", "Rights by type"), rights,
                t(lang, "접수된 권리행사가 없습니다.", "No requests."));
        addRankCell(grid, t(lang, "안전조치 유형별", "Safeguards by type"), safeguards,
                t(lang, "등록된 안전조치가 없습니다.", "No safeguards."));
        doc.add(grid);
    }

    private void addRankCell(PdfPTable grid, String title, List<Seg> items, String empty) throws DocumentException {
        PdfPCell cell = new PdfPCell();
        cell.setBorder(com.lowagie.text.Rectangle.NO_BORDER);
        cell.setPadding(4);
        cell.addElement(new Paragraph(title, kFont(8.5f, Font.BOLD)));
        if (items.isEmpty()) {
            cell.addElement(new Paragraph(empty, kFont(7.5f, Font.NORMAL, C_INK2)));
            grid.addCell(cell);
            return;
        }
        long max = items.stream().mapToLong(Seg::value).max().orElse(1);
        for (Seg item : items) {
            PdfPTable row = new PdfPTable(new float[]{3.2f, 5.5f, 1.3f});
            row.setWidthPercentage(100);
            row.addCell(textCell(item.label(), kFont(7.5f, Font.NORMAL, C_INK2), Element.ALIGN_LEFT));
            PdfPCell barCell = new PdfPCell();
            barCell.setBorder(com.lowagie.text.Rectangle.NO_BORDER);
            barCell.setPadding(1.5f);
            barCell.addElement(barTable(List.of(
                    new Seg(item.label(), item.value(), C_SEQ),
                    new Seg("", Math.max(max - item.value(), 0), C_TRACK)), 7f));
            row.addCell(barCell);
            row.addCell(textCell(String.valueOf(item.value()), kFont(7.5f, Font.BOLD), Element.ALIGN_RIGHT));
            cell.addElement(row);
        }
        grid.addCell(cell);
    }

    /**
     * 가로 막대 — 값에 비례한 폭으로 조각을 이어 붙이고, 조각 사이는 흰 여백으로 띄운다.
     * 값이 모두 0이면 빈 트랙만 그린다.
     */
    private PdfPTable barTable(List<Seg> segs, float height) {
        List<Seg> visible = segs.stream().filter(x -> x.value() > 0).collect(Collectors.toList());
        if (visible.isEmpty()) {
            PdfPTable track = new PdfPTable(1);
            track.setWidthPercentage(100);
            track.addCell(barCell(C_TRACK, height, false));
            return track;
        }
        float[] widths = new float[visible.size()];
        for (int i = 0; i < visible.size(); i++) widths[i] = visible.get(i).value();
        PdfPTable bar = new PdfPTable(widths);
        bar.setWidthPercentage(100);
        for (int i = 0; i < visible.size(); i++) {
            bar.addCell(barCell(visible.get(i).color(), height, i < visible.size() - 1));
        }
        return bar;
    }

    private PdfPCell barCell(Color color, float height, boolean gapAfter) {
        PdfPCell cell = new PdfPCell(new Phrase(" "));
        cell.setFixedHeight(height);
        cell.setBackgroundColor(color);
        cell.setPadding(0);
        if (gapAfter) {
            cell.setBorder(com.lowagie.text.Rectangle.RIGHT);
            cell.setBorderWidthRight(1.5f);
            cell.setBorderColorRight(Color.WHITE);
        } else {
            cell.setBorder(com.lowagie.text.Rectangle.NO_BORDER);
        }
        return cell;
    }

    private PdfPCell textCell(String text, Font font, int align) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBorder(com.lowagie.text.Rectangle.NO_BORDER);
        cell.setHorizontalAlignment(align);
        cell.setPadding(1f);
        return cell;
    }

    /** 상세 수치 — 화면의 '표로 보기'와 같은 값을 다음 장에 부록으로 싣는다 */
    private void addPrivacyDetailTable(Document doc, String lang, PrivacyReportDto.Summary s) throws DocumentException {
        doc.newPage();
        doc.add(new Paragraph(t(lang, "상세 수치", "Detailed figures"), kFont(12, Font.BOLD)));
        doc.add(new Paragraph(" ", kFont(6, Font.NORMAL)));

        PdfPTable table = new PdfPTable(new float[]{3f, 5f, 2f});
        table.setWidthPercentage(100);
        addHeaderCell(table, t(lang, "영역", "Area"), kFont(8.5f, Font.BOLD, Color.WHITE));
        addHeaderCell(table, t(lang, "지표", "Metric"), kFont(8.5f, Font.BOLD, Color.WHITE));
        addHeaderCell(table, t(lang, "값", "Value"), kFont(8.5f, Font.BOLD, Color.WHITE));

        String[][] rows = {
                {t(lang, "개인정보 처리현황", "Processing"), t(lang, "전체", "Total"), String.valueOf(s.getProcessing().getTotal())},
                {"", t(lang, "운영중", "Active"), String.valueOf(s.getProcessing().getActive())},
                {"", t(lang, "중단", "Inactive"), String.valueOf(s.getProcessing().getInactive())},
                {t(lang, "개인정보파일", "Personal data files"), t(lang, "전체", "Total"), String.valueOf(s.getFiles().getTotal())},
                {"", t(lang, "운영중", "Active"), String.valueOf(s.getFiles().getActive())},
                {"", t(lang, "민감정보 포함", "Sensitive"), String.valueOf(s.getFiles().getSensitive())},
                {"", t(lang, "고유식별정보 포함", "Unique ID"), String.valueOf(s.getFiles().getUniqueIdentifier())},
                {t(lang, "수탁사", "Contractors"), t(lang, "전체", "Total"), String.valueOf(s.getContractors().getTotal())},
                {"", t(lang, "점검함", "Inspected"), String.valueOf(s.getContractors().getChecked())},
                {"", t(lang, "미점검", "Not inspected"), String.valueOf(s.getContractors().getUnchecked())},
                {t(lang, "제3자 제공", "Data provision"), t(lang, "전체", "Total"), String.valueOf(s.getProvisions().getTotal())},
                {"", t(lang, "제3자 제공", "Third party"), String.valueOf(s.getProvisions().getThirdParty())},
                {"", t(lang, "공동이용", "Joint use"), String.valueOf(s.getProvisions().getJointUse())},
                {"", t(lang, "국외이전", "Overseas"), String.valueOf(s.getProvisions().getOverseas())},
                {t(lang, "보유기간", "Retention"), t(lang, "전체", "Total"), String.valueOf(s.getRetentions().getTotal())},
                {"", t(lang, "30일 내 만료", "Expiring in 30d"), String.valueOf(s.getRetentions().getExpiringIn30Days())},
                {"", t(lang, "만료 경과", "Overdue"), String.valueOf(s.getRetentions().getOverdue())},
                {"", t(lang, "파기 완료", "Disposed"), String.valueOf(s.getRetentions().getDisposed())},
                {t(lang, "파기", "Disposal"), t(lang, "전체", "Total"), String.valueOf(s.getDisposals().getTotal())},
                {"", t(lang, "계획", "Planned"), String.valueOf(s.getDisposals().getPlanned())},
                {"", t(lang, "승인대기", "Pending approval"), String.valueOf(s.getDisposals().getPendingApproval())},
                {"", t(lang, "완료", "Completed"), String.valueOf(s.getDisposals().getCompleted())},
                {t(lang, "권리행사", "Rights"), t(lang, "전체", "Total"), String.valueOf(s.getRights().getTotal())},
                {"", t(lang, "처리중", "In progress"), String.valueOf(s.getRights().getInProgress())},
                {"", t(lang, "완료", "Completed"), String.valueOf(s.getRights().getCompleted())},
                {"", t(lang, "기한 초과", "SLA breached"), String.valueOf(s.getRights().getSlaBreached())},
                {t(lang, "유출사고", "Breaches"), t(lang, "전체", "Total"), String.valueOf(s.getBreaches().getTotal())},
                {"", t(lang, "미종결", "Open"), String.valueOf(s.getBreaches().getOpen())},
                {"", t(lang, "신고기한 경과", "Report overdue"), String.valueOf(s.getBreaches().getReportOverdue())},
                {"", t(lang, "유출 정보주체", "Affected subjects"), String.valueOf(s.getBreaches().getAffectedSubjects())},
                {t(lang, "법령 준수", "Compliance"), t(lang, "DPIA 전체", "DPIA total"), String.valueOf(s.getCompliance().getDpiaTotal())},
                {"", t(lang, "DPIA 완료", "DPIA completed"), String.valueOf(s.getCompliance().getDpiaCompleted())},
                {"", t(lang, "DPIA 위험 높음", "DPIA high risk"), String.valueOf(s.getCompliance().getDpiaHighRisk())},
                {"", t(lang, "안전조치 전체", "Safeguards"), String.valueOf(s.getCompliance().getSafeguardTotal())},
                {"", t(lang, "안전조치 완료", "Safeguards completed"), String.valueOf(s.getCompliance().getSafeguardCompleted())},
        };
        for (String[] row : rows) addDetailRow(table, row[0], row[1], row[2]);

        if (s.getRights().getByType() != null) {
            boolean first = true;
            for (Map.Entry<String, Long> e : s.getRights().getByType().entrySet()) {
                addDetailRow(table, first ? t(lang, "권리행사 유형", "Rights by type") : "",
                        tRightsType(lang, e.getKey()), String.valueOf(e.getValue()));
                first = false;
            }
        }
        if (s.getCompliance().getSafeguardByType() != null) {
            boolean first = true;
            for (var x : s.getCompliance().getSafeguardByType()) {
                addDetailRow(table, first ? t(lang, "안전조치 유형", "Safeguards by type") : "",
                        tSafeguardType(lang, x.getType()), String.valueOf(x.getCount()));
                first = false;
            }
        }
        doc.add(table);
    }

    private void addDetailRow(PdfPTable table, String area, String label, String value) {
        PdfPCell areaCell = new PdfPCell(new Phrase(area, kFont(8, Font.NORMAL, C_INK2)));
        PdfPCell labelCell = new PdfPCell(new Phrase(label, kFont(8, Font.NORMAL)));
        PdfPCell valueCell = new PdfPCell(new Phrase(value, kFont(8, Font.BOLD)));
        valueCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        for (PdfPCell c : List.of(areaCell, labelCell, valueCell)) {
            c.setPadding(4);
            c.setBorderColor(C_LINE);
            table.addCell(c);
        }
    }

    private String tSafeguardType(String lang, String type) {
        if (!"ko".equalsIgnoreCase(lang)) return type;
        return switch (type) {
            case "ACCESS_REVIEW" -> "접근권한";
            case "ACCESS_REVOKE" -> "권한회수";
            case "ENCRYPTION" -> "암호화";
            case "ACCESS_LOG_REVIEW" -> "접속기록";
            case "PRINTOUT" -> "출력물";
            case "EXPORT" -> "반출";
            case "DORMANT_ACCOUNT" -> "휴면계정";
            default -> type;
        };
    }

    private String tRightsType(String lang, String type) {
        if (!"ko".equalsIgnoreCase(lang)) return type;
        return switch (type) {
            case "ACCESS" -> "열람";
            case "CORRECTION" -> "정정";
            case "DELETION" -> "삭제";
            case "SUSPENSION" -> "처리정지";
            case "CONSENT_WITHDRAWAL" -> "동의철회";
            default -> type;
        };
    }

    private void addCenteredTitle(Document doc, String text, Font font) throws DocumentException {
        Paragraph p = new Paragraph(text, font);
        p.setAlignment(Element.ALIGN_CENTER);
        doc.add(p);
    }

    private void addCenteredSubtitle(Document doc, String text) throws DocumentException {
        Paragraph p = new Paragraph(text, kFont(10, Font.NORMAL));
        p.setAlignment(Element.ALIGN_CENTER);
        doc.add(p);
    }

    /** 설정관리 > 회사정보의 회사명을 제목 아래에 표시 (미등록 시 생략) */
    private void addCompanyLine(Document doc) throws DocumentException {
        String company = appSettingService.getValue("company.name");
        if (company != null && !company.isBlank()) {
            Paragraph p = new Paragraph(company.trim(), kFont(11, Font.BOLD));
            p.setAlignment(Element.ALIGN_CENTER);
            doc.add(p);
        }
    }

    private void addHeaderCell(PdfPTable table, String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBackgroundColor(new Color(37, 99, 235));
        cell.setPadding(6);
        table.addCell(cell);
    }

    private void addVulnStats(Document doc, List<Vulnerability> vulns, Font font, String lang) throws DocumentException {
        PdfPTable stats = new PdfPTable(5);
        stats.setWidthPercentage(60);
        String[] labels = {
                t(lang, "전체", "Total"),
                t(lang, "심각", "Critical"),
                t(lang, "높음", "High"),
                t(lang, "중간", "Medium"),
                t(lang, "낮음", "Low")
        };
        long[] values = {
                vulns.size(),
                vulns.stream().filter(v -> v.getSeverity() == Vulnerability.Severity.CRITICAL).count(),
                vulns.stream().filter(v -> v.getSeverity() == Vulnerability.Severity.HIGH).count(),
                vulns.stream().filter(v -> v.getSeverity() == Vulnerability.Severity.MEDIUM).count(),
                vulns.stream().filter(v -> v.getSeverity() == Vulnerability.Severity.LOW).count()
        };
        for (int i = 0; i < labels.length; i++) {
            PdfPCell cell = new PdfPCell();
            cell.addElement(new Phrase(String.valueOf(values[i]), kFont(16, Font.BOLD)));
            cell.addElement(new Phrase(labels[i], font));
            cell.setPadding(8);
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            stats.addCell(cell);
        }
        doc.add(stats);
    }
}
