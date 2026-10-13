package com.kkamar.cnh.service;

import com.kkamar.cnh.domain.Board;
import com.kkamar.cnh.domain.Department;
import com.kkamar.cnh.domain.Device;
import com.kkamar.cnh.domain.University;
import com.kkamar.cnh.dto.NoticeResponse;
import com.kkamar.cnh.dto.PageResponse;
import com.kkamar.cnh.exception.ResourceNotFoundException;
import com.kkamar.cnh.support.IntegrationTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FeedServiceTest extends IntegrationTestBase {

    @Autowired private FeedService feedService;
    @Autowired private DeviceService deviceService;

    private Device deviceInDeptA;

    @BeforeEach
    void setUp() {
        University university = saveUniversity("U", "https://p.test");
        Department deptA = saveDepartment(university, "컴퓨터정보과");
        Department deptB = saveDepartment(university, "전기과");
        Board univBoard = saveBoard(university, null, "전체공지", "https://p.test/all");
        Board boardA = saveBoard(university, deptA, "컴정공지", "https://p.test/a");
        Board boardB = saveBoard(university, deptB, "전기공지", "https://p.test/b");

        saveNotice(univBoard, "1", "전체 장학금 안내", LocalDate.of(2026, 9, 20));
        saveNotice(boardA, "2", "컴정 장학금 안내", LocalDate.of(2026, 9, 21));
        saveNotice(boardB, "3", "전기 장학금 안내", LocalDate.of(2026, 9, 22));
        saveNotice(boardA, "4", "컴정 행사 안내", LocalDate.of(2026, 9, 23));
        saveNotice(univBoard, "5", "100% 지원 프로그램", LocalDate.of(2026, 9, 19));
        saveNotice(univBoard, "6", "1000명 모집", LocalDate.of(2026, 9, 18));

        deviceInDeptA = saveDevice("feed-device-a", deptA);
    }

    private static List<String> titles(PageResponse<NoticeResponse> page) {
        return page.getContent().stream().map(NoticeResponse::getTitle).toList();
    }

    @Test
    void withoutKeywords_returnsUniversityWideAndOwnDepartmentNoticesNewestFirst() {
        PageResponse<NoticeResponse> feed = feedService.getFeed(deviceInDeptA.getId(), 0, 20);

        assertThat(titles(feed)).containsExactly(
                "컴정 행사 안내", "컴정 장학금 안내", "전체 장학금 안내", "100% 지원 프로그램", "1000명 모집");
        assertThat(feed.getContent().get(0).getBoardName()).isEqualTo("컴정공지");
    }

    @Test
    void withKeyword_filtersByTitle() {
        deviceService.subscribeKeyword(deviceInDeptA.getId(), "장학금");

        assertThat(titles(feedService.getFeed(deviceInDeptA.getId(), 0, 20)))
                .containsExactly("컴정 장학금 안내", "전체 장학금 안내");
    }

    @Test
    void keywordWithLikeWildcard_isMatchedLiterally() {
        // "100%"가 와일드카드로 해석되면 "1000명 모집"까지 걸려 나온다
        deviceService.subscribeKeyword(deviceInDeptA.getId(), "100%");

        assertThat(titles(feedService.getFeed(deviceInDeptA.getId(), 0, 20)))
                .containsExactly("100% 지원 프로그램");
    }

    @Test
    void multipleKeywords_matchAny() {
        deviceService.subscribeKeyword(deviceInDeptA.getId(), "행사");
        deviceService.subscribeKeyword(deviceInDeptA.getId(), "모집");

        assertThat(titles(feedService.getFeed(deviceInDeptA.getId(), 0, 20)))
                .containsExactly("컴정 행사 안내", "1000명 모집");
    }

    @Test
    void deviceWithoutDepartment_seesOnlyUniversityWideNotices() {
        Device noDept = saveDevice("feed-device-none", null);

        assertThat(titles(feedService.getFeed(noDept.getId(), 0, 20)))
                .containsExactly("전체 장학금 안내", "100% 지원 프로그램", "1000명 모집");
    }

    @Test
    void pagination_reportsTotalsAndHasNext() {
        PageResponse<NoticeResponse> first = feedService.getFeed(deviceInDeptA.getId(), 0, 2);
        assertThat(first.getContent()).hasSize(2);
        assertThat(first.getTotalElements()).isEqualTo(5);
        assertThat(first.getTotalPages()).isEqualTo(3);
        assertThat(first.isHasNext()).isTrue();

        PageResponse<NoticeResponse> last = feedService.getFeed(deviceInDeptA.getId(), 2, 2);
        assertThat(last.getContent()).hasSize(1);
        assertThat(last.isHasNext()).isFalse();
    }

    @Test
    void unknownDevice_isNotFound() {
        assertThatThrownBy(() -> feedService.getFeed(999_999L, 0, 20))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
