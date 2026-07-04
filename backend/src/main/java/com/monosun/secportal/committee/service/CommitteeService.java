package com.monosun.secportal.committee.service;

import com.monosun.secportal.auth.entity.User;
import com.monosun.secportal.committee.dto.CommitteeDto;
import com.monosun.secportal.committee.entity.CommitteeFile;
import com.monosun.secportal.committee.entity.CommitteeMeeting;
import com.monosun.secportal.committee.repository.CommitteeFileRepository;
import com.monosun.secportal.committee.repository.CommitteeMeetingRepository;
import com.monosun.secportal.common.exception.BusinessException;
import com.monosun.secportal.common.exception.ResourceNotFoundException;
import com.monosun.secportal.common.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CommitteeService {

    private final CommitteeMeetingRepository meetingRepo;
    private final CommitteeFileRepository fileRepo;
    private final FileStorageService fileStorageService;

    @Transactional(readOnly = true)
    public List<Integer> getYears() {
        return meetingRepo.findDistinctYears();
    }

    @Transactional(readOnly = true)
    public List<CommitteeDto.MeetingResponse> listByYear(int year) {
        return meetingRepo.findByYearOrderBySessionNoAsc(year)
                .stream().map(CommitteeDto.MeetingResponse::from).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public CommitteeDto.MeetingResponse get(Long id) {
        return CommitteeDto.MeetingResponse.from(findMeeting(id));
    }

    @Transactional
    public CommitteeDto.MeetingResponse create(CommitteeDto.MeetingRequest req, User user) {
        validate(req);
        if (meetingRepo.existsByYearAndSessionNo(req.getYear(), req.getSessionNo())) {
            throw new BusinessException(req.getYear() + "년 제" + req.getSessionNo() + "회 회의가 이미 존재합니다.");
        }
        CommitteeMeeting meeting = CommitteeMeeting.builder()
                .year(req.getYear())
                .sessionNo(req.getSessionNo())
                .title(req.getTitle().trim())
                .meetingDate(req.getMeetingDate())
                .location(req.getLocation())
                .attendees(req.getAttendees())
                .status(parseStatus(req.getStatus()))
                .createdBy(user)
                .build();
        return CommitteeDto.MeetingResponse.from(meetingRepo.save(meeting));
    }

    @Transactional
    public CommitteeDto.MeetingResponse update(Long id, CommitteeDto.MeetingRequest req) {
        CommitteeMeeting meeting = findMeeting(id);
        if (req.getTitle() != null && !req.getTitle().isBlank()) meeting.setTitle(req.getTitle().trim());
        if (req.getMeetingDate() != null) meeting.setMeetingDate(req.getMeetingDate());
        if (req.getLocation() != null) meeting.setLocation(req.getLocation());
        if (req.getAttendees() != null) meeting.setAttendees(req.getAttendees());
        if (req.getStatus() != null) meeting.setStatus(parseStatus(req.getStatus()));
        return CommitteeDto.MeetingResponse.from(meeting);
    }

    @Transactional
    public void delete(Long id) throws IOException {
        CommitteeMeeting meeting = findMeeting(id);
        for (CommitteeFile f : meeting.getFiles()) {
            if (f.getFilePath() != null) fileStorageService.delete(f.getFilePath());
        }
        meetingRepo.delete(meeting);
    }

    @Transactional
    public CommitteeDto.FileResponse addFile(Long meetingId, CommitteeDto.FileRequest req,
                                              MultipartFile file, User user) throws IOException {
        CommitteeMeeting meeting = findMeeting(meetingId);
        if (req.getTitle() == null || req.getTitle().isBlank()) throw new BusinessException("파일 제목을 입력하세요.");

        CommitteeFile.FileType fileType = parseFileType(req.getFileType());
        CommitteeFile cf = CommitteeFile.builder()
                .meeting(meeting)
                .fileType(fileType)
                .title(req.getTitle().trim())
                .uploader(user)
                .build();
        cf = fileRepo.save(cf);

        if (file != null && !file.isEmpty()) {
            String path = fileStorageService.store(file, "committee/" + cf.getId());
            cf.setFilePath(path);
            cf.setFileName(file.getOriginalFilename());
            cf.setFileSize(file.getSize());
        }
        return CommitteeDto.FileResponse.from(cf);
    }

    @Transactional
    public void deleteFile(Long fileId) throws IOException {
        CommitteeFile cf = fileRepo.findById(fileId)
                .orElseThrow(() -> new ResourceNotFoundException("CommitteeFile", fileId));
        if (cf.getFilePath() != null) fileStorageService.delete(cf.getFilePath());
        fileRepo.delete(cf);
    }

    public org.springframework.core.io.Resource downloadFile(Long fileId) {
        CommitteeFile cf = fileRepo.findById(fileId)
                .orElseThrow(() -> new ResourceNotFoundException("CommitteeFile", fileId));
        if (cf.getFilePath() == null) throw new BusinessException("첨부파일이 없습니다.");
        return fileStorageService.load(cf.getFilePath());
    }

    public CommitteeFile getFile(Long fileId) {
        return fileRepo.findById(fileId)
                .orElseThrow(() -> new ResourceNotFoundException("CommitteeFile", fileId));
    }

    private CommitteeMeeting findMeeting(Long id) {
        return meetingRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("CommitteeMeeting", id));
    }

    private void validate(CommitteeDto.MeetingRequest req) {
        if (req.getYear() <= 0) throw new BusinessException("연도를 입력하세요.");
        if (req.getSessionNo() <= 0) throw new BusinessException("회차를 입력하세요.");
        if (req.getTitle() == null || req.getTitle().isBlank()) throw new BusinessException("제목을 입력하세요.");
    }

    private CommitteeMeeting.Status parseStatus(String status) {
        if (status == null) return CommitteeMeeting.Status.PLANNED;
        try { return CommitteeMeeting.Status.valueOf(status.toUpperCase()); }
        catch (IllegalArgumentException e) { return CommitteeMeeting.Status.PLANNED; }
    }

    private CommitteeFile.FileType parseFileType(String type) {
        if (type == null) return CommitteeFile.FileType.OTHER;
        try { return CommitteeFile.FileType.valueOf(type.toUpperCase()); }
        catch (IllegalArgumentException e) { return CommitteeFile.FileType.OTHER; }
    }
}
