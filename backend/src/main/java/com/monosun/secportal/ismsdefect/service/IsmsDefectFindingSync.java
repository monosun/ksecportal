package com.monosun.secportal.ismsdefect.service;

import com.monosun.secportal.ismsdefect.entity.IsmsDefect;
import com.monosun.secportal.secfinding.entity.SecurityFinding;
import com.monosun.secportal.secfinding.repository.SecurityFindingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * ISMS 결함 → 보안 운영 &gt; 보안 결함사항 자동 반영.
 *
 * <p>보안 결함사항 화면은 <b>ISMS-P 심사 결함 + 그 외 직접 등록한 결함</b>을 한 곳에서 보는 화면이다.
 * ISMS 결함관리에 결함을 등록·수정·삭제하면 이 클래스가 대응하는 결함사항 행을
 * {@code source_type = ISMS_DEFECT} 로 만들어 두고 계속 같은 상태로 맞춘다.
 *
 * <p>단방향이다 — 보안 결함사항 화면에서는 가져온 건을 고칠 수 없다(고쳐도 다음 동기화에 덮어써지므로).
 * 첨부파일은 <b>경로만 공유</b>하고 실물을 복제하지 않으므로, 가져온 행을 지울 때 파일을 지우면 안 된다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IsmsDefectFindingSync {

    private final SecurityFindingRepository findingRepository;

    /** 결함 1건을 결함사항으로 반영한다(없으면 생성, 있으면 갱신). */
    @Transactional
    public void sync(IsmsDefect d) {
        SecurityFinding f = findingRepository.findBySourceDefectId(d.getId())
                .orElseGet(() -> SecurityFinding.builder()
                        .sourceType(SecurityFinding.SourceType.ISMS_DEFECT)
                        .sourceDefectId(d.getId())
                        .build());

        f.setSourceType(SecurityFinding.SourceType.ISMS_DEFECT);
        f.setSourceDefectId(d.getId());
        f.setSourceLabel(sourceLabel(d));
        f.setYear(d.getYear());
        f.setAuditType(auditType(d.getAuditType()));
        f.setDomain(d.getDomainName());
        f.setRequirementCode(d.getItemCode());
        f.setRequirementName(d.getItemName());
        f.setFindingSummary(summary(d));
        f.setFindingDetail(detail(d));
        f.setRiskLevel(riskLevel(d.getSeverity()));
        f.setCorrectiveAction(correctiveAction(d));
        f.setActionDeadline(d.getDueDate());
        f.setStatus(status(d.getStatus()));
        f.setResolvedAt(d.getCompletedDate());
        f.setResolver(resolver(d));
        // 첨부는 실물 복제 없이 같은 파일을 가리킨다 (원본이 지워지면 함께 사라진다)
        f.setFileName(d.getFileName());
        f.setFilePath(d.getFilePath());
        f.setFileSize(d.getFileSize());
        if (f.getCreatedBy() == null) f.setCreatedBy(d.getRegistrant());

        findingRepository.save(f);
    }

    /** 결함이 지워지면 대응하는 결함사항도 지운다. 첨부파일은 원본 소유이므로 건드리지 않는다. */
    @Transactional
    public void remove(Long defectId) {
        findingRepository.findBySourceDefectId(defectId).ifPresent(findingRepository::delete);
    }

    /**
     * 아직 결함사항으로 만들어지지 않은 결함을 한 번에 반영한다.
     * 기동 시 보충용 — 기능이 추가되기 전에 등록된 결함과, 동기화가 어긋난 경우를 스스로 복구한다.
     *
     * @return 새로 만든 건수
     */
    @Transactional
    public int syncMissing(List<IsmsDefect> defects) {
        List<Long> existing = findingRepository.findAllSourceDefectIds();
        List<IsmsDefect> missing = new ArrayList<>();
        for (IsmsDefect d : defects) {
            if (!existing.contains(d.getId())) missing.add(d);
        }
        missing.forEach(this::sync);
        return missing.size();
    }

    // ── 필드 변환 ───────────────────────────────────────────────────────────

    private static String sourceLabel(IsmsDefect d) {
        String label = "ISMS-P " + auditTypeLabel(d.getAuditType());
        if (d.getDefectNo() != null && !d.getDefectNo().isBlank()) label += " · " + d.getDefectNo().trim();
        return label.length() > 100 ? label.substring(0, 100) : label;
    }

    private static String summary(IsmsDefect d) {
        String title = d.getTitle() != null ? d.getTitle().trim() : "(제목 없음)";
        return title.length() > 500 ? title.substring(0, 500) : title;
    }

    /** 결함 내용 + 원인. 결함 구분(결함/권고/개선)은 본문 맨 앞에 남겨 구분이 사라지지 않게 한다. */
    private static String detail(IsmsDefect d) {
        StringBuilder sb = new StringBuilder();
        sb.append("[").append(defectTypeLabel(d.getDefectType())).append("]");
        if (d.getDefectNo() != null && !d.getDefectNo().isBlank()) sb.append(" ").append(d.getDefectNo().trim());
        sb.append("\n\n");
        if (hasText(d.getContent())) sb.append(d.getContent().trim()).append("\n\n");
        if (hasText(d.getCause())) sb.append("[결함 원인]\n").append(d.getCause().trim()).append("\n\n");
        return sb.toString().trim();
    }

    /** 조치 계획 + 조치 내용 + 재발방지 대책 */
    private static String correctiveAction(IsmsDefect d) {
        StringBuilder sb = new StringBuilder();
        if (hasText(d.getActionPlan())) sb.append("[조치 계획]\n").append(d.getActionPlan().trim()).append("\n\n");
        if (hasText(d.getActionResult())) sb.append("[조치 내용]\n").append(d.getActionResult().trim()).append("\n\n");
        if (hasText(d.getPreventionPlan())) sb.append("[재발방지 대책]\n").append(d.getPreventionPlan().trim());
        String s = sb.toString().trim();
        return s.isEmpty() ? null : s;
    }

    private static String resolver(IsmsDefect d) {
        String dept = d.getDepartment();
        String who = d.getAssignee();
        if (hasText(dept) && hasText(who)) return dept.trim() + " / " + who.trim();
        if (hasText(who)) return who.trim();
        return hasText(dept) ? dept.trim() : null;
    }

    private static SecurityFinding.AuditType auditType(IsmsDefect.AuditType t) {
        return switch (t) {
            case INTERNAL -> SecurityFinding.AuditType.INTERNAL;
            case OTHER -> SecurityFinding.AuditType.OTHER;
            // 최초·사후·갱신심사는 모두 ISMS-P 인증심사다
            default -> SecurityFinding.AuditType.ISMS_P;
        };
    }

    private static SecurityFinding.RiskLevel riskLevel(IsmsDefect.Severity s) {
        return switch (s) {
            case CRITICAL -> SecurityFinding.RiskLevel.CRITICAL;
            case HIGH -> SecurityFinding.RiskLevel.HIGH;
            case LOW -> SecurityFinding.RiskLevel.LOW;
            default -> SecurityFinding.RiskLevel.MEDIUM;
        };
    }

    private static SecurityFinding.Status status(IsmsDefect.Status s) {
        return switch (s) {
            case IN_PROGRESS -> SecurityFinding.Status.IN_PROGRESS;
            case COMPLETED -> SecurityFinding.Status.RESOLVED;
            case HOLD -> SecurityFinding.Status.ACCEPTED;
            default -> SecurityFinding.Status.OPEN;
        };
    }

    private static String auditTypeLabel(IsmsDefect.AuditType t) {
        return switch (t) {
            case INITIAL -> "최초심사";
            case FOLLOWUP -> "사후심사";
            case RENEWAL -> "갱신심사";
            case INTERNAL -> "내부심사";
            case OTHER -> "기타";
        };
    }

    private static String defectTypeLabel(IsmsDefect.DefectType t) {
        return switch (t) {
            case DEFECT -> "결함";
            case RECOMMENDATION -> "권고";
            case IMPROVEMENT -> "개선사항";
        };
    }

    private static boolean hasText(String s) {
        return s != null && !s.isBlank();
    }
}
