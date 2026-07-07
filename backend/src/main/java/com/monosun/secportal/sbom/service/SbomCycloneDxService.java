package com.monosun.secportal.sbom.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.monosun.secportal.audit.service.AuditLogService;
import com.monosun.secportal.common.exception.BusinessException;
import com.monosun.secportal.common.exception.ResourceNotFoundException;
import com.monosun.secportal.sbom.dto.SbomBulkUploadResult;
import com.monosun.secportal.sbom.entity.SbomComponent;
import com.monosun.secportal.sbom.entity.SbomSoftware;
import com.monosun.secportal.sbom.repository.SbomComponentRepository;
import com.monosun.secportal.sbom.repository.SbomSoftwareRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * CycloneDX 1.5 JSON 형식의 SBOM 내보내기/가져오기.
 * https://cyclonedx.org/docs/1.5/json/
 */
@Service
@RequiredArgsConstructor
public class SbomCycloneDxService {

    private static final String SPEC_VERSION = "1.5";
    // SPDX 라이선스 ID 형태 (예: Apache-2.0, GPL-2.0-only) — 아니면 name으로 출력
    private static final Pattern SPDX_ID_PATTERN = Pattern.compile("^[A-Za-z0-9.+\\-]+$");

    private final SbomSoftwareRepository softwareRepository;
    private final SbomComponentRepository componentRepository;
    private final AuditLogService auditLogService;
    private final ObjectMapper objectMapper;

    // ── 내보내기 ──────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public byte[] export(Long softwareId) throws IOException {
        SbomSoftware software = softwareRepository.findById(softwareId)
                .orElseThrow(() -> new ResourceNotFoundException("SbomSoftware", softwareId));

        ObjectNode bom = objectMapper.createObjectNode();
        bom.put("bomFormat", "CycloneDX");
        bom.put("specVersion", SPEC_VERSION);
        bom.put("serialNumber", "urn:uuid:" + UUID.randomUUID());
        bom.put("version", 1);

        ObjectNode metadata = bom.putObject("metadata");
        metadata.put("timestamp",
                OffsetDateTime.now(ZoneOffset.UTC).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
        ObjectNode rootComponent = metadata.putObject("component");
        rootComponent.put("type", "application");
        rootComponent.put("name", software.getName());
        rootComponent.put("version", software.getVersion());
        if (software.getDescription() != null && !software.getDescription().isBlank()) {
            rootComponent.put("description", software.getDescription());
        }
        if (software.getVendor() != null && !software.getVendor().isBlank()) {
            rootComponent.putObject("supplier").put("name", software.getVendor());
        }

        ArrayNode components = bom.putArray("components");
        for (SbomComponent c : software.getComponents()) {
            ObjectNode node = components.addObject();
            node.put("type", c.getComponentType() != null ? c.getComponentType() : "library");
            if (c.getGroupName() != null && !c.getGroupName().isBlank()) node.put("group", c.getGroupName());
            node.put("name", c.getLibraryName());
            if (c.getLibraryVersion() != null && !c.getLibraryVersion().isBlank()) node.put("version", c.getLibraryVersion());
            if (c.getPurl() != null && !c.getPurl().isBlank()) node.put("purl", c.getPurl());
            if (c.getLicense() != null && !c.getLicense().isBlank()) {
                ObjectNode license = node.putArray("licenses").addObject().putObject("license");
                if (SPDX_ID_PATTERN.matcher(c.getLicense()).matches()) {
                    license.put("id", c.getLicense());
                } else {
                    license.put("name", c.getLicense());
                }
            }
            if (c.getRemarks() != null && !c.getRemarks().isBlank()) node.put("description", c.getRemarks());
        }

        return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(bom);
    }

    // ── 가져오기 ──────────────────────────────────────────────────────────────

    @Transactional
    public SbomBulkUploadResult importBom(MultipartFile file) throws IOException {
        JsonNode bom;
        try {
            bom = objectMapper.readTree(new String(file.getBytes(), StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new BusinessException("JSON 파싱에 실패했습니다: " + e.getMessage());
        }

        if (!"CycloneDX".equals(bom.path("bomFormat").asText())) {
            throw new BusinessException("CycloneDX 형식이 아닙니다 (bomFormat 필드 확인)");
        }

        // metadata.component → SW 정보
        JsonNode meta = bom.path("metadata").path("component");
        String swName = meta.path("name").asText(null);
        String swVersion = meta.path("version").asText(null);
        if (swName == null || swName.isBlank()) {
            throw new BusinessException("metadata.component.name이 없습니다 — 대상 SW를 식별할 수 없습니다");
        }
        if (swVersion == null || swVersion.isBlank()) swVersion = "unknown";

        int newSoftwareCount = 0;
        SbomSoftware software = softwareRepository.findByNameAndVersion(swName, swVersion).orElse(null);
        if (software == null) {
            software = softwareRepository.save(SbomSoftware.builder()
                    .name(swName)
                    .version(swVersion)
                    .vendor(meta.path("supplier").path("name").asText(null))
                    .description(meta.path("description").asText(null))
                    .build());
            newSoftwareCount++;
        }

        List<SbomBulkUploadResult.RowError> errors = new ArrayList<>();
        int total = 0, success = 0;

        JsonNode components = bom.path("components");
        if (components.isArray()) {
            int index = 0;
            for (JsonNode node : components) {
                index++;
                total++;
                try {
                    String libName = node.path("name").asText(null);
                    if (libName == null || libName.isBlank()) {
                        throw new IllegalArgumentException("component.name이 없습니다");
                    }
                    String libVersion = blankToNull(node.path("version").asText(null));

                    SbomComponent component = componentRepository
                            .findBySoftwareIdAndLibraryNameAndLibraryVersion(software.getId(), libName, libVersion)
                            .orElse(null);
                    if (component == null) {
                        component = SbomComponent.builder()
                                .software(software)
                                .libraryName(libName)
                                .libraryVersion(libVersion)
                                .build();
                    }
                    component.setComponentType(SbomService.normalizeComponentType(node.path("type").asText(null)));
                    component.setGroupName(blankToNull(node.path("group").asText(null)));
                    component.setPurl(blankToNull(node.path("purl").asText(null)));
                    component.setLicense(parseLicense(node.path("licenses")));
                    componentRepository.save(component);
                    success++;
                } catch (Exception e) {
                    errors.add(new SbomBulkUploadResult.RowError(index, e.getMessage()));
                }
            }
        }

        auditLogService.log("SBOM_CDX_IMPORT", "SBOM", software.getId(),
                swName + " " + swVersion + " — 컴포넌트 " + success + "건 가져오기 (CycloneDX)");
        return new SbomBulkUploadResult(total, success, total - success, newSoftwareCount, errors);
    }

    // licenses: [{license: {id|name}}] 또는 [{expression: "..."}]
    private String parseLicense(JsonNode licenses) {
        if (!licenses.isArray() || licenses.isEmpty()) return null;
        JsonNode first = licenses.get(0);
        String id = first.path("license").path("id").asText(null);
        if (id != null && !id.isBlank()) return id;
        String name = first.path("license").path("name").asText(null);
        if (name != null && !name.isBlank()) return name;
        return blankToNull(first.path("expression").asText(null));
    }

    private String blankToNull(String v) {
        return (v == null || v.isBlank()) ? null : v;
    }
}
