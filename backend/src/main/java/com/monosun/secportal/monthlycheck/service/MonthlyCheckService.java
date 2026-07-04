package com.monosun.secportal.monthlycheck.service;

import com.monosun.secportal.auth.entity.User;
import com.monosun.secportal.auth.repository.UserRepository;
import com.monosun.secportal.common.exception.ResourceNotFoundException;
import com.monosun.secportal.common.service.FileStorageService;
import com.monosun.secportal.monthlycheck.dto.MonthlyCheckDto;
import com.monosun.secportal.monthlycheck.entity.MonthlyCheckEvidence;
import com.monosun.secportal.monthlycheck.entity.MonthlyCheckItem;
import com.monosun.secportal.monthlycheck.entity.MonthlyCheckDefault;
import com.monosun.secportal.monthlycheck.repository.MonthlyCheckDefaultRepository;
import com.monosun.secportal.monthlycheck.repository.MonthlyCheckEvidenceRepository;
import com.monosun.secportal.monthlycheck.repository.MonthlyCheckRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MonthlyCheckService {

    private final MonthlyCheckRepository repository;
    private final MonthlyCheckEvidenceRepository evidenceRepository;
    private final UserRepository userRepository;
    private final FileStorageService fileStorageService;
    private final MonthlyCheckDefaultRepository defaultRepository;

    // ── 항목 조회 ──────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<MonthlyCheckDto.CheckItemResponse> listByMonth(String yearMonth) {
        return repository.findByYearMonthOrderBySortOrderAscIdAsc(yearMonth)
                .stream()
                .map(MonthlyCheckDto.CheckItemResponse::from)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<String> listAvailableMonths() {
        return repository.findDistinctYearMonths();
    }

    @Transactional(readOnly = true)
    public MonthlyCheckDto.SummaryResponse summary(String yearMonth) {
        List<MonthlyCheckItem> items = repository.findByYearMonthOrderBySortOrderAscIdAsc(yearMonth);
        int completed = (int) items.stream().filter(i -> i.getResult() == MonthlyCheckItem.Result.COMPLETED).count();
        int incomplete = (int) items.stream().filter(i -> i.getResult() == MonthlyCheckItem.Result.INCOMPLETE).count();
        int na = (int) items.stream().filter(i -> i.getResult() == MonthlyCheckItem.Result.NA).count();
        return MonthlyCheckDto.SummaryResponse.builder()
                .total(items.size()).completed(completed).incomplete(incomplete).na(na)
                .build();
    }

    // ── 항목 CRUD ──────────────────────────────────────────────────────────

    @Transactional
    public MonthlyCheckDto.CheckItemResponse create(MonthlyCheckDto.CreateRequest req, User user) {
        User assignee = resolveUser(req.getAssigneeId());
        MonthlyCheckItem item = MonthlyCheckItem.builder()
                .yearMonth(req.getYearMonth())
                .priority(parsePriority(req.getPriority()))
                .category(req.getCategory())
                .itemName(req.getItemName())
                .checkMethod(req.getCheckMethod())
                .checkExample(req.getCheckExample())
                .result(req.getResult() != null ? parseResult(req.getResult()) : MonthlyCheckItem.Result.INCOMPLETE)
                .notes(req.getNotes())
                .sortOrder(req.getSortOrder())
                .assignee(assignee)
                .assigneeText(assignee == null ? req.getAssigneeText() : null)
                .createdBy(user)
                .build();
        return MonthlyCheckDto.CheckItemResponse.from(repository.save(item));
    }

    @Transactional
    public MonthlyCheckDto.CheckItemResponse update(Long id, MonthlyCheckDto.UpdateRequest req) {
        MonthlyCheckItem item = findItem(id);
        if (req.getPriority() != null) item.setPriority(parsePriority(req.getPriority()));
        if (req.getCategory() != null) item.setCategory(req.getCategory());
        if (req.getItemName() != null) item.setItemName(req.getItemName());
        if (req.getCheckMethod() != null) item.setCheckMethod(req.getCheckMethod());
        if (req.getCheckExample() != null) item.setCheckExample(req.getCheckExample());
        if (req.getResult() != null) item.setResult(parseResult(req.getResult()));
        if (req.getNotes() != null) item.setNotes(req.getNotes());
        if (req.getSortOrder() != null) item.setSortOrder(req.getSortOrder());
        if (req.isClearAssignee()) {
            item.setAssignee(null);
            item.setAssigneeText(null);
        } else if (req.getAssigneeId() != null) {
            User assignee = resolveUser(req.getAssigneeId());
            item.setAssignee(assignee);
            item.setAssigneeText(null);
        } else if (req.getAssigneeText() != null) {
            item.setAssignee(null);
            item.setAssigneeText(req.getAssigneeText());
        }
        return MonthlyCheckDto.CheckItemResponse.from(repository.save(item));
    }

    @Transactional
    public void delete(Long id) {
        MonthlyCheckItem item = findItem(id);
        for (MonthlyCheckEvidence e : item.getEvidences()) {
            deleteFile(e.getFilePath());
        }
        repository.delete(item);
    }

    @Transactional
    public void clearByYearMonth(String yearMonth) {
        List<MonthlyCheckItem> existing = repository.findByYearMonthOrderBySortOrderAscIdAsc(yearMonth);
        for (MonthlyCheckItem item : existing) {
            for (MonthlyCheckEvidence e : item.getEvidences()) {
                deleteFile(e.getFilePath());
            }
        }
        repository.deleteAll(existing);
    }

    @Transactional(readOnly = true)
    public boolean hasAllDefaults(String yearMonth) {
        List<MonthlyCheckItem> existing = repository.findByYearMonthOrderBySortOrderAscIdAsc(yearMonth);
        java.util.Set<String> existingNames = existing.stream()
                .map(MonthlyCheckItem::getItemName)
                .collect(java.util.stream.Collectors.toSet());
        List<MonthlyCheckDefault> defaults = defaultRepository.findByActiveOrderBySortOrderAsc(true);
        if (defaults.isEmpty()) {
            return DEFAULT_ITEMS.stream()
                    .map(DefaultItem::itemName)
                    .allMatch(existingNames::contains);
        }
        return defaults.stream()
                .map(MonthlyCheckDefault::getItemName)
                .allMatch(existingNames::contains);
    }

    @Transactional
    public List<MonthlyCheckDto.CheckItemResponse> loadDefaults(String yearMonth, User user) {
        List<MonthlyCheckItem> existing = repository.findByYearMonthOrderBySortOrderAscIdAsc(yearMonth);
        for (MonthlyCheckItem item : existing) {
            for (MonthlyCheckEvidence e : item.getEvidences()) {
                deleteFile(e.getFilePath());
            }
        }
        repository.deleteAll(existing);
        repository.flush();

        List<MonthlyCheckDefault> defaults = defaultRepository.findByActiveOrderBySortOrderAsc(true);
        List<MonthlyCheckItem> items;
        if (!defaults.isEmpty()) {
            items = defaults.stream().map(d ->
                    MonthlyCheckItem.builder()
                            .yearMonth(yearMonth)
                            .priority(d.getPriority())
                            .category(d.getCategory())
                            .itemName(d.getItemName())
                            .checkMethod(d.getCheckMethod())
                            .checkExample(d.getCheckExample())
                            .result(MonthlyCheckItem.Result.INCOMPLETE)
                            .sortOrder(d.getSortOrder())
                            .createdBy(user)
                            .build()
            ).collect(Collectors.toList());
        } else {
            // fallback: 하드코딩 상수 사용
            items = DEFAULT_ITEMS.stream().map(d ->
                    MonthlyCheckItem.builder()
                            .yearMonth(yearMonth)
                            .priority(d.priority())
                            .category(d.category())
                            .itemName(d.itemName())
                            .checkMethod(d.checkMethod())
                            .checkExample(d.checkExample())
                            .result(MonthlyCheckItem.Result.INCOMPLETE)
                            .sortOrder(d.sortOrder())
                            .createdBy(user)
                            .build()
            ).collect(Collectors.toList());
        }
        return repository.saveAll(items).stream()
                .map(MonthlyCheckDto.CheckItemResponse::from)
                .collect(Collectors.toList());
    }

    // ── 증적 CRUD ──────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<MonthlyCheckDto.EvidenceResponse> listEvidences(Long itemId) {
        findItem(itemId);
        return evidenceRepository.findByCheckItemIdOrderByCreatedAtDesc(itemId)
                .stream().map(MonthlyCheckDto.EvidenceResponse::from).collect(Collectors.toList());
    }

    @Transactional
    public MonthlyCheckDto.EvidenceResponse createEvidence(
            Long itemId, MonthlyCheckDto.EvidenceCreateRequest req,
            MultipartFile file, User user) throws IOException {
        MonthlyCheckItem item = findItem(itemId);
        String fileName = null;
        String filePath = null;
        if (file != null && !file.isEmpty()) {
            filePath = fileStorageService.store(file, "monthly-check");
            fileName = file.getOriginalFilename();
        }
        MonthlyCheckEvidence evidence = MonthlyCheckEvidence.builder()
                .checkItem(item)
                .title(req.getTitle())
                .content(req.getContent())
                .fileName(fileName)
                .filePath(filePath)
                .uploadedBy(user)
                .build();
        return MonthlyCheckDto.EvidenceResponse.from(evidenceRepository.save(evidence));
    }

    @Transactional
    public MonthlyCheckDto.EvidenceResponse updateEvidence(
            Long evidenceId, MonthlyCheckDto.EvidenceUpdateRequest req,
            MultipartFile file) throws IOException {
        MonthlyCheckEvidence evidence = findEvidence(evidenceId);
        if (req.getTitle() != null) evidence.setTitle(req.getTitle());
        if (req.getContent() != null) evidence.setContent(req.getContent());
        if (file != null && !file.isEmpty()) {
            deleteFile(evidence.getFilePath());
            evidence.setFilePath(fileStorageService.store(file, "monthly-check"));
            evidence.setFileName(file.getOriginalFilename());
        }
        return MonthlyCheckDto.EvidenceResponse.from(evidenceRepository.save(evidence));
    }

    @Transactional
    public void deleteEvidence(Long evidenceId) throws IOException {
        MonthlyCheckEvidence evidence = findEvidence(evidenceId);
        deleteFile(evidence.getFilePath());
        evidenceRepository.delete(evidence);
    }

    @Transactional(readOnly = true)
    public Resource downloadEvidenceFile(Long evidenceId) {
        MonthlyCheckEvidence evidence = findEvidence(evidenceId);
        if (evidence.getFilePath() == null) throw new ResourceNotFoundException("File", evidenceId);
        return fileStorageService.load(evidence.getFilePath());
    }

    @Transactional(readOnly = true)
    public MonthlyCheckEvidence getEvidence(Long evidenceId) {
        return findEvidence(evidenceId);
    }

    // ── 내부 헬퍼 ──────────────────────────────────────────────────────────

    private MonthlyCheckItem findItem(Long id) {
        return repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("MonthlyCheckItem", id));
    }

    private MonthlyCheckEvidence findEvidence(Long id) {
        return evidenceRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("MonthlyCheckEvidence", id));
    }

    private User resolveUser(Long id) {
        if (id == null) return null;
        return userRepository.findById(id).orElse(null);
    }

    private void deleteFile(String filePath) {
        if (filePath != null && !filePath.isBlank()) {
            try { fileStorageService.delete(filePath); } catch (IOException ignored) {}
        }
    }

    private MonthlyCheckItem.Priority parsePriority(String v) {
        return v != null ? MonthlyCheckItem.Priority.valueOf(v) : MonthlyCheckItem.Priority.HIGH;
    }

    private MonthlyCheckItem.Result parseResult(String v) {
        return MonthlyCheckItem.Result.valueOf(v);
    }

    // ── 기본 32개 항목 ─────────────────────────────────────────────────────

    private record DefaultItem(MonthlyCheckItem.Priority priority, String category, String itemName,
                               String checkMethod, String checkExample, int sortOrder) {}

    private static final List<DefaultItem> DEFAULT_ITEMS = List.of(
        new DefaultItem(MonthlyCheckItem.Priority.HIGH, "계정관리", "퇴직자 계정 삭제 여부",
            "인사시스템 퇴직자 명단과 시스템 계정 목록 비교", "퇴사자 A의 AD, VPN, AWS 계정 삭제 확인", 10),
        new DefaultItem(MonthlyCheckItem.Priority.HIGH, "계정관리", "관리자 권한 적정성",
            "관리자 권한 보유자 목록 검토", "일반 직원에게 Admin 권한 부여 여부 확인", 20),
        new DefaultItem(MonthlyCheckItem.Priority.HIGH, "계정관리", "MFA 적용 여부",
            "MFA 설정 현황 점검", "관리자 계정 MFA 적용 여부 확인", 30),
        new DefaultItem(MonthlyCheckItem.Priority.HIGH, "서버보안", "보안 패치 적용 여부",
            "OS 및 Middleware 패치 현황 점검", "Linux/Windows 최신 보안 패치 적용 확인", 40),
        new DefaultItem(MonthlyCheckItem.Priority.HIGH, "개인정보보호", "개인정보 저장 현황 점검",
            "파일 서버 및 DB 스캔", "주민등록번호, 계좌번호 저장 여부 확인", 50),
        new DefaultItem(MonthlyCheckItem.Priority.HIGH, "개인정보보호", "개인정보 반출 이력 점검",
            "DLP 로그 분석", "이메일, USB 등을 통한 대량 반출 여부 확인", 60),
        new DefaultItem(MonthlyCheckItem.Priority.HIGH, "백업관리", "정기 백업 수행 여부",
            "백업 로그 확인", "일일 백업 성공 여부 확인", 70),
        new DefaultItem(MonthlyCheckItem.Priority.HIGH, "백업관리", "복구 테스트 수행 여부",
            "복구 결과 검토", "월 1회 복구 테스트 수행 여부 확인", 80),
        new DefaultItem(MonthlyCheckItem.Priority.HIGH, "로그관리", "관리자 행위 로그 점검",
            "감사 로그 분석", "권한 변경 및 시스템 설정 변경 이력 확인", 90),
        new DefaultItem(MonthlyCheckItem.Priority.HIGH, "로그관리", "이상행위 탐지 여부",
            "SIEM 이벤트 분석", "비정상 로그인, 대량 조회 여부 확인", 100),
        new DefaultItem(MonthlyCheckItem.Priority.HIGH, "취약점관리", "서버 취약점 점검",
            "취약점 스캐너 수행", "OpenVAS, Nessus 결과 검토", 110),
        new DefaultItem(MonthlyCheckItem.Priority.HIGH, "취약점관리", "조치 미완료 취약점 확인",
            "취약점 조치 현황 검토", "High 등급 취약점 미조치 여부 확인", 120),
        new DefaultItem(MonthlyCheckItem.Priority.HIGH, "클라우드보안", "IAM 권한 최소화 여부",
            "권한 정책 검토", "AdministratorAccess 과다 부여 여부 확인", 130),
        new DefaultItem(MonthlyCheckItem.Priority.HIGH, "네트워크보안", "방화벽 정책 적정성",
            "허용 정책 검토", "Any-Any 허용 정책 존재 여부 확인", 140),
        new DefaultItem(MonthlyCheckItem.Priority.HIGH, "네트워크보안", "VPN 계정 관리",
            "VPN 사용자 목록 검토", "퇴사자 VPN 계정 삭제 여부 확인", 150),
        new DefaultItem(MonthlyCheckItem.Priority.MEDIUM, "계정관리", "휴면 계정 존재 여부",
            "최근 90일 이상 미사용 계정 조회", "장기 미사용 계정 잠금 확인", 160),
        new DefaultItem(MonthlyCheckItem.Priority.MEDIUM, "패스워드 관리", "패스워드 정책 준수 여부",
            "정책 설정값 확인", "10자 이상, 특수문자 포함 여부 확인", 170),
        new DefaultItem(MonthlyCheckItem.Priority.MEDIUM, "서버보안", "불필요 서비스 실행 여부",
            "서비스 목록 확인", "Telnet, FTP 사용 여부 확인", 180),
        new DefaultItem(MonthlyCheckItem.Priority.MEDIUM, "서버보안", "불필요 포트 개방 여부",
            "포트 스캔 수행", "21, 23 포트 오픈 여부 확인", 190),
        new DefaultItem(MonthlyCheckItem.Priority.MEDIUM, "개인정보보호", "개인정보 암호화 여부",
            "암호화 정책 확인", "고객정보 암호화 적용 여부 확인", 200),
        new DefaultItem(MonthlyCheckItem.Priority.MEDIUM, "로그관리", "로그인 실패 로그 점검",
            "인증 로그 분석", "계정 공격 징후 확인", 210),
        new DefaultItem(MonthlyCheckItem.Priority.MEDIUM, "클라우드보안", "Access Key 관리",
            "Key 사용 현황 점검", "90일 이상 미교체 Key 확인", 220),
        new DefaultItem(MonthlyCheckItem.Priority.MEDIUM, "클라우드보안", "감사 로그 활성화 여부",
            "CloudTrail/Audit Log 확인", "감사 로그 활성화 여부 확인", 230),
        new DefaultItem(MonthlyCheckItem.Priority.MEDIUM, "네트워크보안", "IDS/IPS 이벤트 확인",
            "탐지 로그 검토", "비정상 트래픽 탐지 여부 확인", 240),
        new DefaultItem(MonthlyCheckItem.Priority.LOW, "패스워드 관리", "계정 잠금 정책 적용 여부",
            "로그인 실패 정책 확인", "5회 실패 시 잠금 설정 확인", 250),
        new DefaultItem(MonthlyCheckItem.Priority.LOW, "개인정보보호", "공유폴더 접근권한 적정성",
            "권한 설정 검토", "공유폴더 권한 검토", 260),
        new DefaultItem(MonthlyCheckItem.Priority.LOW, "백업관리", "백업 데이터 암호화 여부",
            "백업 정책 확인", "백업본 암호화 적용 여부 확인", 270),
        new DefaultItem(MonthlyCheckItem.Priority.LOW, "취약점관리", "웹 취약점 점검",
            "웹 취약점 진단 수행", "XSS, SQL Injection 점검", 280),
        new DefaultItem(MonthlyCheckItem.Priority.LOW, "취약점관리", "오픈소스 취약점 점검",
            "SCA 도구 점검", "Log4j, Spring 취약점 확인", 290),
        new DefaultItem(MonthlyCheckItem.Priority.LOW, "보안운영", "보안 교육 이수 여부",
            "교육 현황 검토", "신규 입사자 교육 이수 확인", 300),
        new DefaultItem(MonthlyCheckItem.Priority.LOW, "보안운영", "개선조치 이행 여부",
            "조치 결과 검토", "전월 미흡사항 조치 완료 확인", 310),
        new DefaultItem(MonthlyCheckItem.Priority.LOW, "보안운영", "협력사 보안 점검 여부",
            "점검 결과 확인", "외주업체 보안 점검 수행 여부 확인", 320)
    );
}
