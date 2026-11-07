package com.monosun.secportal.admin.controller;

import com.monosun.secportal.admin.service.UserAdminService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.HtmlUtils;

/**
 * 관리자 승인 메일의 승인/거부 링크 처리 (로그인 없이 토큰으로 접근).
 *
 * <p>응답 HTML 에 넣는 값은 모두 이스케이프한다 — 결과 메시지에는 사용자가 정한 이름·이메일이 들어가므로
 * 이스케이프하지 않으면 저장형 XSS 가 된다. 예외 메시지는 화면에 내보내지 않고(CWE-209) 서버 로그에만 남긴다.
 */
@Slf4j
@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminApprovalController {

    private static final String INVALID_TOKEN_MESSAGE = "유효하지 않거나 이미 처리된 링크입니다.";
    private static final String GENERIC_ERROR_MESSAGE = "요청을 처리하지 못했습니다. 관리자에게 문의하세요.";

    private final UserAdminService userAdminService;

    @GetMapping(value = "/approve/{token}", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> approve(@PathVariable String token) {
        try {
            String message = userAdminService.approveAction(token);
            return ResponseEntity.ok(buildHtml("승인 완료", message, "#166534", "#dcfce7"));
        } catch (Exception e) {
            return errorPage("승인", e);
        }
    }

    @GetMapping(value = "/reject/{token}", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> reject(@PathVariable String token) {
        try {
            String message = userAdminService.rejectAction(token);
            return ResponseEntity.ok(buildHtml("거부 완료", message, "#7c3aed", "#ede9fe"));
        } catch (Exception e) {
            return errorPage("거부", e);
        }
    }

    /** 예외 내용은 로그로만 남기고, 화면에는 정해진 문구만 보여준다 */
    private ResponseEntity<String> errorPage(String action, Exception e) {
        String message;
        if (e instanceof IllegalArgumentException) {
            // 서비스가 토큰을 찾지 못하면 IllegalArgumentException 을 던진다
            message = INVALID_TOKEN_MESSAGE;
            log.info("관리자 {} 링크 처리 거절: {}", action, e.getMessage());
        } else {
            message = GENERIC_ERROR_MESSAGE;
            log.warn("관리자 {} 링크 처리 실패", action, e);
        }
        return ResponseEntity.badRequest()
                .contentType(MediaType.TEXT_HTML)
                .body(buildHtml("오류", message, "#991b1b", "#fee2e2"));
    }

    private String buildHtml(String title, String message, String textColor, String bgColor) {
        String safeTitle = HtmlUtils.htmlEscape(title);
        String safeMessage = HtmlUtils.htmlEscape(message == null ? "" : message);
        return "<!DOCTYPE html><html lang='ko'><head><meta charset='UTF-8'>"
                + "<title>SecPortal - " + safeTitle + "</title>"
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
                + "<h1>" + safeTitle + "</h1>"
                + "<p>" + safeMessage + "</p>"
                + "</div></body></html>";
    }
}
