package com.kkamar.cnh.support;

import com.kkamar.cnh.domain.Board;
import com.kkamar.cnh.domain.Department;
import com.kkamar.cnh.domain.Device;
import com.kkamar.cnh.domain.Notice;
import com.kkamar.cnh.domain.University;
import com.kkamar.cnh.repository.BoardRepository;
import com.kkamar.cnh.repository.CrawlLogRepository;
import com.kkamar.cnh.repository.DepartmentRepository;
import com.kkamar.cnh.repository.DeviceKeywordRepository;
import com.kkamar.cnh.repository.DeviceRepository;
import com.kkamar.cnh.repository.KeywordRepository;
import com.kkamar.cnh.repository.NoticeRepository;
import com.kkamar.cnh.repository.ScrapRepository;
import com.kkamar.cnh.repository.UniversityRepository;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

/**
 * 통합 테스트 공통 기반. 모든 하위 테스트가 같은 설정(=같은 스프링 컨텍스트)을 공유하고,
 * 매 테스트 전에 인메모리 H2를 비운다.
 */
@SpringBootTest
@Import(TestFakesConfig.class)
public abstract class IntegrationTestBase {

    @Autowired protected UniversityRepository universityRepository;
    @Autowired protected DepartmentRepository departmentRepository;
    @Autowired protected BoardRepository boardRepository;
    @Autowired protected NoticeRepository noticeRepository;
    @Autowired protected DeviceRepository deviceRepository;
    @Autowired protected KeywordRepository keywordRepository;
    @Autowired protected DeviceKeywordRepository deviceKeywordRepository;
    @Autowired protected CrawlLogRepository crawlLogRepository;
    @Autowired protected ScrapRepository scrapRepository;

    @Autowired protected FakePageFetcher fakePageFetcher;
    @Autowired protected RecordingAlertNotifier alertNotifier;

    @BeforeEach
    protected void cleanDatabase() {
        // 외래키 순서: 자식 -> 부모
        crawlLogRepository.deleteAll();
        scrapRepository.deleteAll();
        deviceKeywordRepository.deleteAll();
        noticeRepository.deleteAll();
        deviceRepository.deleteAll();
        boardRepository.deleteAll();
        departmentRepository.deleteAll();
        universityRepository.deleteAll();
        keywordRepository.deleteAll();
        fakePageFetcher.reset();
        alertNotifier.reset();
    }

    protected University saveUniversity(String name, String portalBaseUrl) {
        return universityRepository.save(University.builder().name(name).portalBaseUrl(portalBaseUrl).build());
    }

    protected Department saveDepartment(University university, String name) {
        return departmentRepository.save(Department.builder().university(university).name(name).college("테스트단과").build());
    }

    protected Board saveBoard(University university, Department department, String name, String crawlUrl) {
        return boardRepository.save(Board.builder()
                .university(university).department(department).name(name).crawlUrl(crawlUrl).build());
    }

    protected Notice saveNotice(Board board, String externalId, String title, LocalDate postedAt) {
        return noticeRepository.save(Notice.builder()
                .board(board)
                .externalId(externalId)
                .title(title)
                .originalUrl("https://notice.test/" + externalId)
                .postedAt(postedAt)
                .build());
    }

    protected Device saveDevice(String uuid, Department department) {
        return deviceRepository.save(Device.builder().deviceUuid(uuid).department(department).build());
    }

    protected static String fixture(String name) {
        try {
            return new String(new ClassPathResource("fixtures/" + name).getInputStream().readAllBytes(),
                    StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("fixture not found: " + name, e);
        }
    }
}
