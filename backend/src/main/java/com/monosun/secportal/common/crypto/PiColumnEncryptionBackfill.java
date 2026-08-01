package com.monosun.secportal.common.crypto;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 개인정보 컬럼의 남아 있는 평문을 암호화하는 백필 러너.
 *
 * <p>이미 암호화된 값({@code PIENC1:} 접두어)은 건너뛰므로 매 기동마다 안전하게 다시 실행할 수 있다.
 * 예전 평문 백업을 복원한 뒤에도 재기동하면 다시 암호화된다.</p>
 *
 * <p>컬럼 길이가 부족하면(마이그레이션 미적용) 값이 잘려 복구가 불가능해지므로,
 * 실제 길이를 먼저 확인하고 부족하면 <b>건너뛴 뒤 경고</b>만 남긴다.</p>
 */
@Component
@RequiredArgsConstructor
@Slf4j
@Order(20)
public class PiColumnEncryptionBackfill implements ApplicationRunner {

    /** 암호문을 담으려면 최소 이 길이가 필요하다 */
    private static final int MIN_COLUMN_LENGTH = 255;

    private record Target(String table, List<String> columns) {}

    private static final List<Target> TARGETS = List.of(
            new Target("privacy_rights_requests", List.of("subject_name", "contact")),
            new Target("privacy_contractors", List.of("business_number", "contact_person", "contact_email", "contact_phone"))
    );

    private final JdbcTemplate jdbc;

    @Override
    public void run(ApplicationArguments args) {
        if (!ColumnCipher.isEnabled()) return;

        int total = 0;
        for (Target target : TARGETS) {
            try {
                total += backfill(target);
            } catch (Exception e) {
                log.error("[개인정보 암호화] {} 백필 중 오류: {}", target.table(), e.getMessage());
            }
        }
        if (total > 0) {
            log.info("[개인정보 암호화] 평문으로 남아 있던 개인정보 {}건을 암호화했습니다.", total);
        }
    }

    private int backfill(Target target) {
        List<String> columns = target.columns().stream()
                .filter(c -> hasEnoughLength(target.table(), c))
                .toList();
        if (columns.isEmpty()) return 0;

        String selected = String.join(", ", columns);
        String where = columns.stream()
                .map(c -> "(" + c + " IS NOT NULL AND " + c + " <> '' AND " + c + " NOT LIKE '" + ColumnCipher.PREFIX + "%')")
                .reduce((a, b) -> a + " OR " + b)
                .orElseThrow();

        List<Map<String, Object>> rows = jdbc.queryForList(
                "SELECT id, " + selected + " FROM " + target.table() + " WHERE " + where);

        int updated = 0;
        for (Map<String, Object> row : rows) {
            Object id = row.get("id");
            for (String column : columns) {
                Object value = row.get(column);
                if (!(value instanceof String s) || s.isEmpty() || ColumnCipher.isEncrypted(s)) continue;
                jdbc.update("UPDATE " + target.table() + " SET " + column + " = ? WHERE id = ?",
                        ColumnCipher.encrypt(s), id);
                updated++;
            }
        }
        return updated;
    }

    /** 컬럼 길이가 암호문을 담기에 충분한지 — 부족하면 마이그레이션 안내 후 건너뛴다 */
    private boolean hasEnoughLength(String table, String column) {
        Integer length = jdbc.queryForObject(
                "SELECT character_maximum_length FROM information_schema.columns "
                        + "WHERE table_schema = DATABASE() AND table_name = ? AND column_name = ?",
                Integer.class, table, column);
        if (length == null) {
            log.warn("[개인정보 암호화] {}.{} 컬럼을 찾지 못해 백필을 건너뜁니다.", table, column);
            return false;
        }
        if (length < MIN_COLUMN_LENGTH) {
            // 이 상태에서는 신규 저장도 실패하므로(암호문이 컬럼 길이를 넘김) 눈에 띄게 남긴다
            log.error("[개인정보 암호화] {}.{} 길이가 {}자로 부족합니다. 백필을 건너뛰며 이 컬럼의 저장도 실패할 수 있습니다. "
                            + "db/migration/v1.28.0_pi_column_encryption.sql 을 실행한 뒤 재기동하세요.",
                    table, column, length);
            return false;
        }
        return true;
    }
}
