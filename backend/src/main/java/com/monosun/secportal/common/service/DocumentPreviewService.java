package com.monosun.secportal.common.service;

import com.monosun.secportal.common.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 브라우저가 직접 열지 못하는 오피스 문서(PPT·DOC 등)를 PDF 로 변환해 미리보기에 쓰도록 돕는다.
 *
 * <p>변환은 별도 컨테이너(Gotenberg = LibreOffice 를 감싼 변환 HTTP API)에 맡긴다.
 * 백엔드 이미지에 LibreOffice 와 한글 폰트를 넣으면 이미지가 수백 MB 커지기 때문이다.
 *
 * <p>변환 결과는 업로드 루트 아래 {@code preview/<원본상대경로>.pdf} 로 캐시한다.
 * 저장 파일명은 UUID 라 한 번 만들어진 파일의 내용은 바뀌지 않으므로 캐시 무효화가 필요 없고,
 * 원본이 지워질 때 {@link #deleteCache(String)} 로 함께 지운다.
 */
@Slf4j
@Service
public class DocumentPreviewService {

    /**
     * PDF 변환으로 미리보기를 지원하는 확장자.
     * 엑셀·CSV·이미지·텍스트는 화면(프론트)에서 직접 렌더링하므로 여기 넣지 않는다.
     * .hwp/.hwpx 는 LibreOffice 변환 품질이 일정하지 않아 제외한다(다운로드 안내).
     */
    private static final Set<String> CONVERTIBLE = Set.of(
            "ppt", "pptx", "pptm", "pps", "ppsx", "ppsm", "pot", "potx", "odp",
            "doc", "docx", "odt", "rtf");

    private static final String CACHE_DIR = "preview";

    private final FileStorageService fileStorageService;
    private final String convertUrl;
    private final long maxFileBytes;
    private final RestClient restClient;

    /** 같은 파일을 여러 사용자가 동시에 열어도 변환은 한 번만 하도록 경로별 잠금을 둔다 */
    private final ConcurrentHashMap<String, ReentrantLock> locks = new ConcurrentHashMap<>();

    public DocumentPreviewService(
            FileStorageService fileStorageService,
            @Value("${app.preview.convert-url:}") String convertUrl,
            @Value("${app.preview.timeout-seconds:120}") int timeoutSeconds,
            @Value("${app.preview.max-file-mb:80}") int maxFileMb) {
        this.fileStorageService = fileStorageService;
        this.convertUrl = convertUrl == null ? "" : convertUrl.trim().replaceAll("/+$", "");
        this.maxFileBytes = (long) maxFileMb * 1024 * 1024;

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(5));
        // 큰 발표자료는 LibreOffice 변환에만 수십 초가 걸린다
        factory.setReadTimeout(Duration.ofSeconds(timeoutSeconds));
        this.restClient = RestClient.builder().requestFactory(factory).build();
    }

    /** 이 파일이 PDF 변환 미리보기 대상인지 */
    public boolean isConvertible(String fileName) {
        return CONVERTIBLE.contains(extension(fileName));
    }

    /** 변환 서버가 설정되어 있는지 */
    public boolean isEnabled() {
        return !convertUrl.isEmpty();
    }

    /**
     * 오피스 문서를 PDF 로 변환한 결과를 돌려준다. 이미 변환해 둔 것이 있으면 그대로 쓴다.
     *
     * @param relativePath 업로드 루트 기준 저장 경로 (예: {@code secdoc/12/uuid.pptx})
     * @param displayName  원래 파일명 — 확장자 판별과 오류 메시지에만 쓴다
     */
    public Resource toPdf(String relativePath, String displayName) {
        if (relativePath == null || relativePath.isBlank()) {
            throw new BusinessException("첨부파일이 없습니다.");
        }
        if (!isConvertible(relativePath) && !isConvertible(displayName)) {
            throw new BusinessException("이 형식은 미리보기를 지원하지 않습니다. 다운로드해서 확인해주세요.");
        }
        if (!isEnabled()) {
            throw new BusinessException("문서 미리보기 변환 기능이 설정되어 있지 않습니다. 파일을 다운로드해 확인해주세요.");
        }

        Path source = fileStorageService.resolvePath(relativePath);
        if (!Files.exists(source)) {
            throw new BusinessException("원본 파일을 찾을 수 없습니다.");
        }
        Path cached = cachePath(relativePath);
        Resource ready = readable(cached);
        if (ready != null) return ready;

        ReentrantLock lock = locks.computeIfAbsent(relativePath, k -> new ReentrantLock());
        lock.lock();
        try {
            // 잠금을 기다리는 사이 다른 요청이 이미 변환해 두었을 수 있다
            ready = readable(cached);
            if (ready != null) return ready;

            long size = Files.size(source);
            if (size > maxFileBytes) {
                throw new BusinessException(
                        "파일이 너무 커서 미리보기를 만들 수 없습니다(" + (maxFileBytes / 1024 / 1024) + "MB 초과). 다운로드해 확인해주세요.");
            }
            convert(source, cached);
            Resource converted = readable(cached);
            if (converted == null) throw new BusinessException("미리보기를 만들지 못했습니다. 파일을 다운로드해 확인해주세요.");
            return converted;
        } catch (BusinessException e) {
            throw e;   // 사유가 담긴 안내는 그대로 화면에 전달한다
        } catch (IOException e) {
            log.warn("문서 미리보기 변환 실패: {}", relativePath, e);
            throw new BusinessException("미리보기 파일을 저장하지 못했습니다(" + e.getMessage() + "). 파일을 다운로드해 확인해주세요.");
        } catch (RuntimeException | LinkageError e) {
            // 변환 경로에서 예상 못 한 오류가 나면 화면에는 500(Internal server error)만 떠서
            // 사용자가 원인도 대안도 알 수 없다. 사유를 로그에 남기고 안내 메시지로 바꿔 돌려준다.
            log.error("문서 미리보기 변환 중 오류: {}", relativePath, e);
            throw new BusinessException("미리보기를 만드는 중 오류가 발생했습니다(서버 오류). 파일을 다운로드해 확인해주세요.");
        } finally {
            lock.unlock();
            // 대기자가 없을 때만 정리해 잠금 맵이 무한히 커지지 않게 한다
            if (!lock.hasQueuedThreads()) locks.remove(relativePath, lock);
        }
    }

    /** 원본이 지워질 때 변환 캐시도 함께 지운다 */
    public void deleteCache(String relativePath) {
        if (relativePath == null || relativePath.isBlank()) return;
        try {
            Files.deleteIfExists(cachePath(relativePath));
        } catch (IOException e) {
            log.warn("미리보기 캐시 삭제 실패: {}", relativePath, e);
        }
    }

    private void convert(Path source, Path target) throws IOException {
        Files.createDirectories(target.getParent());

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        // Gotenberg 는 파일명 확장자로 변환 필터를 고른다. 저장 파일명(UUID+확장자)이라 ASCII 로 안전하다.
        body.add("files", new FileSystemResource(source));

        // 변환 중인 파일이 완성본으로 보이지 않도록 임시 파일에 받아 두었다가 옮긴다.
        // 변환된 PDF 는 원본보다 커지는 경우도 있어 메모리에 통째로 담지 않고 흘려 쓴다.
        Path temp = Files.createTempFile(target.getParent(), "convert-", ".part");
        try {
            restClient.post()
                    .uri(convertUrl + "/forms/libreoffice/convert")
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(body)
                    .exchange((request, response) -> {
                        if (!response.getStatusCode().is2xxSuccessful()) {
                            // Gotenberg 는 실패 사유를 본문에 평문으로 담아 준다(암호 걸린 파일·손상된 파일 등)
                            String reason = readErrorBody(response);
                            log.warn("문서 변환 실패({}): 변환 서버 응답 {} {}",
                                    source.getFileName(), response.getStatusCode(), reason);
                            throw new BusinessException("이 문서는 미리보기로 변환하지 못했습니다"
                                    + "(변환 서버 응답 " + response.getStatusCode().value()
                                    + (reason.isBlank() ? "" : ": " + reason) + "). 파일을 다운로드해 확인해주세요.");
                        }
                        try (java.io.InputStream in = response.getBody()) {
                            Files.copy(in, temp, StandardCopyOption.REPLACE_EXISTING);
                        }
                        return null;
                    });
            if (Files.size(temp) == 0) {
                throw new BusinessException("이 문서는 미리보기로 변환하지 못했습니다. 파일을 다운로드해 확인해주세요.");
            }
            Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING);
        } catch (ResourceAccessException e) {
            log.warn("문서 변환 서버 연결 실패({}): {}", convertUrl, e.getMessage());
            throw new BusinessException("문서 변환 서버에 연결하지 못했습니다. 잠시 후 다시 시도하거나 파일을 다운로드해주세요.");
        } catch (RestClientException e) {
            log.warn("문서 변환 실패({}): {}", source.getFileName(), e.getMessage());
            throw new BusinessException("이 문서는 미리보기로 변환하지 못했습니다"
                    + "(변환 요청 실패). 파일을 다운로드해 확인해주세요.");
        } finally {
            Files.deleteIfExists(temp);
        }
    }

    /** 변환 서버가 돌려준 실패 사유를 한 줄로 추린다 (너무 길면 잘라 낸다) */
    private String readErrorBody(org.springframework.http.client.ClientHttpResponse response) {
        try (java.io.InputStream in = response.getBody()) {
            String body = new String(in.readNBytes(500), java.nio.charset.StandardCharsets.UTF_8)
                    .replaceAll("\s+", " ").trim();
            return body.length() > 200 ? body.substring(0, 200) + "…" : body;
        } catch (Exception e) {
            return "";
        }
    }

    private Path cachePath(String relativePath) {
        return fileStorageService.resolvePath(CACHE_DIR + "/" + relativePath + ".pdf");
    }

    private Resource readable(Path path) {
        try {
            if (!Files.exists(path) || Files.size(path) == 0) return null;
            return new org.springframework.core.io.UrlResource(path.toUri());
        } catch (IOException e) {
            return null;
        }
    }

    private String extension(String name) {
        if (name == null) return "";
        int dot = name.lastIndexOf('.');
        return dot > 0 ? name.substring(dot + 1).toLowerCase() : "";
    }
}
