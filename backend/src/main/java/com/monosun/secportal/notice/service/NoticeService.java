package com.monosun.secportal.notice.service;

import com.monosun.secportal.auth.entity.User;
import com.monosun.secportal.common.exception.ResourceNotFoundException;
import com.monosun.secportal.notice.dto.NoticeDto;
import com.monosun.secportal.notice.entity.Notice;
import com.monosun.secportal.notice.repository.NoticeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NoticeService {

    private final NoticeRepository noticeRepository;

    @Transactional(readOnly = true)
    public List<NoticeDto.Response> listActive() {
        return noticeRepository.findAllByActiveTrueOrderByPinnedDescCreatedAtDesc()
                .stream().map(NoticeDto.Response::from).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<NoticeDto.Response> listAll() {
        return noticeRepository.findAllByOrderByPinnedDescCreatedAtDesc()
                .stream().map(NoticeDto.Response::from).collect(Collectors.toList());
    }

    @Transactional
    public NoticeDto.Response create(NoticeDto.CreateRequest req, User creator) {
        Notice notice = Notice.builder()
                .title(req.getTitle())
                .content(req.getContent())
                .pinned(Boolean.TRUE.equals(req.getPinned()))
                .createdBy(creator)
                .build();
        return NoticeDto.Response.from(noticeRepository.save(notice));
    }

    @Transactional
    public NoticeDto.Response update(Long id, NoticeDto.UpdateRequest req) {
        Notice notice = noticeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Notice", id));
        if (req.getTitle() != null) notice.setTitle(req.getTitle());
        if (req.getContent() != null) notice.setContent(req.getContent());
        if (req.getActive() != null) notice.setActive(req.getActive());
        if (req.getPinned() != null) notice.setPinned(req.getPinned());
        return NoticeDto.Response.from(notice);
    }

    @Transactional
    public void delete(Long id) {
        if (!noticeRepository.existsById(id)) {
            throw new ResourceNotFoundException("Notice", id);
        }
        noticeRepository.deleteById(id);
    }
}
