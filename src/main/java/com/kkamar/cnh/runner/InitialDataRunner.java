package com.kkamar.cnh.runner;

import com.kkamar.cnh.domain.*;
import com.kkamar.cnh.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class InitialDataRunner implements CommandLineRunner {

    private final UniversityRepository universityRepository;
    private final DepartmentRepository departmentRepository;
    private final BoardRepository boardRepository;
    private final KeywordRepository keywordRepository;

    private static final List<String> PRESET_KEYWORDS = List.of(
            "장학금", "채용", "공모전", "인턴십", "학사공지", "행사"
    );

    @Override
    @Transactional
    public void run(String... args) {
        seedUniversityData();
        seedPresetKeywords();
    }

    private void seedUniversityData() {
        if (universityRepository.count() > 0) {
            log.info("University data already exists, skipping seeding");
            return;
        }
        // 기존 University / Department / Board 시딩 로직 그대로 유지
        // ...
    }

    private void seedPresetKeywords() {
        if (keywordRepository.count() > 0) {
            log.info("Keyword data already exists, skipping seeding");
            return;
        }
        for (String name : PRESET_KEYWORDS) {
            keywordRepository.save(
                    Keyword.builder()
                            .name(name)
                            .isPreset(true)
                            .build()
            );
        }
        log.info("Seeded {} preset keyword(s)", PRESET_KEYWORDS.size());
    }
}