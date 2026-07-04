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
