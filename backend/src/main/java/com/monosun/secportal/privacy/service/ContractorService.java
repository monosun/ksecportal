package com.monosun.secportal.privacy.service;

import com.monosun.secportal.auth.entity.User;
import com.monosun.secportal.common.exception.BusinessException;
import com.monosun.secportal.common.exception.ResourceNotFoundException;
import com.monosun.secportal.common.service.FileStorageService;
import com.monosun.secportal.privacy.dto.ContractorDto;
import com.monosun.secportal.privacy.entity.Contractor;
import com.monosun.secportal.privacy.entity.ContractorInspection;
import com.monosun.secportal.privacy.entity.ContractorInspectionFile;
import com.monosun.secportal.privacy.repository.ContractorCheckRepository;
import com.monosun.secportal.privacy.repository.ContractorCheckResultRepository;
import com.monosun.secportal.privacy.repository.ContractorInspectionFileRepository;
import com.monosun.secportal.privacy.repository.ContractorInspectionRepository;
import com.monosun.secportal.privacy.repository.ContractorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ContractorService {

    private final ContractorRepository contractorRepo;
    private final ContractorInspectionRepository inspectionRepo;
    private final ContractorInspectionFileRepository fileRepo;
    private final FileStorageService fileStorageService;
    private final ContractorCheckResultRepository checkResultRepo;
    private final ContractorCheckRepository checkRepo;

    // ── Contractor CRUD ───────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<ContractorDto.ContractorResponse> listAll() {
        java.util.Map<Long, Long> checkCounts = checkRepo.countGroupByContractor().stream()
                .collect(java.util.stream.Collectors.toMap(r -> (Long) r[0], r -> (Long) r[1]));
        return contractorRepo.findAllWithInspections().stream()
                .map(c -> ContractorDto.ContractorResponse.from(c, checkCounts.getOrDefault(c.getId(), 0L)))
                .toList();
    }

    @Transactional(readOnly = true)
    public ContractorDto.ContractorResponse getDetail(Long id) {
        Contractor c = contractorRepo.findByIdWithInspections(id)
                .orElseThrow(() -> new ResourceNotFoundException("Contractor", id));
        return ContractorDto.ContractorResponse.from(c);
    }

    @Transactional
    public ContractorDto.ContractorResponse create(ContractorDto.ContractorRequest req) {
        Contractor c = Contractor.builder()
                .name(req.getName().trim())
                .businessNumber(req.getBusinessNumber())
                .representative(req.getRepresentative())
                .serviceType(req.getServiceType())
                .contractStart(req.getContractStart())
                .contractEnd(req.getContractEnd())
                .contactPerson(req.getContactPerson())
                .contactEmail(req.getContactEmail())
                .contactPhone(req.getContactPhone())
                .status(parseStatus(req.getStatus()))
                .notes(req.getNotes())
                .build();
        return ContractorDto.ContractorResponse.fromWithoutInspections(contractorRepo.save(c));
    }

    @Transactional
    public ContractorDto.ContractorResponse update(Long id, ContractorDto.ContractorRequest req) {
        Contractor c = contractorRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Contractor", id));
        if (req.getName() != null && !req.getName().isBlank()) c.setName(req.getName().trim());
        if (req.getBusinessNumber() != null) c.setBusinessNumber(req.getBusinessNumber());
        if (req.getRepresentative() != null) c.setRepresentative(req.getRepresentative());
        if (req.getServiceType() != null) c.setServiceType(req.getServiceType());
        if (req.getContractStart() != null) c.setContractStart(req.getContractStart());
        if (req.getContractEnd() != null) c.setContractEnd(req.getContractEnd());
        if (req.getContactPerson() != null) c.setContactPerson(req.getContactPerson());
        if (req.getContactEmail() != null) c.setContactEmail(req.getContactEmail());
        if (req.getContactPhone() != null) c.setContactPhone(req.getContactPhone());
        if (req.getStatus() != null) c.setStatus(parseStatus(req.getStatus()));
        if (req.getNotes() != null) c.setNotes(req.getNotes());
        return ContractorDto.ContractorResponse.fromWithoutInspections(c);
    }

    @Transactional
    public void delete(Long id) throws IOException {
        Contractor c = contractorRepo.findByIdWithInspections(id)
                .orElseThrow(() -> new ResourceNotFoundException("Contractor", id));
        // contractor_check_results → contractor_checks 순서로 삭제 (FK 제약)
        checkResultRepo.deleteByContractorId(id);
        checkRepo.deleteByContractorId(id);
        // 첨부파일 물리 삭제 (lazy load - @Transactional 내에서 가능)
        for (ContractorInspection insp : c.getInspections()) {
            for (ContractorInspectionFile f : insp.getFiles()) {
                if (f.getFilePath() != null) fileStorageService.delete(f.getFilePath());
            }
        }
        contractorRepo.delete(c);
    }

    // ── Inspection CRUD ───────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<ContractorDto.InspectionResponse> listInspections(Long contractorId) {
        return inspectionRepo.findByContractorIdWithFiles(contractorId).stream()
                .map(ContractorDto.InspectionResponse::from).toList();
    }

    @Transactional
    public ContractorDto.InspectionResponse createInspection(Long contractorId, ContractorDto.InspectionRequest req) {
        Contractor c = contractorRepo.findById(contractorId)
                .orElseThrow(() -> new ResourceNotFoundException("Contractor", contractorId));
        if (req.getInspectionDate() == null) throw new BusinessException("점검일을 입력하세요.");
        ContractorInspection insp = ContractorInspection.builder()
                .contractor(c)
                .inspectionDate(req.getInspectionDate())
                .inspector(req.getInspector())
                .status(parseInspectionStatus(req.getStatus()))
                .result(req.getResult())
                .notes(req.getNotes())
                .build();
        return ContractorDto.InspectionResponse.from(inspectionRepo.save(insp));
    }

    @Transactional
    public ContractorDto.InspectionResponse updateInspection(Long contractorId, Long inspectionId, ContractorDto.InspectionRequest req) {
        ContractorInspection insp = inspectionRepo.findByIdWithFiles(inspectionId)
                .orElseThrow(() -> new ResourceNotFoundException("ContractorInspection", inspectionId));
        if (!insp.getContractor().getId().equals(contractorId)) throw new BusinessException("수탁사 정보가 일치하지 않습니다.");
        if (req.getInspectionDate() != null) insp.setInspectionDate(req.getInspectionDate());
        if (req.getInspector() != null) insp.setInspector(req.getInspector());
        if (req.getStatus() != null) insp.setStatus(parseInspectionStatus(req.getStatus()));
        if (req.getResult() != null) insp.setResult(req.getResult());
        if (req.getNotes() != null) insp.setNotes(req.getNotes());
        return ContractorDto.InspectionResponse.from(insp);
    }

    @Transactional
    public void deleteInspection(Long contractorId, Long inspectionId) throws IOException {
        ContractorInspection insp = inspectionRepo.findByIdWithFiles(inspectionId)
                .orElseThrow(() -> new ResourceNotFoundException("ContractorInspection", inspectionId));
        if (!insp.getContractor().getId().equals(contractorId)) throw new BusinessException("수탁사 정보가 일치하지 않습니다.");
        for (ContractorInspectionFile f : insp.getFiles()) {
            if (f.getFilePath() != null) fileStorageService.delete(f.getFilePath());
        }
        inspectionRepo.delete(insp);
    }

    // ── Inspection File ───────────────────────────────────────────────

    @Transactional
    public ContractorDto.FileResponse addFile(Long inspectionId, String title, MultipartFile file, User uploader) throws IOException {
        ContractorInspection insp = inspectionRepo.findById(inspectionId)
                .orElseThrow(() -> new ResourceNotFoundException("ContractorInspection", inspectionId));
        if (title == null || title.isBlank()) throw new BusinessException("파일 제목을 입력하세요.");
        ContractorInspectionFile f = ContractorInspectionFile.builder()
                .inspection(insp)
                .title(title.trim())
                .uploader(uploader)
                .build();
        f = fileRepo.save(f);
        if (file != null && !file.isEmpty()) {
            String path = fileStorageService.store(file, "contractor/" + f.getId());
            f.setFilePath(path);
            f.setFileName(file.getOriginalFilename());
            f.setFileSize(file.getSize());
        }
        return ContractorDto.FileResponse.from(f);
    }

    @Transactional
    public void deleteFile(Long fileId) throws IOException {
        ContractorInspectionFile f = fileRepo.findById(fileId)
                .orElseThrow(() -> new ResourceNotFoundException("ContractorInspectionFile", fileId));
        if (f.getFilePath() != null) fileStorageService.delete(f.getFilePath());
        fileRepo.delete(f);
    }

    public ContractorInspectionFile getFile(Long fileId) {
        return fileRepo.findById(fileId)
                .orElseThrow(() -> new ResourceNotFoundException("ContractorInspectionFile", fileId));
    }

    public org.springframework.core.io.Resource downloadFile(Long fileId) {
        ContractorInspectionFile f = getFile(fileId);
        if (f.getFilePath() == null) throw new BusinessException("첨부파일이 없습니다.");
        return fileStorageService.load(f.getFilePath());
    }

    // ── Helpers ───────────────────────────────────────────────────────

    private Contractor.Status parseStatus(String s) {
        if (s == null) return Contractor.Status.ACTIVE;
        try { return Contractor.Status.valueOf(s.toUpperCase()); }
        catch (IllegalArgumentException e) { return Contractor.Status.ACTIVE; }
    }

    private ContractorInspection.Status parseInspectionStatus(String s) {
        if (s == null) return ContractorInspection.Status.PLANNED;
        try { return ContractorInspection.Status.valueOf(s.toUpperCase()); }
        catch (IllegalArgumentException e) { return ContractorInspection.Status.PLANNED; }
    }
}
