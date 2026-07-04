package com.monosun.secportal.backup.controller;

import com.monosun.secportal.backup.dto.BackupDto;
import com.monosun.secportal.backup.service.BackupScheduler;
import com.monosun.secportal.backup.service.BackupService;
import com.monosun.secportal.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@RestController
@RequestMapping("/admin/backup")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class BackupController {

    private final BackupService backupService;
    private final BackupScheduler backupScheduler;

    /** 즉시 백업 생성 후 다운로드 */
    @PostMapping("/download")
    public ResponseEntity<byte[]> download(@RequestBody BackupDto.DownloadRequest request) {
        try {
            if (request.getPassword() == null || request.getPassword().isBlank()) {
                return ResponseEntity.badRequest().build();
            }
            byte[] data = backupService.createBackupBytes(request.getPassword());
            String ts = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String rawFilename = "secportal-backup-" + ts + ".bak";
            backupService.recordHistory(rawFilename, data.length, "DOWNLOAD", "SUCCESS",
                    "다운로드 완료 (" + formatSize(data.length) + ")");
            String encoded = URLEncoder.encode(rawFilename, StandardCharsets.UTF_8);
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encoded)
                    .header(HttpHeaders.CONTENT_LENGTH, String.valueOf(data.length))
                    .body(data);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    private String formatSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        return String.format("%.1f MB", bytes / (1024.0 * 1024));
    }

    /** 서버 저장소에 백업 생성 */
    @PostMapping("/save")
    public ApiResponse<String> save(@RequestBody BackupDto.DownloadRequest request) {
        if (request.getPassword() == null || request.getPassword().isBlank()) {
            return ApiResponse.badRequest("비밀번호를 입력해주세요.");
        }
        String filename = backupService.createAndSaveBackup(request.getPassword(), "MANUAL");
        return ApiResponse.ok("백업이 저장되었습니다: " + filename, filename);
    }

    /** 백업 복원 */
    @PostMapping("/restore")
    public ResponseEntity<ApiResponse<Void>> restore(
            @RequestParam("file") MultipartFile file,
            @RequestParam("password") String password) {
        try {
            if (password == null || password.isBlank()) {
                return ResponseEntity.badRequest().body(ApiResponse.badRequest("비밀번호를 입력해주세요."));
            }
            backupService.restoreBackup(file.getBytes(), password, file.getOriginalFilename(), file.getSize());
            return ResponseEntity.ok(ApiResponse.ok("복원이 완료되었습니다."));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.badRequest(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("복원 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }

    /** 서버 저장 백업 파일 목록 */
    @GetMapping("/files")
    public ApiResponse<List<BackupDto.FileInfo>> listFiles() {
        return ApiResponse.ok(backupService.listBackupFiles());
    }

    /** 서버 저장 백업 파일 다운로드 */
    @GetMapping("/files/{filename}/download")
    public ResponseEntity<byte[]> downloadFile(@PathVariable String filename) {
        try {
            byte[] data = backupService.readBackupFile(filename);
            String encoded = URLEncoder.encode(filename, StandardCharsets.UTF_8);
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encoded)
                    .body(data);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    /** 서버 저장 백업 파일 삭제 */
    @DeleteMapping("/files/{filename}")
    public ApiResponse<Void> deleteFile(@PathVariable String filename) {
        try {
            backupService.deleteBackupFile(filename);
            return ApiResponse.ok("삭제되었습니다.");
        } catch (Exception e) {
            return ApiResponse.error("삭제에 실패했습니다.");
        }
    }

    /** 백업 이력 조회 */
    @GetMapping("/history")
    public ApiResponse<List<BackupDto.HistoryResponse>> history() {
        return ApiResponse.ok(backupService.listHistory());
    }

    /** 백업 설정 조회 */
    @GetMapping("/config")
    public ApiResponse<BackupDto.Config> getConfig() {
        return ApiResponse.ok(backupService.getConfig());
    }

    /** 백업 설정 저장 */
    @PutMapping("/config")
    public ApiResponse<Void> updateConfig(@RequestBody BackupDto.Config config) {
        backupService.updateConfig(config, backupScheduler);
        return ApiResponse.ok("설정이 저장되었습니다.");
    }
}
