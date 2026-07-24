package com.monosun.secportal.common.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.*;
import java.util.Set;
import java.util.UUID;

@Service
public class FileStorageService {

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            ".jpg", ".jpeg", ".png", ".gif", ".webp",
            ".pdf", ".txt", ".csv",
            ".doc", ".docx", ".xls", ".xlsx", ".ppt", ".pptx",
            ".zip"
    );

    private final Path rootDir;

    public FileStorageService(@Value("${app.upload.dir:uploads}") String uploadDir) {
        this.rootDir = Paths.get(uploadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.rootDir);
        } catch (IOException e) {
            throw new RuntimeException("업로드 디렉터리 생성 실패: " + rootDir, e);
        }
    }

    public String store(MultipartFile file, String subDir) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("빈 파일은 업로드할 수 없습니다.");
        }
        String original = file.getOriginalFilename() != null ? file.getOriginalFilename() : "file";
        String ext = "";
        int dot = original.lastIndexOf('.');
        if (dot > 0) ext = original.substring(dot).toLowerCase();
        if (!ALLOWED_EXTENSIONS.contains(ext)) {
            throw new IllegalArgumentException("허용되지 않는 파일 형식입니다: " + ext);
        }

        Path targetDir = rootDir.resolve(subDir).normalize();
        if (!targetDir.startsWith(rootDir)) throw new SecurityException("잘못된 저장 경로");
        Files.createDirectories(targetDir);

        String storedName = UUID.randomUUID() + ext;
        Files.copy(file.getInputStream(), targetDir.resolve(storedName), StandardCopyOption.REPLACE_EXISTING);
        return rootDir.relativize(targetDir.resolve(storedName)).toString().replace('\\', '/');
    }

    /**
     * 이미 저장된 파일을 다른 하위 디렉터리로 복제하고 새 상대 경로를 돌려준다.
     * 원본이 없으면 null 을 돌려준다(복사 대상 레코드는 파일 없이 유지).
     */
    public String copy(String relativePath, String subDir) throws IOException {
        if (relativePath == null || relativePath.isBlank()) return null;
        Path source = rootDir.resolve(relativePath).normalize();
        if (!source.startsWith(rootDir)) throw new SecurityException("잘못된 파일 경로");
        if (!Files.exists(source)) return null;

        Path targetDir = rootDir.resolve(subDir).normalize();
        if (!targetDir.startsWith(rootDir)) throw new SecurityException("잘못된 저장 경로");
        Files.createDirectories(targetDir);

        String name = source.getFileName().toString();
        int dot = name.lastIndexOf('.');
        String ext = dot > 0 ? name.substring(dot).toLowerCase() : "";
        Path target = targetDir.resolve(UUID.randomUUID() + ext);
        Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
        return rootDir.relativize(target).toString().replace('\\', '/');
    }

    public Resource load(String relativePath) {
        try {
            Path file = rootDir.resolve(relativePath).normalize();
            if (!file.startsWith(rootDir)) throw new SecurityException("잘못된 파일 경로");
            Resource resource = new UrlResource(file.toUri());
            if (resource.exists() && resource.isReadable()) return resource;
            throw new RuntimeException("파일을 찾을 수 없습니다: " + relativePath);
        } catch (MalformedURLException e) {
            throw new RuntimeException("파일 경로 오류: " + relativePath, e);
        }
    }

    public void delete(String relativePath) throws IOException {
        if (relativePath == null || relativePath.isBlank()) return;
        Path file = rootDir.resolve(relativePath).normalize();
        if (!file.startsWith(rootDir)) throw new SecurityException("잘못된 파일 경로");
        Files.deleteIfExists(file);
    }
}
