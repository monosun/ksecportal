package com.monosun.secportal.sourcescan.service;

import com.monosun.secportal.sourcescan.entity.SourceScanFinding.Severity;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

/**
 * 내장 OWASP 소스코드 정적분석(SAST) 엔진.
 * GitHub 저장소 tarball을 메모리에서 해제하여 소스 파일을 라인 단위 규칙(정규식)으로 점검한다.
 * 규칙은 OWASP Top 10:2021 카테고리와 CWE에 매핑된다.
 */
@Component
public class SastEngine {

    // 점검 대상 소스 확장자
    private static final Set<String> CODE_EXT = Set.of(
            "java", "kt", "scala", "groovy", "js", "jsx", "ts", "tsx", "vue", "mjs", "cjs",
            "py", "rb", "php", "go", "cs", "c", "cc", "cpp", "h", "hpp", "rs", "swift",
            "sql", "sh", "bash", "yml", "yaml", "xml", "properties", "env", "conf", "ini", "tf");

    // 점검 제외 경로 세그먼트
    private static final Set<String> SKIP_DIRS = Set.of(
            "node_modules", "vendor", "dist", "build", "out", "target", ".git",
            "test", "tests", "__tests__", "spec", "mock", "mocks", "fixtures",
            "generated", "migrations", ".gradle", "coverage", "e2e");

    private static final long MAX_FILE_BYTES = 500_000;
    private static final int MAX_FILES = 2000;
    private static final int MAX_FINDINGS = 1000;
    private static final int MAX_LINE_LEN = 2000;   // 초장문(압축·번들) 라인 스킵

    private static final List<Rule> RULES = buildRules();

    public List<Hit> scanArchive(byte[] tarGz) {
        List<Hit> hits = new ArrayList<>();
        int files = 0;
        try (TarArchiveInputStream tar = new TarArchiveInputStream(
                new GZIPInputStream(new ByteArrayInputStream(tarGz)))) {
            TarArchiveEntry entry;
            while ((entry = tar.getNextEntry()) != null) {
                if (hits.size() >= MAX_FINDINGS || files >= MAX_FILES) break;
                if (entry.isDirectory() || entry.getSize() <= 0 || entry.getSize() > MAX_FILE_BYTES) continue;

                String rel = stripTopDir(entry.getName());
                if (rel == null || !isScannable(rel)) continue;

                byte[] data = tar.readAllBytes();
                if (looksBinary(data)) continue;
                files++;
                scanFile(rel, new String(data, StandardCharsets.UTF_8), hits);
            }
        } catch (Exception e) {
            // 손상된 tarball 등 — 부분 결과라도 반환
        }
        return hits;
    }

