package com.kkamar.cnh.util;

import com.kkamar.cnh.util.BoardHtmlParser.ParseResult;
import com.kkamar.cnh.util.BoardHtmlParser.ParsedNotice;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 저장해 둔 HTML(스냅샷)로 파서를 검증한다.
 * 이 테스트는 "내가 파서를 고치다가 기존 게시판을 망가뜨리는 것"을 막는 회귀 테스트이지,
 * 학교 사이트가 바뀐 것을 실시간으로 알아채는 장치가 아니다(그건 crawl_log 기반 자가진단의 몫).
 *
 * fixtures/*.html 은 셀렉터(td.td-subject a, td.td-date)를 기준으로 만든 샘플이다.
 * 실제 게시판 페이지를 브라우저에서 저장해 같은 이름으로 교체하면 더 현실적인 회귀 테스트가 된다.
 */
class BoardHtmlParserTest {

    private static final String PORTAL = "https://portal.test";

    private static Document load(String fixtureName, String baseUri) {
        try {
            String html = new String(new ClassPathResource("fixtures/" + fixtureName).getInputStream().readAllBytes(),
                    StandardCharsets.UTF_8);
            return Jsoup.parse(html, baseUri);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Test
    void universityBoard_extractsAllNoticesWithJsLinkInfo() {
        ParseResult result = BoardHtmlParser.parseUniversityBoard(
                load("university_board.html", PORTAL + "/bbs/all"), PORTAL);

        assertThat(result.linksExtracted()).isEqualTo(10);

        ParsedNotice first = result.notices().get(0);
        assertThat(first.externalId()).isEqualTo("900010");
        assertThat(first.title()).isEqualTo("[장학] 2026학년도 2학기 국가장학금 신청 안내");
        assertThat(first.detailUrl()).isEqualTo("https://portal.test/bbs/inhatc/B0000051/900010/artclView.do");
        // 날짜 끝의 마침표("2026.09.21.")도 처리된다
        assertThat(first.postedAt()).isEqualTo(LocalDate.of(2026, 9, 21));
        assertThat(result.notices().get(1).postedAt()).isEqualTo(LocalDate.of(2026, 9, 20));
    }

    @Test
    void rowsFoundIncludesHeaderRow_soNormalGapIsExactlyOne() {
        // rows found(11) vs links extracted(10): 표 머리글 <tr>은 td.td-subject a 가 없어 건너뛰므로 차이는 항상 1이다.
        ParseResult result = BoardHtmlParser.parseUniversityBoard(
                load("university_board.html", PORTAL + "/bbs/all"), PORTAL);

        assertThat(result.rowsFound()).isEqualTo(11);
        assertThat(result.linksExtracted()).isEqualTo(10);
        assertThat(result.rowsFound() - result.linksExtracted()).isEqualTo(1);
    }

    @Test
    void departmentBoard_extractsExternalIdFromUrlAndTitleFromStrongTag() {
        ParseResult result = BoardHtmlParser.parseDepartmentBoard(
                load("dept_board.html", "https://dept.test/board"));

        assertThat(result.rowsFound()).isEqualTo(11);
        assertThat(result.linksExtracted()).isEqualTo(10);

        ParsedNotice first = result.notices().get(0);
        assertThat(first.externalId()).isEqualTo("55510");
        assertThat(first.title()).isEqualTo("[학과] 2학기 전공 심화 스터디 모집");
        assertThat(first.detailUrl()).isEqualTo("https://dept.test/bbs/inhatc/1234/55510/artclView.do");
        assertThat(first.postedAt()).isEqualTo(LocalDate.of(2026, 9, 22));
    }

    @Test
    void redesignedPage_keepsRowsButExtractsNothing() {
        // 사이트 개편 시나리오: 페이지는 열리고(행 11개) 셀렉터만 안 맞아 링크 추출이 0건이 된다.
        // 에러도 예외도 없이 "조용히" 0건이 되는 것이 이 크롤러의 가장 위험한 실패 모드다.
        ParseResult result = BoardHtmlParser.parseUniversityBoard(
                load("university_board_redesigned.html", PORTAL + "/bbs/all"), PORTAL);

        assertThat(result.rowsFound()).isEqualTo(11);
        assertThat(result.linksExtracted()).isZero();
    }

    @Test
    void universityBoard_skipsLinksThatDoNotMatchKnownJavascriptPattern() {
        String html = "<table><tr><td class='td-subject'><a href=\"javascript:somethingElse('x')\">제목</a></td>"
                + "<td class='td-date'>2026.09.01</td></tr></table>";
        ParseResult result = BoardHtmlParser.parseUniversityBoard(Jsoup.parse(html, PORTAL), PORTAL);

        assertThat(result.rowsFound()).isEqualTo(1);
        assertThat(result.linksExtracted()).isZero();
    }

    @Test
    void invalidDateYieldsNullPostedAtButStillExtracts() {
        String html = "<table><tr><td class='td-subject'><a href='/bbs/x/1/777/artclView.do'>제목</a></td>"
                + "<td class='td-date'>어제</td></tr></table>";
        ParseResult result = BoardHtmlParser.parseDepartmentBoard(Jsoup.parse(html, "https://dept.test"));

        assertThat(result.linksExtracted()).isEqualTo(1);
        assertThat(result.notices().get(0).postedAt()).isNull();
    }
}
