package com.monosun.secportal.backup.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.monosun.secportal.backup.dto.BackupDto;
import com.monosun.secportal.backup.entity.BackupHistory;
import com.monosun.secportal.backup.repository.BackupHistoryRepository;
import com.monosun.secportal.setting.service.AppSettingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.*;
import javax.crypto.spec.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.SecureRandom;
import javax.crypto.spec.PBEKeySpec;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class BackupService {

    private static final String MAGIC = "SPBK";
    private static final int PBKDF2_ITERATIONS = 100_000;
    private static final int KEY_BITS = 256;
    private static final Set<String> EXCLUDE_TABLES = Set.of("backup_history");
    private static final DateTimeFormatter FILENAME_FMT = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    private final JdbcTemplate jdbc;
    private final AppSettingService settingService;
    private final BackupHistoryRepository historyRepo;

    private final ObjectMapper mapper = buildMapper();

    @Value("${backup.directory:./uploads/backups}")
    private String backupDirectory;

    private static ObjectMapper buildMapper() {
        ObjectMapper m = new ObjectMapper();
        m.registerModule(new JavaTimeModule());
        m.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return m;
    }

    // ── Public API ─────────────────────────────────────────────────────────

    public byte[] createBackupBytes(String password) throws Exception {
        Map<String, Object> export = buildExportData();
        byte[] json = mapper.writeValueAsBytes(export);
        byte[] compressed = gzip(json);
        return encrypt(compressed, password);
    }

    public String createAndSaveBackup(String password, String backupType) {
        String filename = "secportal-backup-" + LocalDateTime.now().format(FILENAME_FMT) + ".bak";
        BackupHistory history = BackupHistory.builder()
                .filename(filename)
                .backupType(backupType)
                .status("RUNNING")
                .build();
        historyRepo.save(history);

        try {
            byte[] data = createBackupBytes(password);
            Path dir = Paths.get(backupDirectory);
            Files.createDirectories(dir);
            Path file = dir.resolve(filename);
            Files.write(file, data);

            history.setFileSize((long) data.length);
            history.setStatus("SUCCESS");
            history.setMessage("백업 완료 (" + formatSize(data.length) + ")");
            historyRepo.save(history);

            pruneOldBackups();
            return filename;
        } catch (Exception e) {
            log.error("Backup failed: {}", e.getMessage(), e);
            history.setStatus("FAILED");
            history.setMessage(e.getMessage());
            historyRepo.save(history);
            throw new RuntimeException("백업 생성에 실패했습니다: " + e.getMessage(), e);
        }
    }

    @Transactional
    public void recordHistory(String filename, long fileSize, String backupType, String status, String message) {
        historyRepo.save(BackupHistory.builder()
                .filename(filename)
                .fileSize(fileSize)
                .backupType(backupType)
                .status(status)
                .message(message)
                .build());
    }

    public void restoreBackup(byte[] data, String password, String originalFilename, long originalSize) throws Exception {
        byte[] compressed;
        try {
            compressed = decrypt(data, password);
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalArgumentException("비밀번호가 올바르지 않거나 파일이 손상되었습니다.");
        }

        byte[] json = ungzip(compressed);
        Map<String, Object> export = mapper.readValue(json, new TypeReference<>() {});

        @SuppressWarnings("unchecked")
        Map<String, Object> tables = (Map<String, Object>) export.get("tables");
        if (tables == null) throw new IllegalArgumentException("유효하지 않은 백업 형식입니다.");

        List<String> currentTables = getBackupTableNames();
        Set<String> currentSet = new HashSet<>(currentTables);

        // Get column type info for all tables
        Map<String, Map<String, String>> colTypes = new HashMap<>();
        for (String t : currentTables) {
            colTypes.put(t, getColumnTypes(t));
        }

        jdbc.execute("SET FOREIGN_KEY_CHECKS = 0");
        try {
            for (Map.Entry<String, Object> entry : tables.entrySet()) {
                String tableName = entry.getKey();
                if (!currentSet.contains(tableName)) continue;

                @SuppressWarnings("unchecked")
                Map<String, Object> tableData = (Map<String, Object>) entry.getValue();
                @SuppressWarnings("unchecked")
                List<String> columns = (List<String>) tableData.get("columns");
                @SuppressWarnings("unchecked")
                List<List<Object>> rows = (List<List<Object>>) tableData.get("rows");

                if (columns == null || rows == null) continue;

                jdbc.execute("DELETE FROM `" + tableName + "`");

                if (rows.isEmpty()) continue;

                String colList = columns.stream().map(c -> "`" + c + "`").collect(Collectors.joining(", "));
                String placeholders = String.join(", ", Collections.nCopies(columns.size(), "?"));
                String insertSql = "INSERT INTO `" + tableName + "` (" + colList + ") VALUES (" + placeholders + ")";

                Map<String, String> types = colTypes.getOrDefault(tableName, Map.of());
                List<Object[]> batchArgs = new ArrayList<>();
                for (List<Object> row : rows) {
                    Object[] args = new Object[columns.size()];
                    for (int i = 0; i < columns.size(); i++) {
                        args[i] = convertValue(row.get(i), types.getOrDefault(columns.get(i).toLowerCase(), "varchar"));
                    }
                    batchArgs.add(args);
                }

                jdbc.batchUpdate(insertSql, batchArgs);
            }

            historyRepo.save(BackupHistory.builder()
                    .filename(originalFilename)
                    .fileSize(originalSize)
                    .backupType("RESTORE")
                    .status("SUCCESS")
                    .message("복원 완료")
                    .build());
        } finally {
            jdbc.execute("SET FOREIGN_KEY_CHECKS = 1");
        }
    }

    public List<BackupDto.FileInfo> listBackupFiles() {
        try {
            Path dir = Paths.get(backupDirectory);
            if (!Files.exists(dir)) return List.of();
            try (var stream = Files.list(dir)) {
                return stream
                    .filter(p -> p.getFileName().toString().endsWith(".bak"))
                    .sorted(Comparator.reverseOrder())
                    .map(p -> {
                        try {
                            return BackupDto.FileInfo.builder()
                                    .filename(p.getFileName().toString())
                                    .fileSize(Files.size(p))
                                    .lastModified(Files.getLastModifiedTime(p).toInstant()
                                            .atZone(java.time.ZoneId.systemDefault()).toLocalDateTime())
                                    .build();
                        } catch (IOException e) {
                            return null;
                        }
                    })
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
            }
        } catch (IOException e) {
            return List.of();
        }
    }

    public byte[] readBackupFile(String filename) throws IOException {
        Path file = resolveBackupFile(filename);
        return Files.readAllBytes(file);
    }

    public void deleteBackupFile(String filename) throws IOException {
        Path file = resolveBackupFile(filename);
        Files.deleteIfExists(file);
    }

    public List<BackupDto.HistoryResponse> listHistory() {
        return historyRepo.findAllByOrderByCreatedAtDesc().stream()
                .map(BackupDto.HistoryResponse::from)
                .collect(Collectors.toList());
    }

    public BackupDto.Config getConfig() {
        boolean enabled = "true".equals(settingService.getValue("backup.enabled"));
        String cron = nvl(settingService.getValue("backup.cron"), "0 0 2 * * ?");
        int keepCount = parseInt(settingService.getValue("backup.keep.count"), 10);
        String pw = settingService.getValue("backup.default.password");
        return new BackupDto.Config(enabled, cron, keepCount, pw != null && !pw.isBlank(), null);
    }

    public void updateConfig(BackupDto.Config config, BackupScheduler scheduler) {
        settingService.upsert("backup.enabled", String.valueOf(config.isEnabled()));
        settingService.upsert("backup.cron", nvl(config.getCron(), "0 0 2 * * ?"));
        settingService.upsert("backup.keep.count", String.valueOf(config.getKeepCount() > 0 ? config.getKeepCount() : 10));
        if (config.getDefaultPassword() != null) {
            settingService.upsert("backup.default.password", config.getDefaultPassword());
        }
        scheduler.reschedule(config.getCron(), config.isEnabled());
    }

    // ── Internal helpers ───────────────────────────────────────────────────

    private Map<String, Object> buildExportData() {
        List<String> tables = getBackupTableNames();
        Map<String, Object> export = new LinkedHashMap<>();
        export.put("version", "1.0");
        export.put("createdAt", LocalDateTime.now().toString());
        Map<String, Object> tableMap = new LinkedHashMap<>();
        for (String t : tables) {
            tableMap.put(t, exportTable(t));
        }
        export.put("tables", tableMap);
        return export;
    }

    private List<String> getBackupTableNames() {
        String sql = "SELECT table_name FROM information_schema.tables " +
                     "WHERE table_schema = DATABASE() AND table_type = 'BASE TABLE' " +
                     "ORDER BY table_name";
        return jdbc.queryForList(sql, String.class).stream()
                .filter(t -> !EXCLUDE_TABLES.contains(t))
                .collect(Collectors.toList());
    }

    private Map<String, Object> exportTable(String tableName) {
        List<Map<String, Object>> rawRows = jdbc.queryForList("SELECT * FROM `" + tableName + "`");
        if (rawRows.isEmpty()) {
            return Map.of("columns", List.of(), "rows", List.of());
        }
        List<String> columns = new ArrayList<>(rawRows.get(0).keySet());
        List<List<Object>> rows = rawRows.stream()
                .map(row -> columns.stream().map(c -> normalizeForExport(row.get(c))).collect(Collectors.toList()))
                .collect(Collectors.toList());
        return Map.of("columns", columns, "rows", rows);
    }

    private Object normalizeForExport(Object v) {
        if (v instanceof java.sql.Timestamp ts) return ts.toLocalDateTime().toString();
        if (v instanceof java.sql.Date d) return d.toString();
        if (v instanceof java.sql.Time t) return t.toString();
        if (v instanceof byte[] b) return Base64.getEncoder().encodeToString(b);
        return v;
    }

    private Map<String, String> getColumnTypes(String tableName) {
        String sql = "SELECT LOWER(column_name) AS col, LOWER(data_type) AS dtype " +
                     "FROM information_schema.columns " +
                     "WHERE table_schema = DATABASE() AND LOWER(table_name) = LOWER(?) " +
                     "ORDER BY ordinal_position";
        Map<String, String> types = new LinkedHashMap<>();
        jdbc.query(sql, rs -> {
            types.put(rs.getString("col"), rs.getString("dtype"));
        }, tableName);
        return types;
    }

    private Object convertValue(Object v, String dataType) {
        if (v == null) return null;
        try {
            switch (dataType) {
                case "datetime", "timestamp" -> {
                    if (v instanceof String s) return java.sql.Timestamp.valueOf(s.replace("T", " ").replace("Z", "").substring(0, 19));
                    if (v instanceof Long l) return new java.sql.Timestamp(l);
                }
                case "date" -> {
                    if (v instanceof String s) return java.sql.Date.valueOf(s.length() > 10 ? s.substring(0, 10) : s);
                }
                case "bigint" -> {
                    if (v instanceof Number n) return n.longValue();
                }
                case "int", "integer", "mediumint", "smallint" -> {
                    if (v instanceof Number n) return n.intValue();
                }
                case "tinyint" -> {
                    if (v instanceof Number n) return n.intValue();
                    if (v instanceof Boolean b) return b ? 1 : 0;
                }
                case "decimal", "numeric", "float", "double" -> {
                    if (v instanceof Number n) return n.doubleValue();
                }
            }
        } catch (Exception ignored) {}
        return v;
    }

    private Path resolveBackupFile(String filename) {
        String name = Paths.get(filename).getFileName().toString();
        if (!name.endsWith(".bak")) throw new IllegalArgumentException("유효하지 않은 파일명입니다.");
        Path file = Paths.get(backupDirectory).resolve(name);
        if (!file.normalize().startsWith(Paths.get(backupDirectory).normalize())) {
            throw new IllegalArgumentException("유효하지 않은 경로입니다.");
        }
        return file;
    }

    private void pruneOldBackups() {
        try {
            int keep = parseInt(settingService.getValue("backup.keep.count"), 10);
            List<Path> files;
            Path dir = Paths.get(backupDirectory);
            if (!Files.exists(dir)) return;
            try (var stream = Files.list(dir)) {
                files = stream.filter(p -> p.getFileName().toString().endsWith(".bak"))
                        .sorted(Comparator.reverseOrder())
                        .collect(Collectors.toList());
            }
            if (files.size() > keep) {
                for (Path f : files.subList(keep, files.size())) {
                    Files.deleteIfExists(f);
                    log.info("Pruned old backup: {}", f.getFileName());
                }
            }
        } catch (Exception e) {
            log.warn("Failed to prune backups: {}", e.getMessage());
        }
    }

    // ── Crypto ─────────────────────────────────────────────────────────────

    private byte[] encrypt(byte[] data, String password) throws Exception {
        byte[] salt = new byte[16];
        byte[] iv = new byte[12];
        SecureRandom rng = new SecureRandom();
        rng.nextBytes(salt);
        rng.nextBytes(iv);

        SecretKey key = deriveKey(password, salt);
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(128, iv));
        byte[] encrypted = cipher.doFinal(data);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write(MAGIC.getBytes(StandardCharsets.UTF_8));
        out.write(salt);
        out.write(iv);
        out.write(encrypted);
        return out.toByteArray();
    }

    private byte[] decrypt(byte[] data, String password) throws Exception {
        if (data.length < 32) throw new IllegalArgumentException("파일이 너무 작습니다.");
        ByteArrayInputStream in = new ByteArrayInputStream(data);
        String magic = new String(in.readNBytes(4), StandardCharsets.UTF_8);
        if (!MAGIC.equals(magic)) throw new IllegalArgumentException("유효하지 않은 백업 파일입니다.");

        byte[] salt = in.readNBytes(16);
        byte[] iv = in.readNBytes(12);
        byte[] encrypted = in.readAllBytes();

        SecretKey key = deriveKey(password, salt);
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(128, iv));
        return cipher.doFinal(encrypted);
    }

    private SecretKey deriveKey(String password, byte[] salt) throws Exception {
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, PBKDF2_ITERATIONS, KEY_BITS);
        byte[] keyBytes = factory.generateSecret(spec).getEncoded();
        return new SecretKeySpec(keyBytes, "AES");
    }

    private byte[] gzip(byte[] data) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (GZIPOutputStream gz = new GZIPOutputStream(out)) { gz.write(data); }
        return out.toByteArray();
    }

    private byte[] ungzip(byte[] data) throws IOException {
        try (GZIPInputStream gz = new GZIPInputStream(new ByteArrayInputStream(data))) {
            return gz.readAllBytes();
        }
    }

    private String nvl(String s, String def) { return (s != null && !s.isBlank()) ? s : def; }
    private int parseInt(String s, int def) {
        try { return s != null ? Integer.parseInt(s) : def; } catch (NumberFormatException e) { return def; }
    }
    private String formatSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        return String.format("%.1f MB", bytes / (1024.0 * 1024));
    }
}
