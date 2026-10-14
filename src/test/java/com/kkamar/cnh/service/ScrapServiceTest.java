package com.kkamar.cnh.service;

import com.kkamar.cnh.domain.Board;
import com.kkamar.cnh.domain.Device;
import com.kkamar.cnh.domain.Notice;
import com.kkamar.cnh.domain.University;
import com.kkamar.cnh.dto.ScrapResponse;
import com.kkamar.cnh.exception.ResourceNotFoundException;
import com.kkamar.cnh.support.IntegrationTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ScrapServiceTest extends IntegrationTestBase {

    @Autowired private ScrapService scrapService;

    private Device device;
    private Notice notice1;
    private Notice notice2;

    @BeforeEach
    void setUp() {
        University university = saveUniversity("U", "https://p.test");
        Board board = saveBoard(university, null, "전체공지", "https://p.test/all");
        notice1 = saveNotice(board, "1", "첫 번째 공지", LocalDate.of(2026, 9, 20));
        notice2 = saveNotice(board, "2", "두 번째 공지", LocalDate.of(2026, 9, 21));
        device = saveDevice("scrap-device", null);
    }

    @Test
    void scrap_isIdempotent_andAssignsRotatingColors() {
        ScrapResponse first = scrapService.scrap(device.getId(), notice1.getId(), null);
        ScrapResponse again = scrapService.scrap(device.getId(), notice1.getId(), null);
        ScrapResponse second = scrapService.scrap(device.getId(), notice2.getId(), null);

        assertThat(first.getColorIndex()).isEqualTo(0);
        assertThat(again.getScrapId()).isEqualTo(first.getScrapId());
        assertThat(scrapRepository.count()).isEqualTo(2);
        assertThat(second.getColorIndex()).isEqualTo(1);
        assertThat(first.getNotice().getTitle()).isEqualTo("첫 번째 공지");
        assertThat(first.getNotice().getBoardName()).isEqualTo("전체공지");
    }

    @Test
    void scrap_honorsExplicitColor() {
        ScrapResponse response = scrapService.scrap(device.getId(), notice1.getId(), 5);

        assertThat(response.getColorIndex()).isEqualTo(5);
    }

    @Test
    void unscrap_removesAndIsIdempotent() {
        scrapService.scrap(device.getId(), notice1.getId(), null);

        scrapService.unscrap(device.getId(), notice1.getId());
        scrapService.unscrap(device.getId(), notice1.getId()); // 두 번째는 아무 일도 일어나지 않아야 한다

        assertThat(scrapRepository.count()).isZero();
        assertThat(scrapService.getScraps(device.getId())).isEmpty();
    }

    @Test
    void getScraps_returnsNewestFirst() {
        scrapService.scrap(device.getId(), notice1.getId(), null);
        scrapService.scrap(device.getId(), notice2.getId(), null);

        List<ScrapResponse> scraps = scrapService.getScraps(device.getId());

        assertThat(scraps).extracting(s -> s.getNotice().getTitle())
                .containsExactly("두 번째 공지", "첫 번째 공지");
    }

    @Test
    void scrap_ofUnknownNoticeOrDevice_isNotFound() {
        assertThatThrownBy(() -> scrapService.scrap(device.getId(), 999_999L, null))
                .isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> scrapService.scrap(999_999L, notice1.getId(), null))
                .isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> scrapService.getScraps(999_999L))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
