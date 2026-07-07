package com.monosun.secportal.asset.service;

import com.monosun.secportal.asset.dto.AssetDto;
import com.monosun.secportal.asset.entity.Asset;
import com.monosun.secportal.asset.repository.AssetRepository;
import com.monosun.secportal.audit.service.AuditLogService;
import com.monosun.secportal.common.exception.ResourceNotFoundException;
import com.monosun.secportal.sbom.entity.SbomSoftware;
import com.monosun.secportal.sbom.repository.SbomSoftwareRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AssetService {

    private final AssetRepository assetRepository;
    private final SbomSoftwareRepository sbomSoftwareRepository;
    private final AuditLogService auditLogService;

    @Transactional(readOnly = true)
    public Page<AssetDto.Response> list(String keyword, String type, Asset.Criticality criticality,
                                        Asset.CloudProvider cloudProvider, Asset.Environment environment,
                                        Boolean active, Asset.Status status, Pageable pageable) {
        return assetRepository.search(keyword, type, criticality, cloudProvider, environment, active, status, pageable)
                .map(AssetDto.Response::from);
    }

    @Transactional(readOnly = true)
    public AssetDto.Response get(Long id) {
        return AssetDto.Response.from(findById(id));
    }

    @Transactional
    public AssetDto.Response create(AssetDto.CreateRequest req) {
        Asset.Status status = req.getStatus() != null ? req.getStatus() : Asset.Status.OPERATIONAL;
        Asset asset = Asset.builder()
                .name(req.getName())
                .assetCategory(req.getAssetCategory())
                .type(req.getType())
                .description(req.getDescription())
                .owner(req.getOwner())
                .department(req.getDepartment())
                .operator(req.getOperator())
                .environment(req.getEnvironment() != null ? req.getEnvironment() : Asset.Environment.PRODUCTION)
                .location(req.getLocation())
                .status(status)
                .cloudProvider(req.getCloudProvider() != null ? req.getCloudProvider() : Asset.CloudProvider.ON_PREMISE)
                .cloudResourceId(req.getCloudResourceId())
                .region(req.getRegion())
                .ipAddress(req.getIpAddress())
                .osType(req.getOsType())
                .spec(req.getSpec())
                .criticality(req.getCriticality() != null ? req.getCriticality() : Asset.Criticality.MEDIUM)
                .confidentiality(req.getConfidentiality() != null ? req.getConfidentiality() : Asset.Criticality.MEDIUM)
                .integrity(req.getIntegrity() != null ? req.getIntegrity() : Asset.Criticality.MEDIUM)
                .availability(req.getAvailability() != null ? req.getAvailability() : Asset.Criticality.MEDIUM)
                .personalInfoIncluded(Boolean.TRUE.equals(req.getPersonalInfoIncluded()))
                .personalInfoType(req.getPersonalInfoType())
                .personalInfoProcessing(Boolean.TRUE.equals(req.getPersonalInfoProcessing()))
                .linkedSystems(req.getLinkedSystems())
                .sbomSoftware(resolveSbomSoftware(req.getSbomSoftwareId()))
                .accessControlTarget(Boolean.TRUE.equals(req.getAccessControlTarget()))
                .backupTarget(Boolean.TRUE.equals(req.getBackupTarget()))
                .logManagementTarget(Boolean.TRUE.equals(req.getLogManagementTarget()))
                .monthlyCost(req.getMonthlyCost())
                .contractExpiry(req.getContractExpiry())
                .lastInspectionDate(req.getLastInspectionDate())
                .nextInspectionDate(req.getNextInspectionDate())
                .lastReviewDate(req.getLastReviewDate())
                .remarks(req.getRemarks())
                .build();
        Asset saved = assetRepository.save(asset);
        auditLogService.log("ASSET_CREATED", "ASSET", saved.getId(), saved.getName());
        return AssetDto.Response.from(saved);
    }

    @Transactional
    public AssetDto.Response update(Long id, AssetDto.UpdateRequest req) {
        Asset asset = findById(id);
        if (req.getName() != null) asset.setName(req.getName());
        if (req.getAssetCategory() != null) asset.setAssetCategory(req.getAssetCategory());
        if (req.getType() != null) asset.setType(req.getType());
        if (req.getDescription() != null) asset.setDescription(req.getDescription());
        if (req.getOwner() != null) asset.setOwner(req.getOwner());
        if (req.getDepartment() != null) asset.setDepartment(req.getDepartment());
        if (req.getOperator() != null) asset.setOperator(req.getOperator());
        if (req.getEnvironment() != null) asset.setEnvironment(req.getEnvironment());
        if (req.getLocation() != null) asset.setLocation(req.getLocation());
        if (req.getStatus() != null) asset.setStatus(req.getStatus());
        if (req.getCloudProvider() != null) asset.setCloudProvider(req.getCloudProvider());
        if (req.getCloudResourceId() != null) asset.setCloudResourceId(req.getCloudResourceId());
        if (req.getRegion() != null) asset.setRegion(req.getRegion());
        if (req.getIpAddress() != null) asset.setIpAddress(req.getIpAddress());
        if (req.getOsType() != null) asset.setOsType(req.getOsType());
        if (req.getSpec() != null) asset.setSpec(req.getSpec());
        if (req.getCriticality() != null) asset.setCriticality(req.getCriticality());
        if (req.getConfidentiality() != null) asset.setConfidentiality(req.getConfidentiality());
        if (req.getIntegrity() != null) asset.setIntegrity(req.getIntegrity());
        if (req.getAvailability() != null) asset.setAvailability(req.getAvailability());
        if (req.getPersonalInfoIncluded() != null) asset.setPersonalInfoIncluded(req.getPersonalInfoIncluded());
        if (req.getPersonalInfoType() != null) asset.setPersonalInfoType(req.getPersonalInfoType());
        if (req.getPersonalInfoProcessing() != null) asset.setPersonalInfoProcessing(req.getPersonalInfoProcessing());
        if (req.getLinkedSystems() != null) asset.setLinkedSystems(req.getLinkedSystems());
        if (req.getSbomSoftwareId() != null) asset.setSbomSoftware(resolveSbomSoftware(req.getSbomSoftwareId()));
        if (req.getAccessControlTarget() != null) asset.setAccessControlTarget(req.getAccessControlTarget());
        if (req.getBackupTarget() != null) asset.setBackupTarget(req.getBackupTarget());
        if (req.getLogManagementTarget() != null) asset.setLogManagementTarget(req.getLogManagementTarget());
        if (req.getMonthlyCost() != null) asset.setMonthlyCost(req.getMonthlyCost());
        if (req.getContractExpiry() != null) asset.setContractExpiry(req.getContractExpiry());
        if (req.getLastInspectionDate() != null) asset.setLastInspectionDate(req.getLastInspectionDate());
        if (req.getNextInspectionDate() != null) asset.setNextInspectionDate(req.getNextInspectionDate());
        if (req.getLastReviewDate() != null) asset.setLastReviewDate(req.getLastReviewDate());
        if (req.getRemarks() != null) asset.setRemarks(req.getRemarks());
        if (req.getActive() != null) asset.setActive(req.getActive());
        auditLogService.log("ASSET_UPDATED", "ASSET", id, asset.getName());
        return AssetDto.Response.from(asset);
    }

    @Transactional
    public void delete(Long id) {
        Asset asset = findById(id);
        auditLogService.log("ASSET_DELETED", "ASSET", id, asset.getName());
        assetRepository.delete(asset);
    }

    // 0 이하 값은 매핑 해제
    private SbomSoftware resolveSbomSoftware(Long sbomSoftwareId) {
        if (sbomSoftwareId == null || sbomSoftwareId <= 0) return null;
        return sbomSoftwareRepository.findById(sbomSoftwareId)
                .orElseThrow(() -> new ResourceNotFoundException("SbomSoftware", sbomSoftwareId));
    }

    private Asset findById(Long id) {
        return assetRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Asset", id));
    }
}
