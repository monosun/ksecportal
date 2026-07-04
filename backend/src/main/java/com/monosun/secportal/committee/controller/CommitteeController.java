package com.monosun.secportal.committee.controller;

import com.monosun.secportal.auth.entity.User;
import com.monosun.secportal.committee.dto.CommitteeDto;
import com.monosun.secportal.committee.entity.CommitteeFile;
import com.monosun.secportal.committee.service.CommitteeService;
import com.monosun.secportal.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/committee")
@RequiredArgsConstructor
public class CommitteeController {

    private final CommitteeService service;

    @GetMapping("/years")
    public ApiResponse<List<Integer>> getYears() {
        return ApiResponse.ok(service.getYears());
    }

    @GetMapping
    public ApiResponse<List<CommitteeDto.MeetingResponse>> list(@RequestParam int year) {
        return ApiResponse.ok(service.listByYear(year));
    }

    @GetMapping("/{id}")
    public ApiResponse<CommitteeDto.MeetingResponse> get(@PathVariable Long id) {
        return ApiResponse.ok(service.get(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ApiResponse<CommitteeDto.MeetingResponse> create(
            @RequestBody CommitteeDto.MeetingRequest req,
            @AuthenticationPrincipal User user) {
        return ApiResponse.ok(service.create(req, user));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ApiResponse<CommitteeDto.MeetingResponse> update(
            @PathVariable Long id,
            @RequestBody CommitteeDto.MeetingRequest req) {
        return ApiResponse.ok(service.update(id, req));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ApiResponse<Void> delete(@PathVariable Long id) throws IOException {
        service.delete(id);
        return ApiResponse.ok(null);
    }

    // ── Files ──

    @PostMapping("/{meetingId}/files")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ApiResponse<CommitteeDto.FileResponse> addFile(
            @PathVariable Long meetingId,
            @RequestParam("fileType") String fileType,
            @RequestParam("title") String title,
            @RequestPart(value = "file", required = false) MultipartFile file,
            @AuthenticationPrincipal User user) throws IOException {
        CommitteeDto.FileRequest req = new CommitteeDto.FileRequest(fileType, title);
        return ApiResponse.ok(service.addFile(meetingId, req, file, user));
    }

    @DeleteMapping("/files/{fileId}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ApiResponse<Void> deleteFile(@PathVariable Long fileId) throws IOException {
        service.deleteFile(fileId);
        return ApiResponse.ok(null);
    }

    @GetMapping("/files/{fileId}/download")
    public ResponseEntity<Resource> downloadFile(@PathVariable Long fileId) {
        CommitteeFile cf = service.getFile(fileId);
        Resource resource = service.downloadFile(fileId);
        String encoded = URLEncoder.encode(cf.getFileName() != null ? cf.getFileName() : "file",
                StandardCharsets.UTF_8).replace("+", "%20");
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encoded)
                .body(resource);
    }
}
