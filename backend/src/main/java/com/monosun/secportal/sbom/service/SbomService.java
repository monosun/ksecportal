package com.monosun.secportal.sbom.service;

import com.monosun.secportal.asset.repository.AssetRepository;
import com.monosun.secportal.audit.service.AuditLogService;
import com.monosun.secportal.common.exception.BusinessException;
import com.monosun.secportal.common.exception.ResourceNotFoundException;
import com.monosun.secportal.sbom.dto.SbomDto;
import com.monosun.secportal.sbom.entity.SbomComponent;
import com.monosun.secportal.sbom.entity.SbomSoftware;
import com.monosun.secportal.sbom.repository.SbomComponentRepository;
import com.monosun.secportal.sbom.repository.SbomSoftwareRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SbomService {

    private final SbomSoftwareRepository softwareRepository;
    private final SbomComponentRepository componentRepository;
    private final AssetRepository assetRepository;
    private final AuditLogService auditLogService;

    @Transactional(readOnly = true)
    public Page<SbomDto.Response> list(String keyword, Pageable pageable) {
        return softwareRepository.search(keyword, pageable).map(SbomDto.Response::from);
    }

    @Transactional(readOnly = true)
    public List<SbomDto.SimpleResponse> listAll() {
        return softwareRepository.findAllByOrderByNameAscVersionAsc().stream()
                .map(SbomDto.SimpleResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public SbomDto.DetailResponse get(Long id) {
        return SbomDto.DetailResponse.from(findById(id));
    }

    @Transactional
    public SbomDto.DetailResponse create(SbomDto.CreateRequest req) {
        String name = req.getName().trim();
        String version = req.getVersion().trim();
        if (softwareRepository.existsByNameAndVersion(name, version)) {
            throw new BusinessException("이미 등록된 SW입니다: " + name + " " + version);
        }
        SbomSoftware software = SbomSoftware.builder()
                .name(name)
                .version(version)
                .vendor(req.getVendor())
                .description(req.getDescription())
                .remarks(req.getRemarks())
                .build();
        SbomSoftware saved = softwareRepository.save(software);
        auditLogService.log("SBOM_SW_CREATED", "SBOM", saved.getId(), saved.getName() + " " + saved.getVersion());
        return SbomDto.DetailResponse.from(saved);
    }

    @Transactional
    public SbomDto.DetailResponse update(Long id, SbomDto.UpdateRequest req) {
        SbomSoftware software = findById(id);
        String name = req.getName() != null ? req.getName().trim() : software.getName();
        String version = req.getVersion() != null ? req.getVersion().trim() : software.getVersion();
        boolean identityChanged = !name.equals(software.getName()) || !version.equals(software.getVersion());
        if (identityChanged && softwareRepository.existsByNameAndVersion(name, version)) {
            throw new BusinessException("이미 등록된 SW입니다: " + name + " " + version);
        }
        software.setName(name);
        software.setVersion(version);
        if (req.getVendor() != null) software.setVendor(req.getVendor());
        if (req.getDescription() != null) software.setDescription(req.getDescription());
        if (req.getRemarks() != null) software.setRemarks(req.getRemarks());
        auditLogService.log("SBOM_SW_UPDATED", "SBOM", id, software.getName() + " " + software.getVersion());
        return SbomDto.DetailResponse.from(software);
    }

    @Transactional
    public void delete(Long id) {
        SbomSoftware software = findById(id);
        assetRepository.clearSbomSoftware(id);
        auditLogService.log("SBOM_SW_DELETED", "SBOM", id, software.getName() + " " + software.getVersion());
        softwareRepository.delete(software);
    }

    // ── 구성요소(라이브러리) ─────────────────────────────────────────────────

    @Transactional
    public SbomDto.ComponentResponse addComponent(Long softwareId, SbomDto.ComponentRequest req) {
        SbomSoftware software = findById(softwareId);
        SbomComponent component = SbomComponent.builder()
                .software(software)
                .libraryName(req.getLibraryName().trim())
                .libraryVersion(req.getLibraryVersion())
                .license(req.getLicense())
                .remarks(req.getRemarks())
                .build();
        SbomComponent saved = componentRepository.save(component);
        auditLogService.log("SBOM_COMPONENT_ADDED", "SBOM", softwareId,
                software.getName() + " ← " + saved.getLibraryName());
        return SbomDto.ComponentResponse.from(saved);
    }

    @Transactional
    public SbomDto.ComponentResponse updateComponent(Long componentId, SbomDto.ComponentRequest req) {
        SbomComponent component = componentRepository.findById(componentId)
                .orElseThrow(() -> new ResourceNotFoundException("SbomComponent", componentId));
        if (req.getLibraryName() != null) component.setLibraryName(req.getLibraryName().trim());
        component.setLibraryVersion(req.getLibraryVersion());
        component.setLicense(req.getLicense());
        component.setRemarks(req.getRemarks());
        return SbomDto.ComponentResponse.from(component);
    }

    @Transactional
    public void deleteComponent(Long componentId) {
        SbomComponent component = componentRepository.findById(componentId)
                .orElseThrow(() -> new ResourceNotFoundException("SbomComponent", componentId));
        auditLogService.log("SBOM_COMPONENT_DELETED", "SBOM", component.getSoftware().getId(),
                component.getLibraryName());
        componentRepository.delete(component);
    }

    private SbomSoftware findById(Long id) {
        return softwareRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("SbomSoftware", id));
    }
}
