package com.monosun.secportal.report.controller;

import com.monosun.secportal.report.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@RestController
@RequestMapping("/reports")
@PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.BASIC_ISO_DATE;

    // ── PDF ─────────────────────────────────────────────────────────────────

    @GetMapping("/vulnerabilities/pdf")
    public ResponseEntity<byte[]> vulnerabilityPdf(@RequestParam(defaultValue = "ko") String lang) {
        String filename = "ko".equalsIgnoreCase(lang)
                ? "취약점-보고서-" + today() + ".pdf"
                : "vulnerability-report-" + today() + ".pdf";
        return pdfResponse(reportService.generateVulnerabilityReport(lang), filename);
    }

    @GetMapping("/privacy/pdf")
    public ResponseEntity<byte[]> privacyPdf(@RequestParam(defaultValue = "ko") String lang) {
        String filename = "ko".equalsIgnoreCase(lang)
                ? "개인정보-현황보고서-" + today() + ".pdf"
                : "privacy-status-report-" + today() + ".pdf";
        return pdfResponse(reportService.generatePrivacyReport(lang), filename);
    }

    @GetMapping("/policies/pdf")
    public ResponseEntity<byte[]> policyPdf(@RequestParam(defaultValue = "ko") String lang) {
        String filename = "ko".equalsIgnoreCase(lang)
                ? "보안정책-보고서-" + today() + ".pdf"
                : "security-policy-report-" + today() + ".pdf";
        return pdfResponse(reportService.generatePolicyReport(lang), filename);
    }

    @GetMapping("/assets/pdf")
    public ResponseEntity<byte[]> assetPdf(@RequestParam(defaultValue = "ko") String lang) {
        String filename = "ko".equalsIgnoreCase(lang)
                ? "자산관리-보고서-" + today() + ".pdf"
                : "asset-management-report-" + today() + ".pdf";
        return pdfResponse(reportService.generateAssetReport(lang), filename);
    }

    @GetMapping("/incidents/pdf")
    public ResponseEntity<byte[]> incidentPdf(@RequestParam(defaultValue = "ko") String lang) {
        String filename = "ko".equalsIgnoreCase(lang)
                ? "보안인시던트-보고서-" + today() + ".pdf"
                : "security-incident-report-" + today() + ".pdf";
        return pdfResponse(reportService.generateIncidentReport(lang), filename);
    }

    @GetMapping("/users/pdf")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<byte[]> userPdf(@RequestParam(defaultValue = "ko") String lang) {
        String filename = "ko".equalsIgnoreCase(lang)
                ? "사용자관리-보고서-" + today() + ".pdf"
                : "user-management-report-" + today() + ".pdf";
        return pdfResponse(reportService.generateUserReport(lang), filename);
    }

    @GetMapping("/isms/pdf")
    public ResponseEntity<byte[]> ismsPdf(
            @RequestParam(defaultValue = "0") int year,
            @RequestParam(defaultValue = "ko") String lang) {
        if (year == 0) year = LocalDate.now().getYear();
        String filename = "ko".equalsIgnoreCase(lang)
                ? "ISMS-P-증적관리-" + year + "-보고서-" + today() + ".pdf"
                : "isms-p-evidence-report-" + year + "-" + today() + ".pdf";
        return pdfResponse(reportService.generateIsmsReport(year, lang), filename);
    }

    @GetMapping("/training/pdf")
    public ResponseEntity<byte[]> trainingPdf(@RequestParam(defaultValue = "ko") String lang) {
        String filename = "ko".equalsIgnoreCase(lang)
                ? "교육이수-보고서-" + today() + ".pdf"
                : "training-report-" + today() + ".pdf";
        return pdfResponse(reportService.generateTrainingReport(lang), filename);
    }

    // ── CSV ─────────────────────────────────────────────────────────────────

    @GetMapping("/policies/csv")
    public ResponseEntity<byte[]> policyCsv(@RequestParam(defaultValue = "ko") String lang) {
        String filename = "ko".equalsIgnoreCase(lang)
                ? "보안정책-" + today() + ".csv"
                : "policies-" + today() + ".csv";
        return csvResponse(reportService.exportPolicyCsv(lang), filename);
    }

    @GetMapping("/vulnerabilities/csv")
    public ResponseEntity<byte[]> vulnerabilityCsv(@RequestParam(defaultValue = "ko") String lang) {
        String filename = "ko".equalsIgnoreCase(lang)
                ? "취약점관리-" + today() + ".csv"
                : "vulnerabilities-" + today() + ".csv";
        return csvResponse(reportService.exportVulnerabilityCsv(lang), filename);
    }

    @GetMapping("/assets/csv")
    public ResponseEntity<byte[]> assetCsv(@RequestParam(defaultValue = "ko") String lang) {
        String filename = "ko".equalsIgnoreCase(lang)
                ? "자산관리-" + today() + ".csv"
                : "assets-" + today() + ".csv";
        return csvResponse(reportService.exportAssetCsv(lang), filename);
    }

    @GetMapping("/incidents/csv")
    public ResponseEntity<byte[]> incidentCsv(@RequestParam(defaultValue = "ko") String lang) {
        String filename = "ko".equalsIgnoreCase(lang)
                ? "보안인시던트-" + today() + ".csv"
                : "incidents-" + today() + ".csv";
        return csvResponse(reportService.exportIncidentCsv(lang), filename);
    }

    @GetMapping("/users/csv")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<byte[]> userCsv(@RequestParam(defaultValue = "ko") String lang) {
        String filename = "ko".equalsIgnoreCase(lang)
                ? "사용자관리-" + today() + ".csv"
                : "users-" + today() + ".csv";
        return csvResponse(reportService.exportUserCsv(lang), filename);
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private ResponseEntity<byte[]> pdfResponse(byte[] data, String filename) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename*=UTF-8''" + encodeFilename(filename))
                .contentType(MediaType.APPLICATION_PDF)
                .body(data);
    }

    private ResponseEntity<byte[]> csvResponse(byte[] data, String filename) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename*=UTF-8''" + encodeFilename(filename))
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .body(data);
    }

    private String encodeFilename(String filename) {
        try {
            return java.net.URLEncoder.encode(filename, StandardCharsets.UTF_8).replace("+", "%20");
        } catch (Exception e) {
            return filename;
        }
    }

    private String today() { return LocalDate.now().format(DATE_FMT); }
}