    private void scanFile(String path, String content, List<Hit> hits) {
        String[] lines = content.split("\n", -1);
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            if (line.length() > MAX_LINE_LEN) continue;
            for (Rule rule : RULES) {
                Matcher m = rule.pattern().matcher(line);
                if (m.find()) {
                    hits.add(new Hit(rule.severity(), rule.id(), rule.owasp(), rule.cwe(), rule.title(), path, i + 1));
                    if (hits.size() >= MAX_FINDINGS) return;
                }
            }
        }
    }

    // ── 헬퍼 ─────────────────────────────────────────────────────────────────

    /** GitHub tarball 최상위 디렉터리("owner-repo-sha/")를 제거하고 저장소 상대경로 반환 */
    private String stripTopDir(String name) {
        int slash = name.indexOf('/');
        if (slash < 0 || slash == name.length() - 1) return null;
        return name.substring(slash + 1);
    }

    private boolean isScannable(String rel) {
        for (String seg : rel.split("/")) {
            if (SKIP_DIRS.contains(seg.toLowerCase())) return false;
        }
        int dot = rel.lastIndexOf('.');
        if (dot < 0) return false;
        return CODE_EXT.contains(rel.substring(dot + 1).toLowerCase());
    }

    private boolean looksBinary(byte[] data) {
        int n = Math.min(data.length, 4000);
        for (int i = 0; i < n; i++) if (data[i] == 0) return true;
        return false;
    }

    // ── 규칙 정의 ────────────────────────────────────────────────────────────

    private static List<Rule> buildRules() {
        List<Rule> r = new ArrayList<>();
        int F = Pattern.CASE_INSENSITIVE;

        // A01 접근통제
        r.add(new Rule("SAST-AUTHZ-PERMITALL", "A01:2021-Broken Access Control", "CWE-284", Severity.MEDIUM,
                "모든 요청 permitAll() — 인가 우회 위험", Pattern.compile("anyRequest\\(\\)\\s*\\.\\s*permitAll\\(\\)", F)));
        // A02 암호 실패
        r.add(new Rule("SAST-CRYPTO-PRIVKEY", "A02:2021-Cryptographic Failures", "CWE-798", Severity.CRITICAL,
                "개인키(Private Key) 하드코딩", Pattern.compile("-----BEGIN (RSA |EC |DSA |OPENSSH |PGP )?PRIVATE KEY-----")));
        r.add(new Rule("SAST-CRYPTO-WEAKHASH", "A02:2021-Cryptographic Failures", "CWE-327", Severity.MEDIUM,
                "취약한 해시 알고리즘(MD5/SHA-1) 사용", Pattern.compile(
                "MessageDigest\\.getInstance\\(\\s*[\"'](MD5|SHA-?1)[\"']|hashlib\\.(md5|sha1)\\(|createHash\\(\\s*[\"'](md5|sha1)[\"']", F)));
        r.add(new Rule("SAST-CRYPTO-WEAKCIPHER", "A02:2021-Cryptographic Failures", "CWE-327", Severity.HIGH,
                "취약한 암호 알고리즘/모드(DES·RC4·ECB) 사용", Pattern.compile(
                "Cipher\\.getInstance\\(\\s*[\"'](DES|DESede|RC4|RC2|[^\"']*ECB[^\"']*)[\"']", F)));
        r.add(new Rule("SAST-TLS-DISABLED", "A02:2021-Cryptographic Failures", "CWE-295", Severity.HIGH,
                "TLS 인증서 검증 비활성화", Pattern.compile(
                "rejectUnauthorized\\s*:\\s*false|verify\\s*=\\s*False|CURLOPT_SSL_VERIFY(PEER|HOST)\\s*,\\s*(0|false)|TrustAllCerts|NoopHostnameVerifier|ALLOW_ALL_HOSTNAME", F)));
        r.add(new Rule("SAST-RANDOM-INSECURE", "A02:2021-Cryptographic Failures", "CWE-330", Severity.LOW,
                "보안 용도에 부적합한 난수(Math.random/java.util.Random)", Pattern.compile(
                "Math\\.random\\(\\)|new\\s+java\\.util\\.Random\\(|new\\s+Random\\(", F)));
        // A03 인젝션
        r.add(new Rule("SAST-SQLI-CONCAT", "A03:2021-Injection", "CWE-89", Severity.HIGH,
                "SQL 문자열 결합 — SQL 인젝션 위험", Pattern.compile(
                "(executeQuery|executeUpdate|execute|createQuery|createNativeQuery|prepareStatement|rawQuery|db\\.query)\\s*\\(\\s*[\"'][^\"']*[\"']\\s*\\+", F)));
        r.add(new Rule("SAST-CMDI", "A03:2021-Injection", "CWE-78", Severity.HIGH,
                "OS 명령 실행 싱크 — 명령 인젝션 위험(입력 검증 확인)", Pattern.compile(
                "Runtime\\.getRuntime\\(\\)\\.exec\\(|new\\s+ProcessBuilder\\(|os\\.system\\(|subprocess\\.(call|Popen|run)\\(|child_process\\.(exec|execSync)\\(", F)));
        r.add(new Rule("SAST-CODE-EVAL", "A03:2021-Injection", "CWE-95", Severity.HIGH,
                "동적 코드 실행(eval/Function) — 코드 인젝션 위험", Pattern.compile(
                "(^|[^.\\w])eval\\s*\\(|new\\s+Function\\s*\\(|\\bexec\\s*\\(\\s*[\"']", F)));
        // A05 보안 설정 오류
        r.add(new Rule("SAST-CORS-WILDCARD", "A05:2021-Security Misconfiguration", "CWE-942", Severity.MEDIUM,
                "CORS 와일드카드(*) 허용", Pattern.compile(
                "Access-Control-Allow-Origin[\"'\\s:=]+\\*|allowedOrigins?\\(\\s*[\"']\\*[\"']|origin\\s*:\\s*[\"']\\*[\"']", F)));
        r.add(new Rule("SAST-CSRF-DISABLED", "A05:2021-Security Misconfiguration", "CWE-352", Severity.MEDIUM,
                "CSRF 보호 비활성화", Pattern.compile("csrf\\(\\)\\s*\\.\\s*disable\\(\\)|csrf\\s*:\\s*false", F)));
        r.add(new Rule("SAST-XXE", "A05:2021-Security Misconfiguration", "CWE-611", Severity.MEDIUM,
                "XML 파서 XXE 방어 설정 확인 필요", Pattern.compile(
                "(DocumentBuilderFactory|SAXParserFactory|XMLInputFactory)\\.newInstance\\(\\)", F)));
        r.add(new Rule("SAST-DEBUG-ON", "A05:2021-Security Misconfiguration", "CWE-489", Severity.LOW,
                "디버그 모드 활성화", Pattern.compile("(app\\.)?debug\\s*[=:]\\s*(true|True)", F)));
        // A07 인증 실패 / 하드코딩 비밀
        r.add(new Rule("SAST-HARDCODED-SECRET", "A07:2021-Identification and Authentication Failures", "CWE-798", Severity.HIGH,
                "자격증명/비밀 하드코딩 의심", Pattern.compile(
                "(password|passwd|pwd|secret|api[_-]?key|access[_-]?key|private[_-]?key|token)\\s*[:=]\\s*[\"'][^\"'\\n]{6,}[\"']", F)));
        // A08 무결성 실패 — 안전하지 않은 역직렬화
        r.add(new Rule("SAST-DESERIALIZE", "A08:2021-Software and Data Integrity Failures", "CWE-502", Severity.HIGH,
                "안전하지 않은 역직렬화", Pattern.compile(
                "new\\s+ObjectInputStream|\\.readObject\\s*\\(|new\\s+XMLDecoder\\(|yaml\\.load\\s*\\((?![^)]*Loader)|pickle\\.loads?\\(", F)));
        return r;
    }

    // ── 자료구조 ─────────────────────────────────────────────────────────────

    private record Rule(String id, String owasp, String cwe, Severity severity, String title, Pattern pattern) {}

    public record Hit(Severity severity, String ruleId, String owasp, String cwe, String title, String path, int line) {}
}
