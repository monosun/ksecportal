package com.monosun.secportal.threat.service;

import com.monosun.secportal.common.exception.BusinessException;
import com.monosun.secportal.common.exception.ResourceNotFoundException;
import com.monosun.secportal.threat.dto.ThreatDto;
import com.monosun.secportal.threat.entity.Threat;
import com.monosun.secportal.threat.entity.ThreatDefault;
import com.monosun.secportal.threat.repository.ThreatDefaultRepository;
import com.monosun.secportal.threat.repository.ThreatRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ThreatService {

    private final ThreatRepository threatRepository;
    private final ThreatDefaultRepository threatDefaultRepository;

    private static final int DEFAULT_COUNT = 560;

    @Transactional(readOnly = true)
    public List<ThreatDto.ThreatResponse> list() {
        return threatRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(ThreatDto.ThreatResponse::from)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ThreatDto.ThreatResponse get(Long id) {
        Threat threat = threatRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Threat", id));
        return ThreatDto.ThreatResponse.from(threat);
    }

    @Transactional
    public ThreatDto.ThreatResponse create(ThreatDto.ThreatRequest req) {
        Threat threat = Threat.builder()
                .name(req.getName())
                .type(req.getType())
                .category(req.getCategory())
                .assetDetail(req.getAssetDetail())
                .description(req.getDescription())
                .likelihood(req.getLikelihood())
                .impact(req.getImpact())
                .remark(req.getRemark())
                .build();
        return ThreatDto.ThreatResponse.from(threatRepository.save(threat));
    }

    @Transactional
    public ThreatDto.ThreatResponse update(Long id, ThreatDto.ThreatRequest req) {
        Threat threat = threatRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Threat", id));
        if (req.getName() != null) threat.setName(req.getName());
        if (req.getType() != null) threat.setType(req.getType());
        if (req.getCategory() != null) threat.setCategory(req.getCategory());
        if (req.getAssetDetail() != null) threat.setAssetDetail(req.getAssetDetail());
        if (req.getDescription() != null) threat.setDescription(req.getDescription());
        threat.setLikelihood(req.getLikelihood());
        threat.setImpact(req.getImpact());
        if (req.getRemark() != null) threat.setRemark(req.getRemark());
        return ThreatDto.ThreatResponse.from(threatRepository.save(threat));
    }

    @Transactional
    public void delete(Long id) {
        if (!threatRepository.existsById(id)) {
            throw new ResourceNotFoundException("Threat", id);
        }
        threatRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public ThreatDto.DefaultCheckResponse checkDefaults() {
        List<ThreatDefault> defaults = threatDefaultRepository.findAllByOrderByIdAsc();
        Set<String> existingNames = threatRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(Threat::getName)
                .collect(Collectors.toSet());
        boolean alreadyLoaded = defaults.stream()
                .map(ThreatDefault::getName)
                .allMatch(existingNames::contains);
        int existingCount = (int) threatRepository.count();
        return ThreatDto.DefaultCheckResponse.builder()
                .alreadyLoaded(alreadyLoaded)
                .existingCount(existingCount)
                .defaultCount(DEFAULT_COUNT)
                .build();
    }

    @Transactional
    public int loadDefaults() {
        List<ThreatDefault> defaults = threatDefaultRepository.findAllByOrderByIdAsc();
        int loaded = 0;
        List<Threat> toSave = new ArrayList<>();
        for (ThreatDefault d : defaults) {
            if (!threatRepository.existsByName(d.getName())) {
                toSave.add(Threat.builder()
                        .name(d.getName())
                        .type(d.getType())
                        .category(d.getCategory())
                        .assetDetail(d.getAssetDetail())
                        .description(d.getDescription())
                        .likelihood(d.getLikelihood())
                        .impact(d.getImpact())
                        .build());
                loaded++;
            }
        }
        if (!toSave.isEmpty()) {
            threatRepository.saveAll(toSave);
        }
        return loaded;
    }

    // ── ThreatDefault CRUD ─────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public ThreatDto.DefaultListResponse listDefaults(int page, int size) {
        Page<ThreatDefault> result = threatDefaultRepository.findAll(
                PageRequest.of(page, size, Sort.by("id").ascending()));
        List<ThreatDto.DefaultResponse> items = result.getContent().stream()
                .map(ThreatDto.DefaultResponse::from)
                .collect(Collectors.toList());
        return ThreatDto.DefaultListResponse.builder()
                .total(result.getTotalElements())
                .page(page)
                .size(size)
                .items(items)
                .build();
    }

    @Transactional
    public ThreatDto.DefaultResponse createDefault(ThreatDto.DefaultRequest req) {
        String type = req.getType() != null ? req.getType().trim() : "";
        String category = req.getCategory() != null ? req.getCategory().trim() : "";
        if (threatDefaultRepository.existsByNameAndTypeAndCategory(req.getName(), type, category)) {
            throw new BusinessException("동일한 위협명, 유형, 카테고리 조합이 이미 존재합니다.");
        }
        ThreatDefault d = ThreatDefault.builder()
                .riskId(req.getRiskId() != null ? req.getRiskId() : "")
                .name(req.getName())
                .type(type)
                .category(category)
                .assetDetail(req.getAssetDetail())
                .description(req.getDescription())
                .likelihood(req.getLikelihood())
                .impact(req.getImpact())
                .build();
        return ThreatDto.DefaultResponse.from(threatDefaultRepository.save(d));
    }

    @Transactional
    public ThreatDto.DefaultResponse updateDefault(Long id, ThreatDto.DefaultRequest req) {
        ThreatDefault d = threatDefaultRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ThreatDefault", id));
        String newName = req.getName() != null ? req.getName() : d.getName();
        String newType = req.getType() != null ? req.getType().trim() : d.getType();
        String newCat  = req.getCategory() != null ? req.getCategory().trim() : d.getCategory();
        // 자기 자신 제외하고 중복 확인
        if (threatDefaultRepository.findByNameAndTypeAndCategory(newName, newType, newCat)
                .filter(existing -> !existing.getId().equals(id)).isPresent()) {
            throw new BusinessException("동일한 위협명, 유형, 카테고리 조합이 이미 존재합니다.");
        }
        if (req.getRiskId() != null) d.setRiskId(req.getRiskId());
        d.setName(newName);
        d.setType(newType);
        d.setCategory(newCat);
        if (req.getAssetDetail() != null) d.setAssetDetail(req.getAssetDetail());
        if (req.getDescription() != null) d.setDescription(req.getDescription());
        d.setLikelihood(req.getLikelihood());
        d.setImpact(req.getImpact());
        return ThreatDto.DefaultResponse.from(threatDefaultRepository.save(d));
    }

    @Transactional
    public void deleteDefault(Long id) {
        if (!threatDefaultRepository.existsById(id)) {
            throw new ResourceNotFoundException("ThreatDefault", id);
        }
        threatDefaultRepository.deleteById(id);
    }

    @Transactional
    public int resetToDefaults() {
        threatRepository.deleteAllInBatch();
        List<ThreatDefault> defaults = threatDefaultRepository.findAllByOrderByIdAsc();
        List<Threat> toSave = defaults.stream()
                .map(d -> Threat.builder()
                        .name(d.getName())
                        .type(d.getType())
                        .category(d.getCategory())
                        .assetDetail(d.getAssetDetail())
                        .description(d.getDescription())
                        .likelihood(d.getLikelihood())
                        .impact(d.getImpact())
                        .build())
                .collect(Collectors.toList());
        threatRepository.saveAll(toSave);
        return toSave.size();
    }
}
