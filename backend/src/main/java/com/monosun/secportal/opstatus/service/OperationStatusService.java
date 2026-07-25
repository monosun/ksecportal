package com.monosun.secportal.opstatus.service;

import com.monosun.secportal.audit.service.AuditLogService;
import com.monosun.secportal.common.exception.BusinessException;
import com.monosun.secportal.common.exception.ResourceNotFoundException;
import com.monosun.secportal.opstatus.dto.OperationStatusDto;
import com.monosun.secportal.opstatus.entity.OperationStatusDefault;
import com.monosun.secportal.opstatus.entity.OperationStatusItem;
import com.monosun.secportal.opstatus.repository.OperationStatusDefaultRepository;
import com.monosun.secportal.opstatus.repository.OperationStatusItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OperationStatusService {

    private final OperationStatusItemRepository repository;
    private final OperationStatusDefaultRepository defaultRepository;
    private final AuditLogService auditLogService;

    // ── 조회 ────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<Integer> years() {
        return repository.findYears();
    }

    @Transactional(readOnly = true)
    public List<OperationStatusDto.ItemResponse> list(int year, String type) {
        List<OperationStatusItem> items = (type == null || type.isBlank())
                ? repository.findByYearOrderByTypeAscSortOrderAscIdAsc(year)
                : repository.findByYearAndTypeOrderBySortOrderAscIdAsc(year, parseType(type));
        return items.stream().map(OperationStatusDto.ItemResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public OperationStatusDto.SummaryResponse summary(int year) {
        List<OperationStatusItem> all = repository.findByYearOrderByTypeAscSortOrderAscIdAsc(year);
        List<OperationStatusDto.TypeSummary> byType = new ArrayList<>();
        int totalItems = 0, totalPlanned = 0, totalDone = 0;

        for (OperationStatusItem.Type t : OperationStatusItem.Type.values()) {
            List<OperationStatusItem> items = all.stream().filter(i -> i.getType() == t).toList();
            int planned = 0, done = 0, unplanned = 0;
            List<Integer> plannedByMonth = new ArrayList<>(java.util.Collections.nCopies(12, 0));
            List<Integer> doneByMonth = new ArrayList<>(java.util.Collections.nCopies(12, 0));

            for (OperationStatusItem i : items) {
                for (int m = 1; m <= 12; m++) {
                    boolean p = OperationStatusItem.has(i.getPlanMonths(), m);
                    boolean d = OperationStatusItem.has(i.getDoneMonths(), m);
                    if (p) {
                        planned++;
                        plannedByMonth.set(m - 1, plannedByMonth.get(m - 1) + 1);
                        if (d) done++;
                    } else if (d) {
                        unplanned++;
                    }
                    if (d) doneByMonth.set(m - 1, doneByMonth.get(m - 1) + 1);
                }
            }

            byType.add(OperationStatusDto.TypeSummary.builder()
                    .type(t.name())
                    .items(items.size())
                    .planned(planned)
                    .done(done)
                    .unplannedDone(unplanned)
                    .rate(planned > 0 ? Math.round(done * 100f / planned) : 0)
                    .plannedByMonth(plannedByMonth)
                    .doneByMonth(doneByMonth)
                    .build());

            totalItems += items.size();
            totalPlanned += planned;
            totalDone += done;
        }

        return OperationStatusDto.SummaryResponse.builder()
                .year(year)
                .byType(byType)
                .totalItems(totalItems)
                .totalPlanned(totalPlanned)
                .totalDone(totalDone)
                .rate(totalPlanned > 0 ? Math.round(totalDone * 100f / totalPlanned) : 0)
                .build();
    }

    // ── 등록·수정·삭제 ──────────────────────────────────────────────────────

    @Transactional
    public OperationStatusDto.ItemResponse create(OperationStatusDto.ItemRequest req) {
        if (req.getYear() == null) throw new BusinessException("연도를 입력해주세요.");
        if (req.getName() == null || req.getName().isBlank())
            throw new BusinessException("점검 항목명을 입력해주세요.");

        OperationStatusItem.Type type = parseType(req.getType());
        int nextOrder = repository.findByYearAndTypeOrderBySortOrderAscIdAsc(req.getYear(), type).stream()
                .mapToInt(i -> i.getSortOrder() == null ? 0 : i.getSortOrder())
                .max().orElse(0) + 1;

        OperationStatusItem item = OperationStatusItem.builder()
                .year(req.getYear())
                .type(type)
                .category(req.getCategory())
                .name(req.getName().trim())
                .cycle(req.getCycle())
                .deliverable(req.getDeliverable())
                .owner(req.getOwner())
                .manager(req.getManager())
                .note(req.getNote())
                .sortOrder(req.getSortOrder() != null ? req.getSortOrder() : nextOrder)
                .planMonths(toMask(req.getPlan()))
                .doneMonths(toMask(req.getDone()))
                .build();

        item = repository.save(item);
        auditLogService.log("CREATE", "OPERATION_STATUS", item.getId(),
                req.getYear() + "년 " + type.name() + " 운영현황 항목 등록: " + item.getName());
        return OperationStatusDto.ItemResponse.from(item);
    }

    @Transactional
    public OperationStatusDto.ItemResponse update(Long id, OperationStatusDto.ItemRequest req) {
        OperationStatusItem item = find(id);

        if (req.getCategory() != null) item.setCategory(req.getCategory());
        if (req.getName() != null && !req.getName().isBlank()) item.setName(req.getName().trim());
        if (req.getCycle() != null) item.setCycle(req.getCycle());
        if (req.getDeliverable() != null) item.setDeliverable(req.getDeliverable());
        if (req.getOwner() != null) item.setOwner(req.getOwner());
        if (req.getManager() != null) item.setManager(req.getManager());
        if (req.getNote() != null) item.setNote(req.getNote());
        if (req.getSortOrder() != null) item.setSortOrder(req.getSortOrder());
        if (req.getPlan() != null) item.setPlanMonths(toMask(req.getPlan()));
        if (req.getDone() != null) item.setDoneMonths(toMask(req.getDone()));

        auditLogService.log("UPDATE", "OPERATION_STATUS", id, "운영현황 항목 수정: " + item.getName());
        return OperationStatusDto.ItemResponse.from(item);
    }

    /** 월 칸 하나만 켜고 끄는 경로 — 표에서 클릭할 때마다 전체 항목을 보내지 않기 위해 둔다. */
    @Transactional
    public OperationStatusDto.ItemResponse toggleMonth(Long id, OperationStatusDto.MonthToggleRequest req) {
        OperationStatusItem item = find(id);
        int month = req.getMonth() == null ? 0 : req.getMonth();
        if (month < 1 || month > 12) throw new BusinessException("월은 1~12 사이여야 합니다.");

        boolean on = Boolean.TRUE.equals(req.getValue());
        String field = req.getField() == null ? "DONE" : req.getField().toUpperCase();

        switch (field) {
            case "PLAN" -> item.setPlanMonths(OperationStatusItem.set(item.getPlanMonths(), month, on));
            case "DONE" -> item.setDoneMonths(OperationStatusItem.set(item.getDoneMonths(), month, on));
            default -> throw new BusinessException("field 는 PLAN 또는 DONE 이어야 합니다: " + req.getField());
        }
        return OperationStatusDto.ItemResponse.from(item);
    }

    @Transactional
    public void delete(Long id) {
        OperationStatusItem item = find(id);
        auditLogService.log("DELETE", "OPERATION_STATUS", id, "운영현황 항목 삭제: " + item.getName());
        repository.delete(item);
    }

    // ── 연도 구성 ───────────────────────────────────────────────────────────

    /**
     * 기본 항목 불러오기 — 코드 관리에서 관리하는 기본 항목 마스터(사용 중인 것만)를 해당 연도로 복제한다.
     * 이미 해당 연도·유형에 항목이 있으면 중복 생성하지 않는다
     * (초기화가 필요하면 화면에서 유형 단위로 비운 뒤 다시 불러온다).
     */
    @Transactional
    public int loadDefaults(int year, String type) {
        OperationStatusItem.Type t = parseType(type);
        if (repository.existsByYearAndType(year, t))
            throw new BusinessException(year + "년 " + typeLabel(t) + " 항목이 이미 있습니다. 기존 항목을 삭제한 뒤 다시 불러오세요.");

        List<OperationStatusDefault> rows = defaultRepository.findByTypeAndActiveTrueOrderBySortOrderAscIdAsc(t);
        if (rows.isEmpty())
            throw new BusinessException(typeLabel(t) + " 기본 항목이 없습니다. 관리 > 코드 관리 > 운영현황 기본항목에서 먼저 등록하세요.");

        int order = 1;
        for (OperationStatusDefault r : rows) {
            repository.save(OperationStatusItem.builder()
                    .year(year).type(t)
                    .category(r.getCategory())
                    .name(r.getName())
                    .cycle(r.getCycle())
                    .deliverable(r.getDeliverable())
                    .owner(r.getOwner())
                    .manager(r.getManager())
                    .note(r.getNote())
                    .planMonths(r.getPlanMonths())
                    .doneMonths(0)
                    .sortOrder(order++)
                    .build());
        }
        auditLogService.log("CREATE", "OPERATION_STATUS", null,
                year + "년 " + typeLabel(t) + " 기본 항목 " + rows.size() + "건 불러오기");
        return rows.size();
    }

    /**
     * 전년도 구성 복사 — 항목·계획은 그대로 가져오고 이행 실적은 초기화한다.
     * 연도가 바뀌면 계획은 유지하되 실적은 새로 쌓아야 하기 때문이다.
     */
    @Transactional
    public int copyFromYear(int fromYear, int toYear, String type) {
        OperationStatusItem.Type t = parseType(type);
        if (fromYear == toYear) throw new BusinessException("같은 연도로는 복사할 수 없습니다.");
        if (repository.existsByYearAndType(toYear, t))
            throw new BusinessException(toYear + "년 " + typeLabel(t) + " 항목이 이미 있습니다. 기존 항목을 삭제한 뒤 복사하세요.");

        List<OperationStatusItem> source = repository.findByYearAndTypeOrderBySortOrderAscIdAsc(fromYear, t);
        if (source.isEmpty())
            throw new BusinessException(fromYear + "년 " + typeLabel(t) + " 항목이 없습니다.");

        for (OperationStatusItem s : source) {
            repository.save(OperationStatusItem.builder()
                    .year(toYear).type(t)
                    .category(s.getCategory()).name(s.getName()).cycle(s.getCycle())
                    .deliverable(s.getDeliverable()).owner(s.getOwner()).manager(s.getManager())
                    .note(s.getNote())
                    .planMonths(s.getPlanMonths())
                    .doneMonths(0)
                    .sortOrder(s.getSortOrder())
                    .build());
        }
        auditLogService.log("CREATE", "OPERATION_STATUS", null,
                fromYear + "년 → " + toYear + "년 " + typeLabel(t) + " 운영현황 " + source.size() + "건 복사");
        return source.size();
    }

    /** 유형 단위 전체 삭제 — 잘못 불러온 구성을 되돌리기 위한 경로 */
    @Transactional
    public int clear(int year, String type) {
        OperationStatusItem.Type t = parseType(type);
        List<OperationStatusItem> items = repository.findByYearAndTypeOrderBySortOrderAscIdAsc(year, t);
        repository.deleteAll(items);
        auditLogService.log("DELETE", "OPERATION_STATUS", null,
                year + "년 " + typeLabel(t) + " 운영현황 " + items.size() + "건 삭제");
        return items.size();
    }

    // ── 내부 ────────────────────────────────────────────────────────────────

    private OperationStatusItem find(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("OperationStatusItem", id));
    }

    private OperationStatusItem.Type parseType(String type) {
        if (type == null || type.isBlank()) return OperationStatusItem.Type.ISMS;
        try {
            return OperationStatusItem.Type.valueOf(type.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessException("알 수 없는 구분입니다: " + type);
        }
    }

    private String typeLabel(OperationStatusItem.Type t) {
        return t == OperationStatusItem.Type.PRIVACY ? "개인정보보호 관리체계" : "정보보호 관리체계";
    }

    private Integer toMask(List<Boolean> flags) {
        if (flags == null) return 0;
        int mask = 0;
        for (int i = 0; i < Math.min(12, flags.size()); i++) {
            if (Boolean.TRUE.equals(flags.get(i))) mask |= (1 << i);
        }
        return mask;
    }

    private String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s;
    }
}
