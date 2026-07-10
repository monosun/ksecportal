package com.monosun.secportal.asset.service;

import com.monosun.secportal.asset.dto.AssetSnapshotDto;
import com.monosun.secportal.asset.entity.Asset;
import com.monosun.secportal.asset.entity.AssetSnapshot;
import com.monosun.secportal.asset.entity.AssetSnapshotItem;
import com.monosun.secportal.asset.repository.AssetRepository;
import com.monosun.secportal.asset.repository.AssetSnapshotItemRepository;
import com.monosun.secportal.asset.repository.AssetSnapshotRepository;
import com.monosun.secportal.audit.service.AuditLogService;
import com.monosun.secportal.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AssetSnapshotService {

    private final AssetRepository assetRepository;
    private final AssetSnapshotRepository snapshotRepository;
    private final AssetSnapshotItemRepository itemRepository;
    private final AuditLogService auditLogService;

    @Transactional(readOnly = true)
    public List<AssetSnapshotDto.SnapshotResponse> list() {
        return snapshotRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(AssetSnapshotDto.SnapshotResponse::from)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<AssetSnapshotDto.ItemResponse> items(Long snapshotId) {
        if (!snapshotRepository.existsById(snapshotId)) {
            throw new ResourceNotFoundException("AssetSnapshot", snapshotId);
        }
        return itemRepository.findBySnapshotIdOrderByIdAsc(snapshotId).stream()
                .map(AssetSnapshotDto.ItemResponse::from)
                .collect(Collectors.toList());
    }

    /** 현재 자산 목록 전체를 하나의 시점(스냅샷)으로 저장한다. */
    @Transactional
    public AssetSnapshotDto.SnapshotResponse create(String title, String memo, String createdBy) {
        List<Asset> assets = assetRepository.findAll();

        AssetSnapshot snapshot = snapshotRepository.save(AssetSnapshot.builder()
                .title(title != null && !title.isBlank() ? title.trim() : null)
                .memo(memo != null && !memo.isBlank() ? memo.trim() : null)
                .assetCount(assets.size())
                .createdBy(createdBy)
                .build());

        List<AssetSnapshotItem> items = new ArrayList<>(assets.size());
        for (Asset a : assets) {
            items.add(AssetSnapshotItem.builder()
                    .snapshotId(snapshot.getId())
                    .assetId(a.getId())
                    .name(a.getName())
                    .assetCategory(a.getAssetCategory() != null ? a.getAssetCategory().name() : null)
                    .type(a.getType())
                    .owner(a.getOwner())
                    .department(a.getDepartment())
                    .operator(a.getOperator())
                    .environment(a.getEnvironment() != null ? a.getEnvironment().name() : null)
                    .location(a.getLocation())
                    .status(a.getStatus() != null ? a.getStatus().name() : null)
                    .cloudProvider(a.getCloudProvider() != null ? a.getCloudProvider().name() : null)
                    .ipAddress(a.getIpAddress())
                    .region(a.getRegion())
                    .cloudResourceId(a.getCloudResourceId())
                    .osType(a.getOsType())
                    .criticality(a.getCriticality() != null ? a.getCriticality().name() : null)
                    .personalInfoIncluded(a.isPersonalInfoIncluded())
                    .monthlyCost(a.getMonthlyCost())
                    .build());
        }
        itemRepository.saveAll(items);

        auditLogService.log("ASSET_SNAPSHOT_CREATED", "ASSET_SNAPSHOT", snapshot.getId(),
                "count=" + assets.size() + (snapshot.getTitle() != null ? ", title=" + snapshot.getTitle() : ""));
        return AssetSnapshotDto.SnapshotResponse.from(snapshot);
    }

    @Transactional
    public void delete(Long id) {
        if (!snapshotRepository.existsById(id)) {
            throw new ResourceNotFoundException("AssetSnapshot", id);
        }
        itemRepository.deleteBySnapshotId(id);
        snapshotRepository.deleteById(id);
        auditLogService.log("ASSET_SNAPSHOT_DELETED", "ASSET_SNAPSHOT", id, null);
    }
}
