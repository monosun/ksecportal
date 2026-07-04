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
            for (int i = 0; i < seeds.size(); i++) {
                IsmsItemSeed s = seeds.get(i);
                if (!itemRepository.existsByItemCode(s.code)) {
                    toAdd.add(IsmsItem.builder()
                            .itemCode(s.code)
                            .itemName(s.name)
                            .domainCode(s.domainCode)
                            .domainName(s.domainName)
                            .sectionNum(s.section)
                            .sectionName(s.sectionName)
                            .description(s.desc)
                            .sortOrder(i + 1)
                            .build());
                }
            }
            if (!toAdd.isEmpty()) {
                itemRepository.saveAll(toAdd);
                log.info("ISMS-P items seeded: {} new items added", toAdd.size());
            } else {
                log.info("ISMS-P items already up to date");
            }
        } catch (Exception e) {
            log.error("Failed to seed ISMS-P items: {}", e.getMessage());
        }
    }

    @Data
    private static class IsmsItemSeed {
        String code;
        String name;
        String domainCode;
        String domainName;
        String sectionName;
        String desc;
        int section;
    }
}
