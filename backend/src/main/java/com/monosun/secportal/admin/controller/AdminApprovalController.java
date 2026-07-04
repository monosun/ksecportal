package com.monosun.secportal.admin.controller;

import com.monosun.secportal.admin.service.UserAdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminApprovalController {

    private final UserAdminService userAdminService;

    @GetMapping(value = "/approve/{token}", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> approve(@PathVariable String token) {
        try {
            String message = userAdminService.approveAction(token);
            return ResponseEntity.ok(buildHtml("승인 완료", message, "#166534", "#dcfce7"));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .contentType(MediaType.TEXT_HTML)
                    .body(buildHtml("오류", e.getMessage(), "#991b1b", "#fee2e2"));
        }
    }

    @GetMapping(value = "/reject/{token}", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> reject(@PathVariable String token) {
        try {
            String message = userAdminService.rejectAction(token);
            return ResponseEntity.ok(buildHtml("거부 완료", message, "#7c3aed", "#ede9fe"));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .contentType(MediaType.TEXT_HTML)
                    .body(buildHtml("오류", e.getMessage(), "#991b1b", "#fee2e2"));
        }
    }

    private String buildHtml(String title, String message, String textColor, String bgColor) {
        return "<!DOCTYPE html><html lang='ko'><head><meta charset='UTF-8'>"
                + "<title>SecPortal - " + title + "</title>"
                + "<style>"
                + "body{font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',sans-serif;"
                + "display:flex;justify-content:center;align-items:center;"
                + "min-height:100vh;margin:0;background:#f9fafb}"
                + ".card{background:" + bgColor + ";color:" + textColor + ";"
                + "padding:48px 64px;border-radius:16px;text-align:center;max-width:520px;"
                + "box-shadow:0 4px 24px rgba(0,0,0,.08)}"
                + "h1{font-size:1.75rem;font-weight:700;margin:0 0 16px}"
                + "p{font-size:1rem;margin:0;line-height:1.6}"
                + ".badge{display:inline-block;margin-bottom:20px;font-size:0.75rem;"
                + "font-weight:600;letter-spacing:.05em;opacity:.7}"
                + "</style></head><body>"
                + "<div class='card'>"
                + "<div class='badge'>SecPortal 관리 시스템</div>"
                + "<h1>" + title + "</h1>"
                + "<p>" + message + "</p>"
                + "</div></body></html>";
    }
}
