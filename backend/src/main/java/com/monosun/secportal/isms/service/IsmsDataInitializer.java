package com.monosun.secportal.isms.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.monosun.secportal.isms.entity.IsmsItem;
import com.monosun.secportal.isms.repository.IsmsItemRepository;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class IsmsDataInitializer implements CommandLineRunner {

    private final IsmsItemRepository itemRepository;
    private final ObjectMapper objectMapper;

    @Override
    public void run(String... args) throws Exception {
        try (InputStream is = getClass().getResourceAsStream("/isms_items.json")) {
            if (is == null) {
                log.warn("isms_items.json not found in classpath — skipping ISMS seed");
                return;
            }
            List<IsmsItemSeed> seeds = objectMapper.readValue(is, new TypeReference<>() {});
            List<IsmsItem> toAdd = new ArrayList<>();
            List<IsmsItem> toBackfill = new ArrayList<>();
            for (int i = 0; i < seeds.size(); i++) {
                IsmsItemSeed s = seeds.get(i);
                var existing = itemRepository.findByItemCode(s.code);
                if (existing.isEmpty()) {
                    toAdd.add(IsmsItem.builder()
                            .itemCode(s.code)
                            .itemName(s.name)
                            .domainCode(s.domainCode)
                            .domainName(s.domainName)
                            .sectionNum(s.section)
                            .sectionName(s.sectionName)
                            .description(s.desc)
                            .guide(s.guide)
                            .defaultEvidenceTitle(s.evidenceTitle)
                            .defaultEvidenceContent(s.evidenceContent)
                            .sortOrder(i + 1)
                            .build());
                } else {
                    // 기존 항목: 기본 증적제목/내용/가이드가 비어있으면 시드값으로 백필한다.
                    // (사용자가 이미 입력한 값은 덮어쓰지 않는다 — self-heal)
                    IsmsItem item = existing.get();
                    boolean changed = false;
                    if (isBlank(item.getGuide()) && notBlank(s.guide)) {
                        item.setGuide(s.guide); changed = true;
                    }
                    if (isBlank(item.getDefaultEvidenceTitle()) && notBlank(s.evidenceTitle)) {
                        item.setDefaultEvidenceTitle(s.evidenceTitle); changed = true;
                    }
                    if (isBlank(item.getDefaultEvidenceContent()) && notBlank(s.evidenceContent)) {
                        item.setDefaultEvidenceContent(s.evidenceContent); changed = true;
                    }
                    if (changed) toBackfill.add(item);
                }
            }
            if (!toAdd.isEmpty()) itemRepository.saveAll(toAdd);
            if (!toBackfill.isEmpty()) itemRepository.saveAll(toBackfill);
            if (!toAdd.isEmpty() || !toBackfill.isEmpty()) {
                log.info("ISMS-P items seeded: {} new, {} backfilled", toAdd.size(), toBackfill.size());
            } else {
                log.info("ISMS-P items already up to date");
            }
        } catch (Exception e) {
            log.error("Failed to seed ISMS-P items: {}", e.getMessage());
        }
    }

    private static boolean isBlank(String s) { return s == null || s.isBlank(); }
    private static boolean notBlank(String s) { return s != null && !s.isBlank(); }

    @Data
    private static class IsmsItemSeed {
        String code;
        String name;
        String domainCode;
        String domainName;
        String sectionName;
        String desc;
        int section;
        String guide;
        String evidenceTitle;
        String evidenceContent;
    }
}
