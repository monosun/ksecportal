package com.monosun.secportal.notification.service;

import com.monosun.secportal.vulnerability.entity.Vulnerability;
import com.monosun.secportal.vulnerability.repository.VulnerabilityRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationScheduler {

    private final VulnerabilityRepository vulnerabilityRepository;
    private final EmailService emailService;

    @Scheduled(cron = "${notification.overdue-check-cron}")
    public void notifyOverdueVulnerabilities() {
        LocalDate today = LocalDate.now();
        List<Vulnerability> overdue = vulnerabilityRepository.findOverdue(today);
        log.info("Overdue vulnerability check: {} items found", overdue.size());

        for (Vulnerability v : overdue) {
            if (v.getAssignee() != null && v.getAssignee().getEmail() != null) {
                String subject = "[SecPortal] 취약점 처리 기한 초과: " + v.getTitle();
                String body = buildOverdueEmailHtml(v);
                emailService.send(v.getAssignee().getEmail(), subject, body);
            }
        }
    }

    private String buildOverdueEmailHtml(Vulnerability v) {
        return """
                <html><body>
                <h2 style="color:#dc2626;">취약점 처리 기한이 초과되었습니다</h2>
                <table border="0" cellpadding="8">
                  <tr><td><b>제목</b></td><td>%s</td></tr>
                  <tr><td><b>심각도</b></td><td>%s</td></tr>
                  <tr><td><b>처리 기한</b></td><td>%s</td></tr>
                  <tr><td><b>현재 상태</b></td><td>%s</td></tr>
                </table>
                <p>SecPortal에서 즉시 처리해 주세요.</p>
                </body></html>
                """.formatted(v.getTitle(), v.getSeverity(), v.getDueDate(), v.getStatus());
    }
}
