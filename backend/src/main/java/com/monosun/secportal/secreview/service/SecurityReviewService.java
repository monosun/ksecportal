package com.monosun.secportal.secreview.service;

import com.monosun.secportal.audit.service.AuditLogService;
import com.monosun.secportal.auth.entity.User;
import com.monosun.secportal.common.exception.BusinessException;
import com.monosun.secportal.common.exception.ResourceNotFoundException;
import com.monosun.secportal.common.service.FileStorageService;
import com.monosun.secportal.secreview.dto.SecurityReviewDto;
import com.monosun.secportal.secreview.entity.SecurityReview;
import com.monosun.secportal.secreview.entity.SecurityReviewItem;
import com.monosun.secportal.secreview.repository.SecurityReviewItemRepository;
import com.monosun.secportal.secreview.repository.SecurityReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SecurityReviewService {

    private final SecurityReviewRepository reviewRepository;
    private final SecurityReviewItemRepository itemRepository;
    private final FileStorageService fileStorageService;
    private final AuditLogService auditLogService;

    /**
     * 기본 검토 체크리스트 — 신규 구축·변경 시 설계 단계에서 확인해야 하는 항목.
     * 심의를 만들 때 복사되며, 이후 심의별로 항목을 추가·삭제·수정할 수 있다.
     * {영역, 항목, 검토 기준}
     */
    private static final String[][] DEFAULT_ITEMS = {
            {"인증·권한", "사용자 인증 방식", "관리자·일반 사용자 인증 수단(SSO/MFA 등)과 세션 만료 정책이 정의되었는가"},
            {"인증·권한", "권한 설계(최소권한)", "역할별 권한이 최소권한 원칙으로 설계되고 관리자 기능이 분리되었는가"},
            {"인증·권한", "계정 관리 절차", "계정 생성·변경·삭제·휴면 처리 절차와 담당자가 정의되었는가"},
            {"접근통제", "네트워크 구간 분리", "DMZ·내부망 구간 배치와 방화벽 정책(출발지/목적지/포트)이 설계되었는가"},
            {"접근통제", "관리자 접근 통제", "운영 서버·DB 접근 경로가 통제(점프서버·VPN·IP 제한)되는가"},
            {"접근통제", "외부 연계 보안", "외부 시스템 연계 구간의 인증·암호화·연계 데이터 범위가 정의되었는가"},
            {"암호화", "전송구간 암호화", "HTTPS(TLS 1.2+) 적용 및 인증서 관리 방안이 있는가"},
            {"암호화", "저장 데이터 암호화", "비밀번호(일방향)·고유식별정보·계좌 등 저장 시 암호화 대상과 알고리즘이 정의되었는가"},
            {"암호화", "암호키 관리", "암호키 생성·보관·교체 방안이 정의되었는가(소스코드 하드코딩 금지)"},
            {"개인정보", "수집 항목 최소화", "처리하는 개인정보 항목·목적·보유기간이 정의되고 최소 수집 원칙을 지키는가"},
            {"개인정보", "동의·고지", "수집·이용 동의, 처리방침 반영 등 법적 고지 사항이 반영되었는가"},
            {"개인정보", "마스킹·출력 통제", "화면·출력물·다운로드 시 개인정보 마스킹 기준이 적용되었는가"},
            {"로그·감사", "접속기록 보관", "개인정보 처리·관리자 행위 로그를 남기고 보관기간(법정 기준)을 충족하는가"},
            {"로그·감사", "로그 위변조 방지", "로그 접근 통제·백업 등 위변조 방지 대책이 있는가"},
            {"개발보안", "시큐어 코딩", "OWASP Top 10 대응(입력 검증, SQL 인젝션·XSS 방지) 기준이 적용되는가"},
            {"개발보안", "취약점 점검 계획", "오픈 전 웹 취약점 진단·소스 점검(SAST) 계획이 있는가"},
            {"개발보안", "개발·운영 환경 분리", "개발/테스트/운영 환경이 분리되고 운영 데이터 사용이 통제되는가"},
            {"운영·보안설정", "보안 패치·설정", "OS·WAS·DB 보안 설정 기준과 패치 적용 방안이 있는가"},
            {"운영·보안설정", "백업·복구", "백업 대상·주기·보관 위치와 복구 절차가 정의되었는가"},
            {"운영·보안설정", "모니터링·사고 대응", "보안 이벤트 모니터링과 장애·침해 발생 시 대응 절차가 정의되었는가"},
    };

    // ── 심의 ────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Page<SecurityReviewDto.Response> list(String status, String reviewType, String keyword, Pageable pageable) {
        SecurityReview.Status st = parseEnum(SecurityReview.Status.class, status);
        SecurityReview.ReviewType rt = parseEnum(SecurityReview.ReviewType.class, reviewType);
        String kw = (keyword == null || keyword.isBlank()) ? null : keyword.trim();
        return reviewRepository.search(st, rt, kw, pageable).map(r -> SecurityReviewDto.Response.from(r, false));
    }

    @Transactional(readOnly = true)
    public SecurityReviewDto.Response get(Long id) {
        return SecurityReviewDto.Response.from(find(id), true);
    }

    @Transactional(readOnly = true)
    public SecurityReviewDto.Summary summary() {
        return SecurityReviewDto.Summary.builder()
                .requested(reviewRepository.countByStatus(SecurityReview.Status.REQUESTED))
                .inReview(reviewRepository.countByStatus(SecurityReview.Status.IN_REVIEW))
                .revision(reviewRepository.countByStatus(SecurityReview.Status.REVISION))
                .completed(reviewRepository.countByStatus(SecurityReview.Status.COMPLETED))
                .approved(reviewRepository.countByDecision(SecurityReview.Decision.APPROVED))
                .conditional(reviewRepository.countByDecision(SecurityReview.Decision.CONDITIONAL))
                .rejected(reviewRepository.countByDecision(SecurityReview.Decision.REJECTED))
                .build();
    }

    /** 심의 요청 등록 — 기본 검토 체크리스트를 함께 만들어 준다. */
    @Transactional
    public SecurityReviewDto.Response create(SecurityReviewDto.CreateRequest req, MultipartFile file, User user)
            throws IOException {
        SecurityReview review = SecurityReview.builder()
                .title(req.getTitle().trim())
                .systemName(req.getSystemName().trim())
                .reviewType(parseEnumOrDefault(SecurityReview.ReviewType.class, req.getReviewType(),
                        SecurityReview.ReviewType.NEW))
                .department(req.getDepartment())
                .requester(user)
                .description(req.getDescription())
                .handlesPersonalData(Boolean.TRUE.equals(req.getHandlesPersonalData()))
                .internetFacing(Boolean.TRUE.equals(req.getInternetFacing()))
                .targetDate(req.getTargetDate())
                .status(SecurityReview.Status.REQUESTED)
                .build();
        review = reviewRepository.save(review);

        if (file != null && !file.isEmpty()) {
            review.setFilePath(fileStorageService.store(file, "secreview/" + review.getId()));
            review.setFileName(file.getOriginalFilename());
        }

        List<SecurityReviewItem> items = new ArrayList<>();
        for (int i = 0; i < DEFAULT_ITEMS.length; i++) {
            String[] d = DEFAULT_ITEMS[i];
            items.add(SecurityReviewItem.builder()
                    .review(review).category(d[0]).itemName(d[1]).criteria(d[2])
                    .result(SecurityReviewItem.Result.PENDING).sortOrder(i + 1)
                    .build());
        }
        itemRepository.saveAll(items);
        review.getItems().addAll(items);

        auditLogService.log("SEC_REVIEW_CREATED", "SEC_REVIEW", review.getId(), review.getTitle());
        return SecurityReviewDto.Response.from(review, true);
    }

    @Transactional
    public SecurityReviewDto.Response update(Long id, SecurityReviewDto.UpdateRequest req) {
        SecurityReview r = find(id);
        if (req.getTitle() != null && !req.getTitle().isBlank()) r.setTitle(req.getTitle().trim());
        if (req.getSystemName() != null && !req.getSystemName().isBlank()) r.setSystemName(req.getSystemName().trim());
        if (req.getReviewType() != null) {
            r.setReviewType(parseEnumOrDefault(SecurityReview.ReviewType.class, req.getReviewType(), r.getReviewType()));
        }
        if (req.getDepartment() != null) r.setDepartment(req.getDepartment());
        if (req.getDescription() != null) r.setDescription(req.getDescription());
        if (req.getHandlesPersonalData() != null) r.setHandlesPersonalData(req.getHandlesPersonalData());
        if (req.getInternetFacing() != null) r.setInternetFacing(req.getInternetFacing());
        if (req.getTargetDate() != null) r.setTargetDate(req.getTargetDate());
        if (req.getStatus() != null) {
            SecurityReview.Status st = parseEnum(SecurityReview.Status.class, req.getStatus());
            if (st != null) {
                if (st == SecurityReview.Status.COMPLETED) {
                    throw new BusinessException("심의 완료는 '심의 결과 등록'으로 처리해 주세요.");
                }
                r.setStatus(st);
            }
        }
        return SecurityReviewDto.Response.from(r, true);
    }

    /** 심의 결과 확정 — 미검토 항목이 남아 있으면 막는다(형식적 승인 방지). */
    @Transactional
    public SecurityReviewDto.Response decide(Long id, SecurityReviewDto.DecisionRequest req, User user) {
        SecurityReview r = find(id);
        SecurityReview.Decision decision = parseEnum(SecurityReview.Decision.class, req.getDecision());
        if (decision == null) throw new BusinessException("심의 결과(승인/조건부승인/반려)를 선택해 주세요.");

        long pending = r.getItems().stream()
                .filter(i -> i.getResult() == SecurityReviewItem.Result.PENDING).count();
        if (pending > 0) {
            throw new BusinessException("미검토 항목이 " + pending + "건 남아 있습니다. 모든 항목을 검토한 뒤 결과를 등록해 주세요.");
        }

        r.setDecision(decision);
        r.setReviewComment(req.getReviewComment());
        r.setReviewer(user);
        r.setReviewedAt(LocalDateTime.now());
        r.setStatus(SecurityReview.Status.COMPLETED);

        auditLogService.log("SEC_REVIEW_DECIDED", "SEC_REVIEW", r.getId(),
                r.getTitle() + " → " + decision.name());
        return SecurityReviewDto.Response.from(r, true);
    }

    @Transactional
    public void delete(Long id) throws IOException {
        SecurityReview r = find(id);
        if (r.getFilePath() != null) fileStorageService.delete(r.getFilePath());
        auditLogService.log("SEC_REVIEW_DELETED", "SEC_REVIEW", id, r.getTitle());
        reviewRepository.delete(r);
    }

    // ── 첨부 ────────────────────────────────────────────────────────────────

    @Transactional
    public SecurityReviewDto.Response uploadFile(Long id, MultipartFile file) throws IOException {
        SecurityReview r = find(id);
        if (file == null || file.isEmpty()) throw new BusinessException("파일을 선택해 주세요.");
        if (r.getFilePath() != null) fileStorageService.delete(r.getFilePath());
        r.setFilePath(fileStorageService.store(file, "secreview/" + r.getId()));
        r.setFileName(file.getOriginalFilename());
        return SecurityReviewDto.Response.from(r, true);
    }

    @Transactional(readOnly = true)
    public Resource downloadFile(Long id) {
        SecurityReview r = find(id);
        if (r.getFilePath() == null) throw new ResourceNotFoundException("SecurityReview file", id);
        return fileStorageService.load(r.getFilePath());
    }

    @Transactional(readOnly = true)
    public String fileName(Long id) {
        SecurityReview r = find(id);
        return r.getFileName() != null ? r.getFileName() : "attachment";
    }

    // ── 검토 항목 ────────────────────────────────────────────────────────────

    @Transactional
    public SecurityReviewDto.ItemResponse updateItem(Long itemId, SecurityReviewDto.ItemRequest req) {
        SecurityReviewItem item = itemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("SecurityReviewItem", itemId));
        if (req.getCategory() != null && !req.getCategory().isBlank()) item.setCategory(req.getCategory().trim());
        if (req.getItemName() != null && !req.getItemName().isBlank()) item.setItemName(req.getItemName().trim());
        if (req.getCriteria() != null) item.setCriteria(req.getCriteria());
        if (req.getComment() != null) item.setComment(req.getComment());
        if (req.getResult() != null) {
            SecurityReviewItem.Result result = parseEnum(SecurityReviewItem.Result.class, req.getResult());
            if (result != null) item.setResult(result);
        }
        if (req.getSortOrder() != null) item.setSortOrder(req.getSortOrder());

        // 검토가 시작되면 상태를 자동으로 '검토중'으로 올린다(요청 접수 상태에 머무르지 않도록)
        SecurityReview review = item.getReview();
        if (review.getStatus() == SecurityReview.Status.REQUESTED
                && item.getResult() != SecurityReviewItem.Result.PENDING) {
            review.setStatus(SecurityReview.Status.IN_REVIEW);
        }
        return SecurityReviewDto.ItemResponse.from(item);
    }

    @Transactional
    public SecurityReviewDto.ItemResponse addItem(Long reviewId, SecurityReviewDto.ItemRequest req) {
        SecurityReview review = find(reviewId);
        if (req.getItemName() == null || req.getItemName().isBlank()) {
            throw new BusinessException("검토 항목명을 입력해 주세요.");
        }
        int nextOrder = review.getItems().stream().mapToInt(SecurityReviewItem::getSortOrder).max().orElse(0) + 1;
        SecurityReviewItem item = itemRepository.save(SecurityReviewItem.builder()
                .review(review)
                .category(req.getCategory() != null && !req.getCategory().isBlank() ? req.getCategory().trim() : "기타")
                .itemName(req.getItemName().trim())
                .criteria(req.getCriteria())
                .comment(req.getComment())
                .result(parseEnumOrDefault(SecurityReviewItem.Result.class, req.getResult(),
                        SecurityReviewItem.Result.PENDING))
                .sortOrder(req.getSortOrder() != null ? req.getSortOrder() : nextOrder)
                .build());
        review.getItems().add(item);
        return SecurityReviewDto.ItemResponse.from(item);
    }

    @Transactional
    public void deleteItem(Long itemId) {
        SecurityReviewItem item = itemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("SecurityReviewItem", itemId));
        item.getReview().getItems().remove(item);
        itemRepository.delete(item);
    }

    // ── 헬퍼 ────────────────────────────────────────────────────────────────

    private SecurityReview find(Long id) {
        return reviewRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("SecurityReview", id));
    }

    private <E extends Enum<E>> E parseEnum(Class<E> type, String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return Enum.valueOf(type, value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private <E extends Enum<E>> E parseEnumOrDefault(Class<E> type, String value, E defaultValue) {
        E parsed = parseEnum(type, value);
        return parsed != null ? parsed : defaultValue;
    }
}
